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

import com.cisco.oss.foundation.http.HttpResponse;
import com.google.common.collect.ImmutableListMultimap;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Yair Ogen on 1/16/14.
 */
public class JettyHttpResponse implements HttpResponse {

    private Response httpResponse = null;
    private URI requestUri = null;


    public JettyHttpResponse(Response response, URI requestUri) {
        this.httpResponse = response;
        this.requestUri = requestUri;
    }

    @Override
    public int getStatus() {
        return httpResponse.getStatus();
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();

        HttpFields headers = httpResponse.getHeaders();
        for (HttpField header : headers) {
            builder.put(header.getName(), header.getValue());
        }
        return builder.build().asMap();
    }

    @Override
    public boolean hasResponseBody() {
        if (httpResponse instanceof ContentResponse) {
            ContentResponse contentResponse = (ContentResponse)httpResponse;
            return contentResponse.getContent() != null;
        } else {
            return false;
        }
    }

    @Override
    public byte[] getResponse() {
        if (hasResponseBody()) {
            ContentResponse contentResponse = (ContentResponse)httpResponse;
            return contentResponse.getContent();
        } else {
            return new byte[0];
        }
    }

    @Override
    public String getResponseAsString() {
        if (hasResponseBody()) {
            ContentResponse contentResponse = (ContentResponse)httpResponse;
            return contentResponse.getContentAsString();
        } else {
            return "";
        }
    }

    @Override
    public InputStream getInputStream() {
//        if (hasResponseBody()) {
            throw new UnsupportedOperationException();
//        } else {
//            return null;
//        }
    }

    @Override
    public URI getRequestedURI() {
        return requestUri;
    }

    @Override
    public boolean isSuccess() {
        boolean isSuccess = false;
        int status = httpResponse.getStatus() >= 0 ? httpResponse.getStatus() : 0;
        isSuccess = status / 100 == 2;
        return isSuccess;
    }

    @Override
    public void close() {
    }
}
