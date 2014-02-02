package com.cisco.vss.foundation.http;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Yair Ogen on 1/1/14.
 */
public interface HttpResponse {

    int getStatus();

    Map<String, Collection<String>> getHeaders();

    boolean hasResponseBody();

    byte[] getResponse();

    String getResponseAsString();

    InputStream getInputStream();

    URI getRequestedURI();

    boolean isSuccess();

    void close();
}
