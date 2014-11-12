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

import com.cisco.oss.foundation.http.HttpRequest;
import com.cisco.oss.foundation.http.ResponseCallback;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.cisco.oss.foundation.loadbalancer.InternalServerProxy;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Created by Yair Ogen on 1/23/14.
 */
public class JettyCompleteListener extends BufferingResponseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyCompleteListener.class);
    private JettyHttpClient jettyHttpClient;
    private ResponseCallback<JettyHttpResponse> responseCallback;
    private InternalServerProxy serverProxy;
    private LoadBalancerStrategy loadBalancerStrategy;
    private String apiName;
    private HttpRequest request;


    public JettyCompleteListener(JettyHttpClient jettyHttpClient, HttpRequest request, ResponseCallback<JettyHttpResponse> responseCallback, InternalServerProxy serverProxy, LoadBalancerStrategy loadBalancerStrategy, String apiName) {
        this.jettyHttpClient = jettyHttpClient;
        this.responseCallback = responseCallback;
        this.serverProxy = serverProxy;
        this.loadBalancerStrategy = loadBalancerStrategy;
        this.apiName = apiName;
        this.request = request;
    }

    @Override
    public void onComplete(Result result) {
        Throwable failure = result.getFailure();
        URI uri = result.getRequest().getURI();
        if (failure != null) {
            try {
                loadBalancerStrategy.handleException(apiName, serverProxy, failure);
            } catch (Exception e) {
                LOGGER.error("Error running request {}. Error is: {}", uri, e);
                responseCallback.failed(e);
            }

            try {
                jettyHttpClient.execute(request, responseCallback, loadBalancerStrategy, apiName);
            } catch (Throwable e) {
                result.getRequest().abort(e);
                responseCallback.failed(e);
            }

        } else {
            serverProxy.setCurrentNumberOfAttempts(0);
            serverProxy.setFailedAttemptTimeStamp(0);
            LOGGER.info("got response: {}", uri);

            Response response = result.getResponse();

            byte[] content = getContent();
            if (content != null) {
                response = new HttpContentResponse(response,content,null, getEncoding());
            }

            responseCallback.completed(new JettyHttpResponse(response, uri));
        }

    }
}
