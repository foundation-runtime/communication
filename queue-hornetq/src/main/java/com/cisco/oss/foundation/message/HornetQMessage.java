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

import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.core.message.impl.MessageImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * The hornetq implementation for the goundation Message interface
 * This is the class users will get when receiving messages using the hornetq flavor
 * Created by Yair Ogen on 24/04/2014.
 */
public class HornetQMessage implements Message {

    private ClientMessage clientMessage = null;

    public HornetQMessage(ClientMessage clientMessage){
        this.clientMessage = clientMessage;
    }

    @Override
    public String getPayloadAsString() {
        return clientMessage.getBodyBuffer().readString();
    }

    @Override
    public byte[] getPayloadAsBytes() {
        int readableBytes = clientMessage.getBodyBuffer().readableBytes();
        byte[] payload = new byte[readableBytes];
        clientMessage.getBodyBuffer().readBytes(payload);
        return payload;
    }

    @Override
    public Map<String, Object> getProperties() {

        if(clientMessage instanceof MessageImpl){
            MessageImpl message = (MessageImpl)clientMessage;
            return message.getProperties().getMap();
        }

        return new HashMap<String,Object>();
    }
}
