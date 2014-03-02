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

/**
 * enumeration for teh different Http Methods supported by the {@code com.cisco.oss.foundation.http.HttpClient}.
 * Created by Yair Ogen on 12/30/13.
 */
public enum HttpMethod {

    GET("GET"),
    PUT("PUT"),
    POST("POST"),
    DELETE("DELETE"),
    OPTIONS("OPTIONS"),
    HEAD("HEAD");

    private final String method;

    HttpMethod(String method) {
        this.method = method;
    }

    public String method() {
        return method;
    }
}
