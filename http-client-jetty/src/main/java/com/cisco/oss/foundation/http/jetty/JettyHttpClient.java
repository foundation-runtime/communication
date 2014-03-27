/*
 * Copyright 2014 Cisco Systems, Inc.
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

package com.cisco.oss.foundation.http.jetty;

import com.cisco.oss.foundation.flowcontext.FlowContext;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.http.*;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.cisco.oss.foundation.loadbalancer.InternalServerProxy;
import com.cisco.oss.foundation.loadbalancer.RequestTimeoutException;
import com.google.common.base.Joiner;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public JettyHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing) {
        super(apiName, configuration, enableLoadBalancing);
        configureClient();
    }

    public JettyHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE strategyType, Configuration configuration, boolean enableLoadBalancing) {
        super(apiName, strategyType, configuration, enableLoadBalancing);
        configureClient();
    }

    private org.eclipse.jetty.client.HttpClient httpClient = new HttpClient();

    @Override
    protected void configureClient() {

        boolean addSslSupport = StringUtils.isNotEmpty(metadata.getKeyStorePath()) && StringUtils.isNotEmpty(metadata.getKeyStorePassword());
        if(addSslSupport){

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(metadata.getKeyStorePath());
            sslContextFactory.setKeyStorePassword(metadata.getKeyStorePassword());

            boolean addTrustSupport = StringUtils.isNotEmpty(metadata.getTrustStorePath()) && StringUtils.isNotEmpty(metadata.getTrustStorePassword());
            if(addTrustSupport){
                sslContextFactory.setTrustStorePath(metadata.getTrustStorePath());
                sslContextFactory.setTrustStorePassword(metadata.getTrustStorePassword());
            }else{
                sslContextFactory.setTrustAll(true);
            }

            httpClient = new  HttpClient(sslContextFactory);
        }

        httpClient.setConnectTimeout(metadata.getConnectTimeout());
        httpClient.setIdleTimeout(metadata.getIdleTimeout());
        httpClient.setMaxConnectionsPerDestination(metadata.getMaxConnectionsPerAddress());
        httpClient.setMaxRequestsQueuedPerDestination(metadata.getMaxQueueSizePerAddress());
        httpClient.setFollowRedirects(followRedirects);

        try {
            httpClient.start();
        } catch (Exception e) {
            throw new ClientException("failed to start jetty http client: " + e, e);
        }

    }

    @Override
    public HttpResponse executeDirect(HttpRequest request){

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
        httpRequest.send(new JettyCompleteListener(this, request, responseCallback, serverProxy, loadBalancerStrategy, apiName));

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
