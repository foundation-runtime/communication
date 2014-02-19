package com.cisco.vss.foundation.http;

/**
 * enumeration for teh different Http Methods supported by the {@code com.cisco.vss.foundation.http.HttpClient}.
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
