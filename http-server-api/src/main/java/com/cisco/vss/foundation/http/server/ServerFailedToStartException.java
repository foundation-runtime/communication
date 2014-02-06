package com.cisco.vss.foundation.http.server;

/**
 * User: Yair Ogen
 * Date: 5/1/13
 */
public class ServerFailedToStartException extends RuntimeException{
    public ServerFailedToStartException(Throwable cause) {
        super(cause);
    }
}
