package com.cisco.oss.foundation.message;/*
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

import java.util.Map;

/**
 * basic interface that defines API available for message producers.
 * Note: in some implementations this instance may NOT be thread safe.
 * Created by Yair Ogen on 24/04/2014.
 */
public interface MessageProducer {

    /**
     * send a simple byte array based message
     * @param message the payload as byte array
     */
    void sendMessage(byte[] message);

    /**
     * send a simple string based message
     * @param message the payload as a string
     */
    void sendMessage(String message);

    /**
     * send a simple byte array based message with additional message headers
     * @param message the payload as byte array
     * @param messageHeaders the message headers
     */
    void sendMessage(byte[] message, Map<String,Object> messageHeaders);

    /**
     * send a simple string based message
     * @param message the payload as a string
     * @param messageHeaders the message headers
     */
    void sendMessage(String message, Map<String,Object> messageHeaders);

    void preSendMessage();

    void postSendMessage();

    /**
     * close this producer. This is useful for resource cleanup.
     * usually you don't need to run this API directly as implementation registers a shutdown hook for that.
     */
    void close();

}
