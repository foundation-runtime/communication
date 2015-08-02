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

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.HornetQObjectClosedException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.SessionFailureListener;
import org.hornetq.core.message.impl.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A HornetQ producer wrapper. NOTE: This class is thread safe although wraps HornetQ ClientProducer
 * which is not thread safe. The internal implementation is to provide a single threaded producer instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class HornetQMessageProducer extends AbstractMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessageProducer.class);
    private static final ThreadLocal<List<ClientProducer>> producer = new ThreadLocal<List<ClientProducer>>();
    private static final Set<ClientProducer> producersSet = new HashSet<ClientProducer>();
    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String groupId = "";
    private long expiration = 1800000;

    private AtomicInteger nextIndex = new AtomicInteger(0);

    HornetQMessageProducer(String producerName) {

        super(producerName);
    }

    protected int nextNode(int serverProxisListSize) {
        int index = nextIndex.incrementAndGet() % serverProxisListSize;
        return index;
    }

    @Override
    public String getProducerImpl() {
        return "CORE";
    }

    private ClientProducer getProducer(String groupIdentifier) {
        try {
            if (producer.get() == null) {
                String realQueueName = createQueueIfNeeded();
                List<ClientProducer> producers = new ArrayList<>();
                for (Pair<ClientSession,SessionFailureListener> clientSession : HornetQMessagingFactory.getSession()) {
                    ClientProducer clientProducer = clientSession.getLeft().createProducer(realQueueName);
                    producersSet.add(clientProducer);
                    producers.add(clientProducer);
                }
                producer.set(producers);
            }

            ClientProducer clientProducer = null;
            if(groupIdentifier != null){
                clientProducer = producer.get().get(groupIdentifier.hashCode()%producer.get().size());
            }else{
                clientProducer = producer.get().get(nextNode(producer.get().size()));
            }
            return clientProducer;

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

        String groupIdentifier = null;
        if (StringUtils.isNoneBlank(groupId) && messageHeaders.get(groupId) != null) {
            groupIdentifier = messageHeaders.get(groupId).toString();
        }


        try {

            ClientMessage clientMessage = getClientMessage(messageHeaders, groupIdentifier);
            clientMessage.setExpiration(System.currentTimeMillis() + expiration);
            clientMessage.getBodyBuffer().writeBytes(message);
            getProducer(groupIdentifier).send(clientMessage);

        } catch (Exception e) {
            LOGGER.error("can't send message: {}", e, e);
            throw new QueueException(e);
        }

    }

    @Override
    public void sendMessage(String message, Map<String, Object> messageHeaders) {
        try {

            sendMessageInternal(message, messageHeaders);

        } catch (HornetQObjectClosedException e) {
            LOGGER.warn("can't send message. will try one reconnect. error is: {}", e);
            producer.set(null);
            HornetQMessagingFactory.sessionThreadLocal.set(null);
            //retry once it came back till now
            try {
                sendMessageInternal(message, messageHeaders);
            } catch (Exception e1) {
                LOGGER.error("can't send message: {}", e1, e1);
                throw new QueueException(e1);
            }
        } catch (Exception e) {
            LOGGER.error("can't send message: {}", e, e);
            throw new QueueException(e);
        }
    }

    private void sendMessageInternal(String message, Map<String, Object> messageHeaders) throws HornetQException {
        String groupIdentifier = null;
        if (StringUtils.isNoneBlank(groupId) && messageHeaders.get(groupId) != null) {
            groupIdentifier = messageHeaders.get(groupId).toString();
        }

        ClientMessage clientMessage = getClientMessage(messageHeaders, groupIdentifier);
        clientMessage.setExpiration(System.currentTimeMillis() + expiration);
        clientMessage.getBodyBuffer().writeString(message);
        getProducer(groupIdentifier).send(clientMessage);
    }

    private ClientMessage getClientMessage(Map<String, Object> messageHeaders, String groupIdentifier) {

        ClientMessage clientMessage = HornetQMessagingFactory.getSession().get(0).getLeft().createMessage(true);

        clientMessage.putStringProperty(QueueConstants.FLOW_CONTEXT_HEADER, FlowContextFactory.serializeNativeFlowContext());

        for (Map.Entry<String, Object> headers : messageHeaders.entrySet()) {
            clientMessage.putObjectProperty(headers.getKey(), headers.getValue());
        }

        return clientMessage;
    }

    @Override
    public void close() {
        if (getProducer(null) != null) {
            try {
                for (ClientProducer clientProducer : producersSet) {
                    clientProducer.close();
                }
            } catch (HornetQException e) {
                LOGGER.error("can't close producer, error: {}", e, e);
            }
        }
    }
}
