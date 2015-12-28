package com.cisco.oss.foundation.http.netlifx.apache;

import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpResponse;
import com.cisco.oss.foundation.http.apache.ApacheHttpResponse;
import com.google.common.reflect.TypeToken;
import com.netflix.client.http.CaseInsensitiveMultiMap;
import com.netflix.client.http.HttpHeaders;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Yair Ogen (yaogen) on 13/12/2015.
 */
public class ApacheNetflixHttpResponse extends ApacheHttpResponse implements com.netflix.client.http.HttpResponse {
    private URI requestUri = null;
    private byte[] responseBody;
    private boolean isClosed = false;
    private com.netflix.client.http.HttpResponse netflixHttpResponse;

    public ApacheNetflixHttpResponse(org.apache.http.HttpResponse httpResponse, URI requestUri, boolean autoCloseable) {
        super(httpResponse, requestUri, autoCloseable);
    }
    public ApacheNetflixHttpResponse(com.netflix.client.http.HttpResponse netflixHttpResponse) {
        super();
        this.netflixHttpResponse = netflixHttpResponse;
    }

//    @Override
//    public int getStatus() {
//        return httpResponse.getStatus();
//    }
//
//    @Override
//    public Map<String, Collection<String>> getHeaders() {
//        Map<String, Collection<String>> headers = new HashMap<>();
//        HttpHeaders httpHeaders = httpResponse.getHttpHeaders();
//        List<Map.Entry<String, String>> allHeaders = httpHeaders.getAllHeaders();
//        for (Map.Entry<String, String> allHeader : allHeaders) {
//            headers.put(allHeader.getKey(),httpHeaders.getAllValues(allHeader.getKey()));
//        }
////        return httpResponse.getHttpHeaders();
//        return headers;
//    }
//
//    @Override
//    public boolean hasResponseBody() {
//        return httpResponse.hasEntity();
//    }
//
//    @Override
//    public byte[] getResponse() {
//        try {
//            return httpResponse.getEntity(byte[].class);
//        } catch (Exception e) {
//            throw new ClientException(e.toString(),e);
//        }
//    }
//
//    @Override
//    public String getResponseAsString() {
//        try {
//            return httpResponse.getEntity(String.class);
//        } catch (Exception e) {
//            throw new ClientException(e.toString(),e);
//        }
//    }
//
//    @Override
//    public String getResponseAsString(String charset) {
//        try {
//            return httpResponse.getEntity(String.class);
//        } catch (Exception e) {
//            throw new ClientException(e.toString(),e);
//        }
//    }
//
//    @Override
//    public InputStream getInputStream() {
//        return httpResponse.getInputStream();
//    }
//
//    @Override
//    public URI getRequestedURI() {
//        return httpResponse.getRequestedURI();
//    }
//
//    @Override
//    public boolean isSuccess() {
//        return httpResponse.isSuccess();
//    }
//
//    @Override
//    public void close() {
//        httpResponse.close();
//    }

    @Override
    public String getStatusLine() {
        return netflixHttpResponse.getStatusLine();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
//        HttpHeaders httpHeaders = new CaseInsensitiveMultiMap();
//        return httpHeaders;
        return netflixHttpResponse.getHttpHeaders();
    }

    @Override
    public boolean hasEntity() {
//        return hasResponseBody();
        return netflixHttpResponse.hasEntity();
    }

    @Override
    public <T> T getEntity(Class<T> type) throws Exception {
        return netflixHttpResponse.getEntity(type);
    }

    @Override
    public <T> T getEntity(Type type) throws Exception {
        return netflixHttpResponse.getEntity(type);
    }

    @Override
    public <T> T getEntity(TypeToken<T> type) throws Exception {
        return netflixHttpResponse.getEntity(type);
    }

    @Override
    public Object getPayload() throws com.netflix.client.ClientException {
//        return getResponse();
        return netflixHttpResponse.getPayload();
    }

    @Override
    public boolean hasPayload() {
//        return hasResponseBody();
        return netflixHttpResponse.hasPayload();
    }
}
