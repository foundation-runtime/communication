package com.cisco.oss.foundation.loadbalancer;

import java.io.IOException;


/**
 * This exception is thrown is a given strategy implementation has looped all its registered servers and none were found active.
 *
 * @author Yair Ogen
 */
public class NoActiveServersIOException extends IOException {

    private static final long serialVersionUID = 1216283914923976885L;


    /**
     * NoActiveServersException ctor.
     *
     * @param message the exception message.
     */
    public NoActiveServersIOException(final String message) {
        super(message);

    }


}
