/*
 * Copyright 2015 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.http.netlifx.netty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.http.*;
import com.cisco.oss.foundation.loadbalancer.InternalServerProxy;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerConstants;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.google.common.base.Joiner;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.ribbon.transport.netty.RibbonTransport;
import com.netflix.ribbon.transport.netty.http.LoadBalancingHttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Yair Ogen (yaogen) on 13/12/2015.
 */
class NettyNetflixHttpClient implements HttpClient<HttpRequest, NettyNetflixHttpResponse> {

    static {
        if (!ConfigurationManager.isConfigurationInstalled()) {
            ConfigurationManager.install((AbstractConfiguration) ConfigurationFactory.getConfiguration());
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyNetflixHttpClient.class);
    private HostnameVerifier hostnameVerifier;
    protected InternalServerProxyMetadata metadata = loadServersMetadataConfiguration();
    protected boolean followRedirects = false;
    protected boolean autoEncodeUri = true;
    private IClientConfig clientConfig = null;
    private Joiner joiner = Joiner.on(",").skipNulls();
    private String apiName = "HTTP";
    protected boolean enableLoadBalancing = true;
    BaseLoadBalancer loadBalancer = new DynamicServerListLoadBalancer<DiscoveryEnabledServer>();
    LoadBalancingHttpClient<ByteBuf, ByteBuf> httpClient = null;
    private RetryHandler retryHandler = null;

    public NettyNetflixHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing, HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        this.apiName = apiName;
        this.enableLoadBalancing = enableLoadBalancing;
        configureClient();
    }



    public boolean isAutoCloseable() {
        return autoCloseable;
    }

    private boolean autoCloseable = true;

    @Override
    public NettyNetflixHttpResponse execute(HttpRequest request) {

        try {
            if (enableLoadBalancing) {
                return executeWithLoadBalancer(request);
            } else {
                return (NettyNetflixHttpResponse) executeDirect(request);
            }

        } catch (Exception e) {
            throw new ClientException(e.toString(), e);
        }
    }

    public NettyNetflixHttpResponse executeDirect(HttpRequest request) {
        HttpClientRequest<ByteBuf> httpRequest = buildNetflixHttpRequest(request, joiner);
        rx.Observable<HttpClientResponse<ByteBuf>> responseObservable = httpClient.submit(httpRequest, retryHandler, clientConfig);
        return new NettyNetflixHttpResponse(responseObservable.toBlocking().first());
    }

    @Override
    public NettyNetflixHttpResponse executeWithLoadBalancer(HttpRequest request) {
        return executeDirect(request);

    }

    @Override
    public void executeWithLoadBalancer(HttpRequest request, final ResponseCallback<NettyNetflixHttpResponse> responseCallback) {
        HttpClientRequest<ByteBuf> httpRequest = buildNetflixHttpRequest(request, joiner);
        rx.Observable<HttpClientResponse<ByteBuf>> responseObservable = httpClient.submit(httpRequest, retryHandler, clientConfig);

        responseObservable.subscribe(new Subscriber<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
//                responseCallback.
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("error serving request: {}", e, e);
                responseCallback.failed(e);
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> byteBufHttpClientResponse) {
                responseCallback.completed(new NettyNetflixHttpResponse(byteBufHttpClientResponse));
            }
        });

    }


    @Override
    public String getApiName() {
        return apiName;
    }


    protected void configureClient() {

        clientConfig = new DefaultClientConfigImpl();
        clientConfig.loadProperties(getApiName());

        EurekaInstanceConfig eurekaInstanceConfig = new MyDataCenterInstanceConfig(getApiName());
        EurekaClientConfig eurekaClientConfig = new DefaultEurekaClientConfig(getApiName() + ".");
        DiscoveryManager.getInstance().initComponent(eurekaInstanceConfig, eurekaClientConfig);

        loadBalancer.initWithNiwsConfig(clientConfig);

        httpClient = RibbonTransport.newHttpClient(loadBalancer, clientConfig);

        retryHandler = new NettyNetflixRetryHandler(metadata);


        boolean addSslSupport = StringUtils.isNotEmpty(metadata.getKeyStorePath()) && StringUtils.isNotEmpty(metadata.getKeyStorePassword());

        boolean addTrustSupport = StringUtils.isNotEmpty(metadata.getTrustStorePath()) && StringUtils.isNotEmpty(metadata.getTrustStorePassword());

        autoCloseable = metadata.isAutoCloseable();

        autoEncodeUri = metadata.isAutoEncodeUri();
        followRedirects = metadata.isFollowRedirects();


    }

    private URI buildUri(HttpRequest request, Joiner joiner) {
        URI requestUri = request.getUri();

        Map<String, Collection<String>> queryParams = request.getQueryParams();
        if (queryParams != null && !queryParams.isEmpty()) {
            URIBuilder uriBuilder = new URIBuilder();
            StringBuilder queryStringBuilder = new StringBuilder();
            boolean hasQuery = !queryParams.isEmpty();
            for (Map.Entry<String, Collection<String>> stringCollectionEntry : queryParams.entrySet()) {
                String key = stringCollectionEntry.getKey();
                Collection<String> queryParamsValueList = stringCollectionEntry.getValue();
                if (request.isQueryParamsParseAsMultiValue()) {
                    for (String queryParamsValue : queryParamsValueList) {
                        uriBuilder.addParameter(key, queryParamsValue);
                        queryStringBuilder.append(key).append("=").append(queryParamsValue).append("&");
                    }
                } else {
                    String value = joiner.join(queryParamsValueList);
                    uriBuilder.addParameter(key, value);
                    queryStringBuilder.append(key).append("=").append(value).append("&");
                }
            }
            uriBuilder.setFragment(requestUri.getFragment());
            uriBuilder.setHost(requestUri.getHost());
            uriBuilder.setPath(requestUri.getPath());
            uriBuilder.setPort(requestUri.getPort());
            uriBuilder.setScheme(requestUri.getScheme());
            uriBuilder.setUserInfo(requestUri.getUserInfo());
            try {

                if (!autoEncodeUri) {
                    String urlPath = "";
                    if (requestUri.getRawPath() != null && requestUri.getRawPath().startsWith("/")) {
                        urlPath = requestUri.getRawPath();
                    } else {
                        urlPath = "/" + requestUri.getRawPath();
                    }

                    if (hasQuery) {
                        String query = queryStringBuilder.substring(0, queryStringBuilder.length() - 1);
                        requestUri = new URI(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + urlPath + "?" + query);
                    } else {
                        requestUri = new URI(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + urlPath);
                    }
                } else {
                    requestUri = uriBuilder.build();
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("could not update uri: {}", requestUri);
            }
        }
        return requestUri;
    }

    private InternalServerProxyMetadata loadServersMetadataConfiguration() {


        Configuration subset = ConfigurationFactory.getConfiguration().subset(getApiName());
        final Iterator<String> keysIterator = subset.getKeys();

        // read default values
        int readTimeout = subset.getInt("http." + LoadBalancerConstants.READ_TIME_OUT, LoadBalancerConstants.DEFAULT_READ_TIMEOUT);
        int connectTimeout = subset.getInt("http." + LoadBalancerConstants.CONNECT_TIME_OUT, LoadBalancerConstants.DEFAULT_CONNECT_TIMEOUT);
        long waitingTime = subset.getLong("http." + LoadBalancerConstants.WAITING_TIME, LoadBalancerConstants.DEFAULT_WAITING_TIME);
        int numberOfAttempts = subset.getInt("http." + LoadBalancerConstants.NUMBER_OF_ATTEMPTS, LoadBalancerConstants.DEFAULT_NUMBER_OF_ATTEMPTS);
        long retryDelay = subset.getLong("http." + LoadBalancerConstants.RETRY_DELAY, LoadBalancerConstants.DEFAULT_RETRY_DELAY);

        long idleTimeout = subset.getLong("http." + LoadBalancerConstants.IDLE_TIME_OUT, LoadBalancerConstants.DEFAULT_IDLE_TIMEOUT);
        int maxConnectionsPerAddress = subset.getInt("http." + LoadBalancerConstants.MAX_CONNECTIONS_PER_ADDRESS, LoadBalancerConstants.DEFAULT_MAX_CONNECTIONS_PER_ADDRESS);
        int maxConnectionsTotal = subset.getInt("http." + LoadBalancerConstants.MAX_CONNECTIONS_TOTAL, LoadBalancerConstants.DEFAULT_MAX_CONNECTIONS_TOTAL);
        int maxQueueSizePerAddress = subset.getInt("http." + LoadBalancerConstants.MAX_QUEUE_PER_ADDRESS, LoadBalancerConstants.DEFAULT_MAX_QUEUE_PER_ADDRESS);
        boolean followRedirects = subset.getBoolean("http." + LoadBalancerConstants.FOLLOW_REDIRECTS, false);
        boolean disableCookies = subset.getBoolean("http." + LoadBalancerConstants.DISABLE_COOKIES, false);
        boolean autoCloseable = subset.getBoolean("http." + LoadBalancerConstants.AUTO_CLOSEABLE, true);
        boolean autoEncodeUri = subset.getBoolean("http." + LoadBalancerConstants.AUTO_ENCODE_URI, true);
        boolean staleConnectionCheckEnabled = subset.getBoolean("http." + LoadBalancerConstants.IS_STALE_CONN_CHECK_ENABLED, false);
        boolean serviceDirectoryEnabled = subset.getBoolean("http." + LoadBalancerConstants.SERVICE_DIRECTORY_IS_ENABLED, false);
        String serviceName = subset.getString("http." + LoadBalancerConstants.SERVICE_DIRECTORY_SERVICE_NAME, "UNKNOWN");

        String keyStorePath = subset.getString("http." + LoadBalancerConstants.KEYSTORE_PATH, "");
        String keyStorePassword = subset.getString("http." + LoadBalancerConstants.KEYSTORE_PASSWORD, "");
        String trustStorePath = subset.getString("http." + LoadBalancerConstants.TRUSTSTORE_PATH, "");
        String trustStorePassword = subset.getString("http." + LoadBalancerConstants.TRUSTSTORE_PASSWORD, "");


        final List<String> keys = new ArrayList<String>();

        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            keys.add(key);
        }

        Collections.sort(keys);

        List<Pair<String, Integer>> hostAndPortPairs = new CopyOnWriteArrayList<Pair<String, Integer>>();

        for (String key : keys) {

            if (key.contains(LoadBalancerConstants.HOST)) {

                String host = subset.getString(key);

                // trim the host name
                if (org.apache.commons.lang.StringUtils.isNotEmpty(host)) {
                    host = host.trim();
                }
                final String portKey = key.replace(LoadBalancerConstants.HOST, LoadBalancerConstants.PORT);
                if (subset.containsKey(portKey)) {
                    int port = subset.getInt(portKey);
                    // save host and port for future creation of server list
                    hostAndPortPairs.add(Pair.of(host, port));
                }
            }

        }

        InternalServerProxyMetadata metadata = new InternalServerProxyMetadata(readTimeout, connectTimeout, idleTimeout, maxConnectionsPerAddress, maxConnectionsTotal, maxQueueSizePerAddress, waitingTime, numberOfAttempts, retryDelay, hostAndPortPairs, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword, followRedirects, autoCloseable, staleConnectionCheckEnabled, disableCookies, serviceDirectoryEnabled, serviceName, autoEncodeUri);

        return metadata;

    }

    //    @Override
    public void execute(HttpRequest request, ResponseCallback responseCallback, LoadBalancerStrategy loadBalancerStrategy, String apiName) {
        executeWithLoadBalancer(request, responseCallback);

    }

    //    @Override
    public void close() {
    }

    private HttpClientRequest<ByteBuf> buildNetflixHttpRequest(HttpRequest request, Joiner joiner) {
        HttpClientRequest<ByteBuf> httpRequest = null;
        HttpMethod httpMethod = HttpMethod.valueOf(request.getHttpMethod().name());


        URI uri = buildUri(request, joiner);

        httpRequest = HttpClientRequest.create(httpMethod, uri.toString());
        byte[] entity = request.getEntity();
        if (entity != null) {
            httpRequest.withContent(entity);
        }


        Map<String, Collection<String>> headers = request.getHeaders();
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : headers.entrySet()) {
            String key = stringCollectionEntry.getKey();
            Collection<String> stringCollection = stringCollectionEntry.getValue();
            String value = joiner.join(stringCollection);
            httpRequest.withHeader(key, value);
        }

        if (StringUtils.isNoneEmpty(request.getContentType())) {
            String contentType = request.getContentType();
            httpRequest.withHeader("Content-Type", contentType);
        }

        return httpRequest;
    }


    private static class NettyNetflixRetryHandler implements RetryHandler {
        InternalServerProxyMetadata metadata = null;

        public NettyNetflixRetryHandler(InternalServerProxyMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public boolean isRetriableException(Throwable e, boolean sameServer) {
            return e instanceof IOException;
        }

        @Override
        public boolean isCircuitTrippingException(Throwable e) {
            return e instanceof IOException;
        }

        @Override
        public int getMaxRetriesOnSameServer() {
            return metadata.getNumberOfAttempts();
        }

        @Override
        public int getMaxRetriesOnNextServer() {
            return metadata.getNumberOfAttempts();
        }
    }

}
