package com.cisco.vss.foundation.http.apache;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.*;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;
import com.google.common.base.Joiner;
import org.apache.commons.configuration.Configuration;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Yair Ogen on 1/16/14.
 */
public class ApacheHttpClient extends AbstractHttpClient<HttpRequest, HttpResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheHttpClient.class);
    private CloseableHttpClient httpClient = null;

    public ApacheHttpClient(String apiName) {
        super(apiName);
        configureClient();
    }

    public ApacheHttpClient(String apiName, Configuration configuration) {
        super(apiName, configuration);
        configureClient();
    }

    public ApacheHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE strategyType) {
        super(apiName, strategyType);
        configureClient();
    }

    public ApacheHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE strategyType, Configuration configuration) {
        super(apiName, strategyType, configuration);
        configureClient();
    }

    @Override
    protected void configureClient() {

        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(metadata.getConnectTimeout());
        requestBuilder = requestBuilder.setSocketTimeout(metadata.getReadTimeout());

        httpClient = HttpClientBuilder.create()
                .setMaxConnPerRoute(metadata.getMaxConnectionsPerAddress())
                .setMaxConnTotal(metadata.getMaxConnectionsTotal())
                .setDefaultRequestConfig(requestBuilder.build())
                .setKeepAliveStrategy(new InfraConnectionKeepAliveStrategy(metadata.getIdleTimeout()))
                .build();
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws IOException {

        HttpUriRequest httpUriRequest = null;

        Joiner joiner = Joiner.on(",").skipNulls();
        URI requestUri = buildUri(request, joiner);

        httpUriRequest = buildHttpUriRequest(request, joiner, requestUri);

        CloseableHttpResponse response = httpClient.execute(httpUriRequest);
        return new ApacheHttpResponse(response, requestUri);
    }

    private HttpUriRequest buildHttpUriRequest(HttpRequest request, Joiner joiner, URI requestUri) {
        HttpUriRequest httpUriRequest;
        switch (request.getHttpMethod()) {
            case GET:
                httpUriRequest = new HttpGet(requestUri);
                break;
            case POST:
                httpUriRequest = new HttpPost(requestUri);
                break;
            case PUT:
                httpUriRequest = new HttpPut(requestUri);
                break;
            case DELETE:
                httpUriRequest = new HttpDelete(requestUri);
                break;
            case HEAD:
                httpUriRequest = new HttpHead(requestUri);
                break;
            case OPTIONS:
                httpUriRequest = new HttpOptions(requestUri);
                break;
            default:
                throw new ClientException("You have to one of the REST verbs such as GET, POST etc.");
        }

        byte[] entity = request.getEntity();
        if (entity != null) {
            if (httpUriRequest instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = (HttpEntityEnclosingRequestBase) httpUriRequest;
                httpEntityEnclosingRequestBase.setEntity(new ByteArrayEntity(entity, ContentType.create(request.getContentType())));
            } else {
                throw new ClientException("sending content for request type " + request.getHttpMethod() + " is not supported!");
            }
        }


        Map<String, Collection<String>> headers = request.getHeaders();
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : headers.entrySet()) {
            String key = stringCollectionEntry.getKey();
            Collection<String> stringCollection = stringCollectionEntry.getValue();
            String value = joiner.join(stringCollection);
            httpUriRequest.setHeader(key, value);
        }
        return httpUriRequest;
    }

    private URI buildUri(HttpRequest request, Joiner joiner) {
        URI requestUri = request.getUri();

        Map<String, Collection<String>> queryParams = request.getQueryParams();
        if (queryParams != null && !queryParams.isEmpty()) {
            URIBuilder uriBuilder = new URIBuilder();
            for (Map.Entry<String, Collection<String>> stringCollectionEntry : queryParams.entrySet()) {
                String key = stringCollectionEntry.getKey();
                Collection<String> stringCollection = stringCollectionEntry.getValue();
                String value = joiner.join(stringCollection);
                uriBuilder.addParameter(key, value);
            }
            uriBuilder.setFragment(requestUri.getFragment());
            uriBuilder.setHost(requestUri.getHost());
            uriBuilder.setPath(requestUri.getPath());
            uriBuilder.setPort(requestUri.getPort());
            uriBuilder.setScheme(requestUri.getScheme());
            uriBuilder.setUserInfo(requestUri.getUserInfo());
            try {
                requestUri = uriBuilder.build();
            } catch (URISyntaxException e) {
                LOGGER.warn("could not update uri: {}", requestUri);
            }
        }
        return requestUri;
    }

    @Override
    public void execute(HttpRequest request, ResponseCallback<HttpResponse> responseCallback, HighAvailabilityStrategy highAvailabilityStrategy, String apiName) {
        throw new UnsupportedOperationException();
    }
}
