package com.cisco.vss.foundation.http.apache;

import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpResponse;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
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

    private CloseableHttpResponse httpResponse = null;
    private URI requestUri = null;

    public ApacheHttpResponse(CloseableHttpResponse closeableHttpResponse, URI requestUri) {
        this.httpResponse = closeableHttpResponse;
        this.requestUri = requestUri;
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
            try {
                return EntityUtils.toByteArray(httpResponse.getEntity());
            } catch (IOException e) {
                throw new ClientException(e.toString(), e);
            }
        } else {
            return new byte[0];
        }
    }

    @Override
    public String getResponseAsString() {
        if (hasResponseBody()) {
            try {
                return EntityUtils.toString(httpResponse.getEntity());
            } catch (IOException e) {
                throw new ClientException(e.toString(), e);
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
        int status = httpResponse.getStatusLine() != null? httpResponse.getStatusLine().getStatusCode(): null;
        isSuccess = status / 100 == 2;
        return isSuccess;
    }

    @Override
    public void close() {
        try {
            httpResponse.close();
        } catch (IOException e) {
            throw new ClientException(e.toString(), e);
        }
    }
}
