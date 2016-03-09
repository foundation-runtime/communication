package com.cisco.oss.foundation.message;

/**
 * Created by Yair Ogen (yaogen) on 09/03/2016.
 */
public class RabbitMQAdminException extends RuntimeException {

    public RabbitMQAdminException(String message) {
        super(message);
    }

    public RabbitMQAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
