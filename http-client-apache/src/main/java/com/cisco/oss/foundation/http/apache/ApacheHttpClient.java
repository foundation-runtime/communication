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

package com.cisco.oss.foundation.http.apache;

import com.cisco.oss.foundation.http.AbstractHttpClient;
import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpRequest;
import com.cisco.oss.foundation.http.HttpResponse;
import com.cisco.oss.foundation.http.ResponseCallback;
import com.cisco.oss.foundation.loadbalancer.InternalServerProxy;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.google.common.base.Joiner;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import javax.net.ssl.HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yair Ogen on 1/16/14.
 */
class ApacheHttpClient<S extends HttpRequest, R extends HttpResponse> extends AbstractHttpClient<S, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheHttpClient.class);
    CloseableHttpAsyncClient httpAsyncClient = null;
    private CloseableHttpClient httpClient = null;
    private HostnameVerifier hostnameVerifier;
    public static ThreadLocal<HttpContext> HTTP_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    public boolean isAutoCloseable() {
        return autoCloseable;
    }

    private boolean autoCloseable = true;



    ApacheHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing, HostnameVerifier hostnameVerifier) {
        super(apiName, configuration, enableLoadBalancing);
        this.hostnameVerifier = hostnameVerifier;
        configureClient();
    }


    ApacheHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE strategyType, Configuration configuration, boolean enableLoadBalancing, HostnameVerifier hostnameVerifier) {
        super(apiName, strategyType, configuration, enableLoadBalancing);
        this.hostnameVerifier = hostnameVerifier;
        configureClient();
    }

    @Override
    protected void configureClient() {

        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(metadata.getConnectTimeout());
        requestBuilder = requestBuilder.setSocketTimeout(metadata.getReadTimeout());
        requestBuilder = requestBuilder.setStaleConnectionCheckEnabled(metadata.isStaleConnectionCheckEnabled());

        RequestConfig requestConfig = requestBuilder.build();

        boolean addSslSupport = StringUtils.isNotEmpty(metadata.getKeyStorePath()) && StringUtils.isNotEmpty(metadata.getKeyStorePassword());

        boolean addTrustSupport = StringUtils.isNotEmpty(metadata.getTrustStorePath()) && StringUtils.isNotEmpty(metadata.getTrustStorePassword());

        autoCloseable = metadata.isAutoCloseable();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        SSLContext sslContext = null;

        try {

            String keystoreType = "JKS";
            if (addSslSupport && addTrustSupport) {

                KeyStore keyStore = KeyStore.getInstance(keystoreType);
                keyStore.load(new FileInputStream(metadata.getKeyStorePath()), metadata.getKeyStorePassword().toCharArray());

                KeyStore trustStore = KeyStore.getInstance(keystoreType);
                trustStore.load(new FileInputStream(metadata.getTrustStorePath()), metadata.getTrustStorePassword().toCharArray());

                sslContext = SSLContexts.custom()
                        .useProtocol("TLS")
                        .loadKeyMaterial(keyStore, metadata.getKeyStorePassword().toCharArray())
                        .loadTrustMaterial(trustStore, null)
                        .build();

            } else if (addSslSupport) {

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

                KeyStore keyStore = KeyStore.getInstance(keystoreType);
                keyStore.load(new FileInputStream(metadata.getKeyStorePath()), metadata.getKeyStorePassword().toCharArray());

                tmf.init(keyStore);


                sslContext = SSLContexts.custom()
                        .useProtocol("SSL")
                        .loadKeyMaterial(keyStore, metadata.getKeyStorePassword().toCharArray())
                        .build();

                sslContext.init(null, tmf.getTrustManagers(),null);

                SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);

                httpClientBuilder.setSSLSocketFactory(sf);


            } else if (addTrustSupport) {

                KeyStore trustStore = KeyStore.getInstance(keystoreType);
                trustStore.load(new FileInputStream(metadata.getTrustStorePath()), metadata.getTrustStorePassword().toCharArray());

                sslContext = SSLContexts.custom()
                        .useProtocol("TLS")
                        .loadTrustMaterial(trustStore, null)
                        .build();

            }

            if (addSslSupport | addTrustSupport) {
                SSLContext.setDefault(sslContext);
                httpClientBuilder.setSslcontext(sslContext);
            }


        } catch (Exception e) {
            LOGGER.error("can't set TLS Support. Error is: {}", e, e);
        }


        httpClientBuilder.setMaxConnPerRoute(metadata.getMaxConnectionsPerAddress())
                .setMaxConnTotal(metadata.getMaxConnectionsTotal())
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(metadata.getIdleTimeout(), TimeUnit.MILLISECONDS)
                .setKeepAliveStrategy(new InfraConnectionKeepAliveStrategy(metadata.getIdleTimeout()));




        HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients.custom();

        httpAsyncClientBuilder.setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(metadata.getMaxConnectionsPerAddress())
                .setMaxConnTotal(metadata.getMaxConnectionsTotal())
                .setKeepAliveStrategy(new InfraConnectionKeepAliveStrategy(metadata.getIdleTimeout()))
                .setSSLContext(sslContext);

        if(metadata.isDisableCookies()){
            httpClientBuilder.disableCookieManagement();
            httpAsyncClientBuilder.disableCookieManagement();
        }

        if (hostnameVerifier != null) {
            httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
            httpAsyncClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
        }

        if (!followRedirects) {
            httpClientBuilder.disableRedirectHandling();
        }

        httpClient = httpClientBuilder.build();

        httpAsyncClient = httpAsyncClientBuilder.build();

        httpAsyncClient.start();

    }

    @Override
    public void executeDirect(HttpRequest request, ResponseCallback responseCallback) {

        org.apache.http.HttpRequest httpRequest = null;

        Joiner joiner = Joiner.on(",").skipNulls();
        URI requestUri = buildUri(request, joiner);

        httpRequest = buildHttpUriRequest(request, joiner, requestUri);

        HttpHost httpHost = new HttpHost(requestUri.getHost(),requestUri.getPort(),requestUri.getScheme());

        httpAsyncClient.execute(httpHost, httpRequest, new FoundationDirectFutureCallBack(apiName, request, this, responseCallback));

    }

    @Override
    public HttpResponse executeDirect(HttpRequest request) {

        org.apache.http.HttpRequest httpRequest = null;

        Joiner joiner = Joiner.on(",").skipNulls();
        URI requestUri = buildUri(request, joiner);

        httpRequest = buildHttpUriRequest(request, joiner, requestUri);

        try {
//            LOGGER.info("sending request: {}", request.getUri());

            HttpHost httpHost = new HttpHost(requestUri.getHost(),requestUri.getPort(),requestUri.getScheme());
            HttpContext httpContext = HTTP_CONTEXT_THREAD_LOCAL.get();
            CloseableHttpResponse response = httpContext == null ? httpClient.execute(httpHost, httpRequest): httpClient.execute(httpHost, httpRequest, httpContext);
            ApacheHttpResponse apacheHttpResponse = new ApacheHttpResponse(response, requestUri, autoCloseable);
//            LOGGER.info("got response status: {} for request: {}",apacheHttpResponse.getStatus(), apacheHttpResponse.getRequestedURI());
            return apacheHttpResponse;
        } catch (IOException e) {
            throw new ClientException(e.toString(), e);
        }
    }

    private org.apache.http.HttpRequest buildHttpUriRequest(HttpRequest request, Joiner joiner, URI requestUri) {
        org.apache.http.HttpRequest httpRequest;
        if(autoEncodeUri){
            switch (request.getHttpMethod()) {
                case GET:
                    httpRequest = new HttpGet(requestUri);
                    break;
                case POST:
                    httpRequest = new HttpPost(requestUri);
                    break;
                case PUT:
                    httpRequest = new HttpPut(requestUri);
                    break;
                case DELETE:
                    httpRequest = new HttpDelete(requestUri);
                    break;
                case HEAD:
                    httpRequest = new HttpHead(requestUri);
                    break;
                case OPTIONS:
                    httpRequest = new HttpOptions(requestUri);
                    break;
                case PATCH:
                    httpRequest = new HttpPatch(requestUri);
                    break;
                default:
                    throw new ClientException("You have to one of the REST verbs such as GET, POST etc.");
            }
        }else{
            switch (request.getHttpMethod()) {
                case POST:
                case PUT:
                case DELETE:
                case PATCH:
                    httpRequest = new BasicHttpEntityEnclosingRequest(request.getHttpMethod().method(), requestUri.toString());
                    break;
                default:
                    httpRequest = new BasicHttpRequest(request.getHttpMethod().method(), requestUri.toString());
            }

        }

        byte[] entity = request.getEntity();
        if (entity != null) {
            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest httpEntityEnclosingRequestBase = (HttpEntityEnclosingRequest) httpRequest;
                httpEntityEnclosingRequestBase.setEntity(new ByteArrayEntity(entity, ContentType.create(request.getContentType())));
            } else {
                throw new ClientException("sending content for request type " + request.getHttpMethod() + " is not supported!");
            }
        }else{
            if (request instanceof ApacheHttpRequest && httpRequest instanceof HttpEntityEnclosingRequest){
                HttpEntityEnclosingRequest httpEntityEnclosingRequestBase = (HttpEntityEnclosingRequest) httpRequest;
                ApacheHttpRequest apacheHttpRequest = (ApacheHttpRequest) request;
                httpEntityEnclosingRequestBase.setEntity(apacheHttpRequest.getApacheHttpEntity());
            }
        }

        Map<String, Collection<String>> headers = request.getHeaders();
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : headers.entrySet()) {
            String key = stringCollectionEntry.getKey();
            Collection<String> stringCollection = stringCollectionEntry.getValue();
            String value = joiner.join(stringCollection);
            httpRequest.setHeader(key, value);
        }
        return httpRequest;
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
                }else{
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

                if(!autoEncodeUri){
                    String urlPath = "";
                    if (requestUri.getRawPath() != null && requestUri.getRawPath().startsWith("/")) {
                        urlPath = requestUri.getRawPath();
                    } else {
                        urlPath = "/" + requestUri.getRawPath();
                    }

                    if (hasQuery){
                        String query = queryStringBuilder.substring(0,queryStringBuilder.length()-1);
                        requestUri = new URI(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + urlPath + "?" + query);
                    }else{
                        requestUri = new URI(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + urlPath);
                    }
                }else{
                    requestUri = uriBuilder.build();
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("could not update uri: {}", requestUri);
            }
        }
        return requestUri;
    }

    @Override
    public void execute(HttpRequest request, ResponseCallback responseCallback, LoadBalancerStrategy loadBalancerStrategy, String apiName) {

        InternalServerProxy serverProxy = loadBalancerStrategy.getServerProxy(request);
        Throwable lastCaugtException = null;


        if (serverProxy == null) {
            // server proxy will be null if the configuration was not
            // configured properly
            // or if all the servers are passivated.
            loadBalancerStrategy.handleNullserverProxy(apiName, lastCaugtException);
        }

        request = updateRequestUri((S)request, serverProxy);

        if (request.isSilentLogging()) {
            LOGGER.trace("sending request: {}-{}", request.getHttpMethod(), request.getUri());
        }else{
            LOGGER.info("sending request: {}-{}", request.getHttpMethod(), request.getUri());
        }


        org.apache.http.HttpRequest httpRequest = null;

        Joiner joiner = Joiner.on(",").skipNulls();
        URI requestUri = buildUri(request, joiner);

        httpRequest = buildHttpUriRequest(request, joiner, requestUri);

        HttpHost httpHost = new HttpHost(requestUri.getHost(),requestUri.getPort(),requestUri.getScheme());

        httpAsyncClient.execute(httpHost, httpRequest, new FoundationFutureCallBack(this,request, responseCallback, serverProxy, loadBalancerStrategy, apiName));

    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }

            if (httpAsyncClient != null) {
                httpAsyncClient.close();
            }
        } catch (IOException e) {
            LOGGER.warn("can't close http client: {}", e);
        }
    }

    private static class FoundationDirectFutureCallBack implements FutureCallback<org.apache.http.HttpResponse> {

        private ResponseCallback responseCallback;
        private String apiName;
        private HttpRequest request;
        private ApacheHttpClient apacheHttpClient;

        public FoundationDirectFutureCallBack(String apiName, HttpRequest request, ApacheHttpClient apacheHttpClient, ResponseCallback responseCallback) {
            this.responseCallback=responseCallback;
            this.apiName = apiName;
            this.request = request;
            this.apacheHttpClient = apacheHttpClient;
        }

        @Override
        public void completed(org.apache.http.HttpResponse response) {
            ApacheHttpResponse apacheHttpResponse = new ApacheHttpResponse(response, request.getUri(), apacheHttpClient.isAutoCloseable());
            if (request.isSilentLogging()) {
                LOGGER.trace("got response status: {} for request: {}", apacheHttpResponse.getStatus(), apacheHttpResponse.getRequestedURI());
            }else{
                LOGGER.info("got response status: {} for request: {}", apacheHttpResponse.getStatus(), apacheHttpResponse.getRequestedURI());
            }
            responseCallback.completed(apacheHttpResponse);
        }

        @Override
        public void failed(Exception ex) {
            responseCallback.failed(ex);
        }

        @Override
        public void cancelled() {
            responseCallback.cancelled();
        }
    }

    private static class FoundationFutureCallBack implements FutureCallback<org.apache.http.HttpResponse> {
        private ResponseCallback responseCallback;
        private InternalServerProxy serverProxy;
        private LoadBalancerStrategy loadBalancerStrategy;
        private String apiName;
        private HttpRequest request;
        private ApacheHttpClient apacheHttpClient;


        private FoundationFutureCallBack(ApacheHttpClient apacheHttpClient, HttpRequest request, ResponseCallback<ApacheHttpResponse> responseCallback, InternalServerProxy serverProxy, LoadBalancerStrategy loadBalancerStrategy, String apiName) {
            this.responseCallback = responseCallback;
            this.apacheHttpClient = apacheHttpClient;
            this.serverProxy = serverProxy;
            this.loadBalancerStrategy = loadBalancerStrategy;
            this.apiName = apiName;
            this.request = request;
        }

        @Override
        public void completed(org.apache.http.HttpResponse response) {

            serverProxy.setCurrentNumberOfAttempts(0);
            serverProxy.setFailedAttemptTimeStamp(0);

            ApacheHttpResponse apacheHttpResponse = new ApacheHttpResponse(response, request.getUri(), apacheHttpClient.isAutoCloseable());
            if (request.isSilentLogging()) {
                LOGGER.trace("got response status: {} for request: {}", apacheHttpResponse.getStatus(), apacheHttpResponse.getRequestedURI());
            }else{
                LOGGER.info("got response status: {} for request: {}", apacheHttpResponse.getStatus(), apacheHttpResponse.getRequestedURI());
            }
            responseCallback.completed(apacheHttpResponse);
        }

        @Override
        public void failed(Exception ex) {

            try {
                loadBalancerStrategy.handleException(apiName, serverProxy, ex);
            } catch (Exception e) {
                LOGGER.error("Error running request {}. Error is: {}", request.getUri(), e);
                responseCallback.failed(e);
            }

            try {
                apacheHttpClient.execute(request, responseCallback, loadBalancerStrategy, apiName);
            } catch (Throwable e) {
//                result.getRequest().abort(e);
                responseCallback.failed(e);
            }
        }

        @Override
        public void cancelled() {
            responseCallback.cancelled();
        }
    }


}
