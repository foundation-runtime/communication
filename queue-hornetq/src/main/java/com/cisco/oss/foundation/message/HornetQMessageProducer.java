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

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A HornetQ producer wrapper. NOTE: This class is thread safe although wraps HornetQ ClientProducer
 * which is not thread safe. The internal implementation is to provide a single threaded producer instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class HornetQMessageProducer implements MessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessageProducer.class);
    private static final ThreadLocal<ClientProducer> producer = new ThreadLocal<ClientProducer>();
    private static final Set<ClientProducer> producers = new HashSet<ClientProducer>();
    private String producerName = "N/A";
    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String queueName = "";

    HornetQMessageProducer(String producerName) {

        this.producerName = producerName;
    }

    private ClientProducer getProducer() {
        try {
            if (producer.get() == null) {
                String realQueueName = createQueueIfNeeded();
                ClientProducer clientProducer = HornetQMessagingFactory.getSession().createProducer(realQueueName);
                producers.add(clientProducer);
                producer.set(clientProducer);
            }
            return producer.get();
        } catch (Exception e) {
            LOGGER.error("can't create queue consumer: {}", e, e);
            throw new QueueException(e);
        }
    }

    private String createQueueIfNeeded() {

        Configuration subset = configuration.subset(producerName);
        queueName = subset.getString("queue.name");

        if (StringUtils.isBlank(queueName)) {
            throw new QueueException("Check Configuration - missing required queue name for producer: " + producerName);
        }

        String realQueueName = /*"foundation." + */queueName;


        boolean queueExists = false;

        try {
            queueExists = HornetQMessagingFactory.getSession().queueQuery(new SimpleString(realQueueName)).isExists();
        } catch (HornetQException e) {
            queueExists = false;
        }

//        if (!queueExists) {
//            boolean isDurable = subset.getBoolean("queue.isDurable", true);
//
//        }

        return realQueueName;
    }

    @Override
    public void sendMessage(byte[] message) {
        sendMessage(message, new HashMap<String, Object>());
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(message, new HashMap<String, Object>());
    }

    @Override
    public void sendMessage(byte[] message, Map<String, Object> messageHeaders) {

        try {

            ClientMessage clientMessage = getClientMessage(messageHeaders);
            clientMessage.getBodyBuffer().writeBytes(message);
            getProducer().send(clientMessage);

        } catch (Exception e) {
            LOGGER.error("can't send message: {}", e, e);
            throw new QueueException(e);
        }

    }

    @Override
    public void sendMessage(String message, Map<String, Object> messageHeaders) {
        try {

            ClientMessage clientMessage = getClientMessage(messageHeaders);
            clientMessage.getBodyBuffer().writeString(message);
            getProducer().send(clientMessage);

        } catch (Exception e) {
            LOGGER.error("can't send message: {}", e, e);
            throw new QueueException(e);
        }
    }

    private ClientMessage getClientMessage(Map<String, Object> messageHeaders) {

        ClientMessage clientMessage = HornetQMessagingFactory.getSession().createMessage(true);

        clientMessage.putStringProperty(QueueConstants.FLOW_CONTEXT_HEADER, FlowContextFactory.serializeNativeFlowContext());

        for (Map.Entry<String, Object> headers : messageHeaders.entrySet()) {
            clientMessage.putObjectProperty(headers.getKey(), headers.getValue());
        }

        return clientMessage;
    }

    @Override
    public void close() {
        if (getProducer() != null) {
            try {
                for (ClientProducer clientProducer : producers) {
                    clientProducer.close();
                }
            } catch (HornetQException e) {
                LOGGER.error("can't close producer, error: {}", e, e);
            }
        }
    }
}
