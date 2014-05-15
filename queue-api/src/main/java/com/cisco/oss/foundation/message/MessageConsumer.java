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

package com.cisco.oss.foundation.message;

/**
 * basic interface that defines API available for message consumers.
 * Note: in some implementations this instance may NOT be thread safe.
 * Created by Yair Ogen on 24/04/2014.
 */
public interface MessageConsumer {

    /**
     * a blocking synced receive API.
     * @return the returned message
     */
    Message receive();

    /**
     * a blocking synced receive API.
     * @param timeout - timeout in millis how long to block
     * @return the returned message
     */
    Message receive(long timeout);

    /**
     * register a handler that will receive messages asynchronously. This method will no block.
     * Note: in many cases you should not use this interface directly. Instead extend a abstract class from the vendor specifc API implementation
     * @param messageHandler - the message handler instance that will be called-back when new messages arrive.
     */
    void registerMessageHandler(MessageHandler messageHandler);

    /**
     * close this consumer. This is useful for resource cleanup.
     * usually you don't need to run this API directly as implementation registers a shutdown hook for that.
     */
    void close();
}
