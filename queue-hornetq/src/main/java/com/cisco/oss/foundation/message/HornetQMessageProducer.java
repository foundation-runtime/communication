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
import org.hornetq.api.core.HornetQObjectClosedException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.core.message.impl.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A HornetQ producer wrapper. NOTE: This class is thread safe although wraps HornetQ ClientProducer
 * which is not thread safe. The internal implementation is to provide a single threaded producer instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class HornetQMessageProducer extends AbstractMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessageProducer.class);
    private static final ThreadLocal<ClientProducer> producer = new ThreadLocal<ClientProducer>();
    private static final Set<ClientProducer> producers = new HashSet<ClientProducer>();
    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String groupId = "";

    private long expiration = 1800000;

    HornetQMessageProducer(String producerName) {

        super(producerName);
    }

    @Override
    public String getProducerImpl() {
        return "CORE";
    }

    private ClientProducer getProducer() {
        try {
            if (producer.get() == null) {
                String realQueueName = createQueueIfNeeded();
                ClientProducer clientProducer = HornetQMessagingFactory.getSession().createProducer(realQueueName);
                producers.add(clientProducer);
                producer.set(clientProducer);
            }
//                ClientProducer clientProducer = producer.get();
//                String realQueueName = createQueueIfNeeded();
//                if (clientProducer == null) {
//                    ClientProducer clientProducerTmp = HornetQMessagingFactory.getSession().createProducer(realQueueName);
//                    producers.add(clientProducerTmp);
//                    producer.set(clientProducerTmp);
//                    clientProducer = clientProducerTmp;
//                }
                return producer.get();

//                if (clientProducer.isClosed()) {
//                    producers.remove(clientProducer);
//                    producer.set(null);
//                    try {
//                        ClientProducer clientProducerTmp = HornetQMessagingFactory.getSession().createProducer(realQueueName);
//                        producers.add(clientProducerTmp);
//                        producer.set(clientProducerTmp);
//                        return clientProducerTmp;
//                    } catch (Exception e) {
//                        LOGGER.error("can't create queue producer: {}", e, e);
//                        throw new QueueException(e);
//                    }
//                throw new QueueException("producer is closed. probably server was restarted");
//                }

//                return clientProducer;
        } catch (Exception e) {
            LOGGER.error("can't create queue producer: {}", e, e);
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

        //update expiration
        expiration = subset.getLong("queue.expiration", 1800000);
        groupId = subset.getString("queue.groupId", "");

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
            clientMessage.setExpiration(System.currentTimeMillis() + expiration);
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
            clientMessage.setExpiration(System.currentTimeMillis() + expiration);
            clientMessage.getBodyBuffer().writeString(message);
            getProducer().send(clientMessage);

        } catch (HornetQObjectClosedException e) {
            LOGGER.warn("can't send message: {}", e, e);
            producer.set(null);
            HornetQMessagingFactory.sessionThreadLocal.set(null);
        }catch (Exception e) {
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

        if (StringUtils.isNoneBlank(groupId) && messageHeaders.get(groupId) != null) {
            clientMessage.putStringProperty(MessageImpl.HDR_GROUP_ID.toString(), messageHeaders.get(groupId).toString());
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
