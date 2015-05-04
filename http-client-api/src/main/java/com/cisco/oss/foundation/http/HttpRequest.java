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

package com.cisco.oss.foundation.http;

import com.cisco.oss.foundation.flowcontext.FlowContext;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.loadbalancer.ClientRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

/**
 * Representation of a Http Request. This class is immutable. you should build it using the builder.
 * Created by Yair Ogen on 12/30/13.
 */
public class HttpRequest implements ClientRequest{

    protected URI uri;
    private Multimap<String, String> headers = ArrayListMultimap.create();
    private Multimap<String, String> queryParams = ArrayListMultimap.create();
    private byte[] entity;
    private HttpMethod httpMethod;
    private String lbKey;
    private FlowContext flowContext;
    private String contentType = "application/json";

    protected boolean httpsEnabled = false;
    protected boolean retryOnServerBusy = false;


    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    private HttpRequest() {
        this.httpMethod = HttpMethod.GET;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    public String getLbKey() {
        return lbKey;
    }

    protected final HttpRequest setLbKey(String lbKey) {
        this.lbKey = lbKey;
        return this;
    }

    public final URI getUri() {
        return uri;
    }

    protected final HttpRequest setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Map<String, Collection<String>> getQueryParams() {
        return queryParams.asMap();
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Map<String, Collection<String>> getHeaders() {
        return headers.asMap();
    }

    public byte[] getEntity() {
        return entity;
    }

    /**
     * Return a new instance of HttpRequest replacing the URI.
     */
    HttpRequest replaceUri(URI newURI) {
        return (new Builder()).uri(newURI)
                .entity(this.getEntity())
                .headers(this.headers)
                .lbKey(this.lbKey)
                .contentType(this.contentType)
                .queryParams(this.queryParams)
                .FlowContext(this.flowContext)
                .httpMethod(this.getHttpMethod()).build();
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "uri=" + uri +
                ", headers=" + headers +
                ", queryParams=" + queryParams +
                ", httpMethod=" + httpMethod +
                ", contentType='" + contentType + '\'' +
                '}';
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    /**
     * The builder for the HttpRequest
     */
    public static class Builder {

        private HttpRequest request = new HttpRequest();

        public Builder uri(URI uri) {
            request.setUri(uri);
            return this;
        }

        public Builder uri(String uri) {
            try {
                request.setUri(new URI(uri));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder lbKey(String lbKey) {
            request.setLbKey(lbKey);
            return this;
        }

        public Builder contentType(String contentType) {
            request.setContentType(contentType);
            return this;
        }

        public Builder header(String name, String value) {
            request.headers.put(name, value);
            return this;
        }

        public Builder headers(Multimap<String, String> headers) {
            request.headers = headers;
            return this;
        }

        public Builder queryParams(Multimap<String, String> queryParams) {
            request.queryParams = queryParams;
            return this;
        }

        public Builder queryParams(String name, String value) {
            request.queryParams.put(name, value);
            return this;
        }

        public Builder entity(byte[] entity) {
            request.entity = entity;
            return this;
        }

        public Builder entity(String entity) {
            request.entity = entity.getBytes(Charset.forName("UTF-8"));
            return this;
        }

        public Builder entity(String entity, String charset) {
            request.entity = entity.getBytes(Charset.forName(charset));
            return this;
        }

        public Builder FlowContext(FlowContext flowContext) {
            request.flowContext = flowContext;
            return this;
        }

        public Builder httpMethod(HttpMethod HttpMethod) {
            request.httpMethod = HttpMethod;
            return this;
        }

        public Builder https() {
            request.httpsEnabled = true;
            return this;
        }

        public Builder retryOnServerBusy() {
            request.retryOnServerBusy = true;
            return this;
        }

        public HttpRequest build() {
            request.flowContext = FlowContextFactory.getFlowContext();
            request.headers.removeAll("FLOW_CONTEXT");
            request.headers.put("FLOW_CONTEXT", FlowContextFactory.serializeNativeFlowContext());
            return request;
        }
    }

}

