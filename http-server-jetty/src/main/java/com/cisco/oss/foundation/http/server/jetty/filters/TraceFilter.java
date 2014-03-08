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

package com.cisco.oss.foundation.http.server.jetty.filters;

import com.cisco.oss.foundation.http.server.AbstractInfraHttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TraceFilter extends AbstractInfraHttpFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceFilter.class);

    public TraceFilter(String serviceName) {
        super(serviceName);
    }


    @Override
    public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // only interested in http requests
        if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpResp = (HttpServletResponse) response;

            String originalClient = getOriginalClient(httpReq);
            String url = httpReq.getRequestURL().toString() + (httpReq.getQueryString() != null ? ("?" + httpReq.getQueryString()) : "");
            LOGGER.info("Received HTTP {} request from {}:{}. URL: {}", httpReq.getMethod(), originalClient, request.getRemotePort(), url);

            // allow skipping / excluding by details in the request
            if (isExcluded(httpReq)) {
                // pass request through, without tracing
                chain.doFilter(request, response);
            } else {
                // trace the request / response
                TraceLogger tracer = newTracer();
                TraceServletRequest traceReq = new TraceServletRequest(httpReq, tracer);
                TraceServletResponse traceResp = new TraceServletResponse(httpResp, tracer);
                chain.doFilter(traceReq, traceResp);
                if (httpReq.isAsyncStarted()) {
                    AsyncContext async = httpReq.getAsyncContext();
                    async.addListener(tracer);
                } else {
                    tracer.close();
                }
            }

            LOGGER.info("Sending HTTP response to " + originalClient + ":" + request.getRemotePort());

        } else {
            // pass request through unchanged
            chain.doFilter(request, response);
        }
    }

    private boolean isExcluded(HttpServletRequest httpReq) {
        // TODO add support to exclude trace behavior on specific requests
        // return false to trace everything
        return false;
    }

    private TraceLogger newTracer() throws IOException {
        return new TraceLogger();
    }

    @Override
    protected String getKillSwitchFlag() {
        return "http.traceFilter.isEnabled";
    }

    @Override
    protected boolean isEnabledByDefault() {
        return false;
    }


}
