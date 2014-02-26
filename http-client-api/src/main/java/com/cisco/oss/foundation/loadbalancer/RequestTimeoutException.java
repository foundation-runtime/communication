/**
 *
 */
package com.cisco.oss.foundation.loadbalancer;

/**
 * thrown when client recieves timeout from server
 *
 * @author Yair Ogen
 */
public class RequestTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 2231958446442004888L;

    public RequestTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
