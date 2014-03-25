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

package com.cisco.oss.foundation.loadbalancer;


/**
 * This exception is thrown is a given strategy implementation has looped all its registered servers and none were found active.
 *
 * @author Yair Ogen
 */
public class NoActiveServersException extends RuntimeException {

    private static final long serialVersionUID = 1216283914923976885L;

    /**
     * NoActiveServersException ctor.
     *
     * @param message the exception message.
     * @param cause   the cause of this exception.
     */
    public NoActiveServersException(final String message, final Throwable cause) {
        super(message, cause);

    }


}
