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

package com.cisco.oss.foundation.http;

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
     * get the HTTP response as a String
     * @param charset use this charset is possible when creating the string response
     * @return
     */
    String getResponseAsString(String charset);

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
