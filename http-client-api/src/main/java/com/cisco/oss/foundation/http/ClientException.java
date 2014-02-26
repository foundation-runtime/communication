package com.cisco.oss.foundation.http;

/**
 * generic runtime exception that wrapps most of the errors that can be thrown by a HttpClient
 * Created by Yair Ogen on 1/14/14.
 */
public class ClientException extends RuntimeException {

    protected ErrorType errorType = ErrorType.GENERAL;

    public ClientException() {
        super();
    }

    public ClientException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public enum ErrorType{
        GENERAL,
        CONFIGURATION,
        NUMBEROF_RETRIES_EXEEDED,
        NUMBEROF_RETRIES_NEXTSERVER_EXCEEDED,
        SOCKET_TIMEOUT_EXCEPTION,
        READ_TIMEOUT_EXCEPTION,
        UNKNOWN_HOST_EXCEPTION,
        CONNECT_EXCEPTION,
        CLIENT_THROTTLED,
        SERVER_THROTTLED,
        NO_ROUTE_TO_HOST_EXCEPTION,
        CACHE_MISSING;
    }
}
