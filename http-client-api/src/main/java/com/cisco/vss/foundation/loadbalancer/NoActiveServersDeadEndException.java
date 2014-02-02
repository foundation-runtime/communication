package com.cisco.vss.foundation.loadbalancer;


/**
 * This exception is thrown is a given strategy implementation has looped all its registered servers and none were found active.
 *
 * @author Yair Ogen
 */
public class NoActiveServersDeadEndException extends NoActiveServersException {

    private static final long serialVersionUID = 1216283914923976885L;

    /**
     * NoActiveServersException ctor.
     *
     * @param message the exception message.
     * @param cause   the cause of this exception.
     */
    public NoActiveServersDeadEndException(final String message, final Throwable cause) {
        super(message, cause);

    }


}
