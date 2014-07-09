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

package com.cisco.oss.foundation.http.apache;

import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpResponse;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Yair Ogen on 1/16/14.
 */
public class ApacheHttpResponse implements HttpResponse {

    private org.apache.http.HttpResponse httpResponse = null;
    private URI requestUri = null;
    private String responseBody;
    private boolean isClosed = false;

    public ApacheHttpResponse(org.apache.http.HttpResponse httpResponse, URI requestUri, boolean autoCloseable) {
        this.httpResponse = httpResponse;
        this.requestUri = requestUri;
        if (autoCloseable) {
            if (hasResponseBody()) {
                responseBody = getResponseAsString();
            }
            close();
        }
    }

    @Override
    public int getStatus() {
        return httpResponse.getStatusLine().getStatusCode();
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();

        Header[] allHeaders = httpResponse.getAllHeaders();
        for (Header header : allHeaders) {
            builder.put(header.getName(), header.getValue());
        }
        return builder.build().asMap();
    }

    @Override
    public boolean hasResponseBody() {
        try {
            return httpResponse.getEntity() != null && httpResponse.getEntity().getContent() != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public byte[] getResponse() {
        if (hasResponseBody()) {
            if (!isClosed) {
                try {
                    return EntityUtils.toByteArray(httpResponse.getEntity());
                } catch (IOException e) {
                    throw new ClientException(e.toString(), e);
                }
            } else {
                return responseBody.getBytes();
            }
        } else {
            return new byte[0];
        }
    }

    @Override
    public String getResponseAsString() {
        if (hasResponseBody()) {
            if (!isClosed) {
                try {
                    return EntityUtils.toString(httpResponse.getEntity());
                } catch (IOException e) {
                    throw new ClientException(e.toString(), e);
                }
            } else {
                return responseBody;
            }
        } else {
            return "";
        }
    }

    @Override
    public InputStream getInputStream() {
        if (hasResponseBody()) {
            try {
                return httpResponse.getEntity().getContent();
            } catch (IOException e) {
                throw new ClientException(e.toString(), e);
            }
        } else {
            return null;
        }
    }

    @Override
    public URI getRequestedURI() {
        return requestUri;
    }

    @Override
    public boolean isSuccess() {
        boolean isSuccess = false;
        int status = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : null;
        isSuccess = status / 100 == 2;
        return isSuccess;
    }

    @Override
    public void close() {
        try {
            if (httpResponse instanceof CloseableHttpResponse) {
                ((CloseableHttpResponse) httpResponse).close();
            }
        } catch (IOException e) {
            throw new ClientException(e.toString(), e);
        }
        isClosed = true;
    }
}
