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

import com.rabbitmq.client.GetResponse;

import java.util.Map;

/**
 * The RabbitMQ implementation for the goundation Message interface
 * This is the class users will get when receiving messages using the RabbitMQ flavor
 * Created by Yair Ogen on 24/04/2014.
 */
public class RabbitMQMessage implements Message {

    private GetResponse getResponse = null;
    private String consumerTag = null;

    public RabbitMQMessage(GetResponse getResponse, String consumerTag) {
        this.getResponse = getResponse;
        this.consumerTag = consumerTag;
    }

    @Override
    public String getPayloadAsString() {
        return new String(getResponse.getBody());
    }

    @Override
    public byte[] getPayloadAsBytes() {
        return getResponse.getBody();
    }

    @Override
    public Map<String, Object> getProperties() {

        return getResponse.getProps().getHeaders();
    }

    public Long getDeliveryTag() {
        return getResponse.getEnvelope().getDeliveryTag();
    }
}
