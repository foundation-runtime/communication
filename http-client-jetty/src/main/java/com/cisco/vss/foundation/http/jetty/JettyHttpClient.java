package com.cisco.vss.foundation.http.jetty;

import com.cisco.vss.foundation.flowcontext.FlowContext;
import com.cisco.vss.foundation.flowcontext.FlowContextFactory;
import com.cisco.vss.foundation.http.*;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;
import com.cisco.vss.foundation.loadbalancer.InternalServerProxy;
import com.cisco.vss.foundation.loadbalancer.RequestTimeoutException;
import com.google.common.base.Joiner;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Yair Ogen on 1/16/14.
 */
public class JettyHttpClient<S extends HttpRequest, R extends HttpResponse> extends AbstractHttpClient<S, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyHttpClient.class);

    public JettyHttpClient(String apiName) {
        super(apiName);
        configureClient();
    }
    public JettyHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE strategyType) {
        super(apiName, strategyType);
        configureClient();
    }

    private org.eclipse.jetty.client.HttpClient httpClient = new HttpClient();

    @Override
    protected void configureClient() {

        httpClient.setConnectTimeout(metadata.getConnectTimeout());
        httpClient.setIdleTimeout(metadata.getIdleTimeout());
        httpClient.setMaxConnectionsPerDestination(metadata.getMaxConnectionsPerAddress());
        httpClient.setMaxRequestsQueuedPerDestination(metadata.getMaxQueueSizePerAddress());
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new ClientException("failed to start jetty http client: " + e, e);
        }

    }

    @Override
    public HttpResponse execute(HttpRequest request) throws IOException{

        Request httpRequest = prepareRequest(request);


        try {
            ContentResponse contentResponse = httpRequest.send();
            return new JettyHttpResponse(contentResponse, httpRequest.getURI());
        } catch (InterruptedException e) {
            throw new ClientException(e.toString(), e);
        } catch (TimeoutException e) {
            throw new RequestTimeoutException(e.toString(), e);
        } catch (ExecutionException e) {
            throw new ClientException(e.toString(), e);
        }
    }

    @Override
    public void execute(HttpRequest request, ResponseCallback responseCallback, HighAvailabilityStrategy highAvailabilityStrategy, String apiName) {

        InternalServerProxy serverProxy = highAvailabilityStrategy.getServerProxy(request);
        Throwable lastCaugtException = null;


        if (serverProxy == null) {
            // server proxy will be null if the configuration was not
            // configured properly
            // or if all the servers are passivated.
            highAvailabilityStrategy.handleNullserverProxy(apiName, lastCaugtException);
        }

        request = updateRequestUri((S)request, serverProxy);

        final HttpRequest tempRequest = request;

        LOGGER.info("sending request: {}", request.getUri());
        final FlowContext fc = FlowContextFactory.getFlowContext();
        Request httpRequest = prepareRequest(request).onRequestQueued(new Request.QueuedListener() {
            @Override
            public void onQueued(Request jettyRequest) {
                FlowContextFactory.addFlowContext(((S) tempRequest).getFlowContext());
            }
        }).onRequestBegin(new Request.BeginListener() {
            @Override
            public void onBegin(Request jettyRequest) {
                FlowContextFactory.addFlowContext(((S) tempRequest).getFlowContext());
            }
        }).onRequestFailure(new Request.FailureListener() {
            @Override
            public void onFailure(Request jettyRequest, Throwable failure) {
                FlowContextFactory.addFlowContext(((S) tempRequest).getFlowContext());
            }
        });
        httpRequest.send(new JettyCompleteListener(this, request, responseCallback, serverProxy, highAvailabilityStrategy, apiName));

    }


    private Request prepareRequest(HttpRequest request) {
        Request httpRequest = httpClient.newRequest(request.getUri())
                .timeout(metadata.getReadTimeout(), TimeUnit.MILLISECONDS)
                .method(request.getHttpMethod().method());


        byte[] entity = request.getEntity();
        if(entity != null){
            httpRequest = httpRequest.content(new BytesContentProvider(entity),request.getContentType());
        }

        Joiner joiner = Joiner.on(",").skipNulls();

        Map<String, Collection<String>> headers = request.getHeaders();
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : headers.entrySet()) {
            String key = stringCollectionEntry.getKey();
            Collection<String> stringCollection = stringCollectionEntry.getValue();
            String value = joiner.join(stringCollection);
            httpRequest = httpRequest.header(key, value);
        }

        for (Map.Entry<String, Collection<String>> stringCollectionEntry : request.getQueryParams().entrySet()) {
            String key = stringCollectionEntry.getKey();
            Collection<String> stringCollection = stringCollectionEntry.getValue();
            String value = joiner.join(stringCollection);
            httpRequest = httpRequest.param(key, value);
        }
        return httpRequest;
    }
}
