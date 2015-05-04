/*
 * Copyright 2015 Cisco Systems, Inc.
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

package com.cisco.oss.foundation.message;

/**
 * Basic interface for asynchronous call-back that are activated whenever new messages arrive.
 * Created by Yair Ogen on 24/04/2014.
 */
public interface ConcurrentMessageHandler {

    /**
     * the call back method
     * @param message the asynchronously arrived message
     */
    void onRecieveMessage(Message message);

    /**
     * Check whether the message could be process now or not
     * @param message
     * @return true if the message could be process now, otherwise return false
     */
    public boolean isDispatchable(Message message);

    /**
     * The message is dispatchable now and will be added to the worker thread-pool
     * @param message
     */
    public void onDispatchMessage(Message message);

    /**
     * Do the business logic
     * @param message
     */
    public void process(Message message);

    /**
     * The 'process'  of the method has been completed successfully
     * @param message
     */
    public void onCompleteMessage(Message message);

    /**
     * The 'process' method has been completed with exception
     * @param message
     * @param throwable
     */
    public void onException(Message message, Throwable throwable);
}
