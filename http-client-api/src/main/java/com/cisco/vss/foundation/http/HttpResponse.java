package com.cisco.vss.foundation.http;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Representation of a HttpResponse. Implementation libraries must implement this interface.
 * Created by Yair Ogen on 1/1/14.
 */
public interface HttpResponse {

    /**
     * get the http status.
     * @return
     */
    int getStatus();

    /**
     * get the HTTP headers
     * @return
     */
    Map<String, Collection<String>> getHeaders();

    /**
     * returns true if the HTTP response has content
     * @return
     */
    boolean hasResponseBody();

    /**
     * get the HTTP response as byte array
     * @return
     */
    byte[] getResponse();

    /**
     * get the HTTP response as a String
     * @return
     */
    String getResponseAsString();

    /**
     * get the HTTP response as an InputStream
     * @return
     */
    InputStream getInputStream();

    /**
     * get the original request URI that was used to generate this response
     * @return
     */
    URI getRequestedURI();

    /**
     * return true if the request is considered to be successful.
     * @return
     */
    boolean isSuccess();

    /**
     * close the response.
     * NOT IMPLEMENTED
     */
    void close();
}
