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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A HornetQ consumerThreadLocal wrapper. NOTE: This class is thread safe although wraps HornetQ ClientConsumer
 * which is not thread safe. The internal implementation is to provide a single threaded consumerThreadLocal instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class HornetQMessageConsumer implements MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessageConsumer.class);
    static final List<ClientConsumer> consumers = new CopyOnWriteArrayList<>();
    static final Map<String,MessageHandler> consumerInfo = new ConcurrentHashMap<>();
    public static final ThreadLocal<List<ClientConsumer>> consumerThreadLocal = new ThreadLocal<List<ClientConsumer>>();
    private String consumerName = "N/A";
    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String queueName = "";


    private AtomicInteger nextIndex = new AtomicInteger(0);

    HornetQMessageConsumer(String consumerName) {
        this.consumerName = consumerName;
    }

    protected int nextNode(int serverProxisListSize) {
        int index = nextIndex.incrementAndGet() % serverProxisListSize;
        return index;
    }

    private List<ClientConsumer> getConsumer(boolean fetchAll) {
        try {
            if (consumerThreadLocal.get() == null) {

                LOGGER.info("waiting for connection successful signal");
                HornetQMessagingFactory.INIT_READY.await();
                List<ClientConsumer> consumers = new ArrayList<>();
                for (Pair<ClientSession,SessionFailureListener> clientSessionSessionFailureListenerPair : HornetQMessagingFactory.getSession(FoundationQueueConsumerFailureListener.class)) {
                    String realQueueName = createQueueIfNeeded();
                    LOGGER.info("creating new consumerThreadLocal for: {}", consumerName);
                    ClientConsumer clientConsumer = clientSessionSessionFailureListenerPair.getLeft().createConsumer(realQueueName);
                    ((FoundationQueueConsumerFailureListener)clientSessionSessionFailureListenerPair.getRight()).setReconnectProperties(realQueueName, clientConsumer);
                    HornetQMessageConsumer.consumers.add(clientConsumer);
                    consumers.add(clientConsumer);
                }

                consumerThreadLocal.set(consumers);
            }
            return fetchAll ? consumerThreadLocal.get() : Collections.singletonList(consumerThreadLocal.get().get(consumerThreadLocal.get().size()));
        } catch (Exception e) {
            LOGGER.error("can't create queue consumerThreadLocal: {}", e, e);
            throw new QueueException(e);
        }
    }

    private String createQueueIfNeeded() {

        Configuration subset = configuration.subset(consumerName);
        queueName = subset.getString("queue.name");

        String filter = subset.getString("queue.filter", "");
        boolean isDurable = subset.getBoolean("queue.isDurable", true);
        boolean isSubscription = subset.getBoolean("queue.isSubscription", false);
        String subscribedTo = isSubscription ? subset.getString("queue.subscribedTo", "") : queueName;

        if (isSubscription) {
            if (StringUtils.isBlank(subscribedTo)) {
                throw new QueueException("Check Configuration - missing required subscribedTo name for consumerThreadLocal[" + consumerName + "] as it is marked as isSubscription=true");
            }
//            subscribedTo = "foundation." + subscribedTo;
        }

        if (StringUtils.isBlank(queueName)) {

            String rpmSoftwareName = System.getenv("_RPM_SOFTWARE_NAME");
            String artifactId = System.getenv("_ARTIFACT_ID");
            String componentName = "";


            if (StringUtils.isNoneBlank(rpmSoftwareName)) {
                componentName = rpmSoftwareName;
            } else {
                if (StringUtils.isNoneBlank(artifactId)) {
                    componentName = artifactId;
                }
            }

            if (StringUtils.isNoneBlank(componentName)) {
                queueName = componentName + "_" + subscribedTo;
            }


            if (StringUtils.isBlank(queueName)) {
                throw new QueueException("Check Configuration - missing required queue name for consumerThreadLocal: " + consumerName);
            }
        }

        String realQueueName = /*"foundation." + */queueName;

        boolean queueExists = false;

        for (Pair<ClientSession, SessionFailureListener> clientSessionSessionFailureListenerPair : HornetQMessagingFactory.getSession(FoundationQueueConsumerFailureListener.class)) {
            ClientSession clientSession = clientSessionSessionFailureListenerPair.getLeft();
            try {
                queueExists = clientSession.queueQuery(new SimpleString(realQueueName)).isExists();
                if(!queueExists){
                    clientSession.createQueue(isSubscription ? subscribedTo : realQueueName, realQueueName, filter, isDurable);
                }
            } catch (HornetQException e) {
                try {
                    clientSession.createQueue(isSubscription ? subscribedTo : realQueueName, realQueueName, filter, isDurable);
                } catch (HornetQException e1) {
                    throw new QueueException("Can't create queue: " + realQueueName + ". Error: " + e1, e1);
                }
            }
        }

        return realQueueName;

    }

    @Override
    public Message receive() {
        try {

            ClientMessage clientMessage = getConsumer(false).get(0).receive();
            clientMessage.acknowledge();

            return new HornetQMessage(clientMessage);

        } catch (HornetQException e) {
            LOGGER.error("can't receive message: {}", e, e);
            throw new QueueException(e);
        }
    }

    @Override
    public Message receive(long timeout) {
        try {

            ClientMessage clientMessage = getConsumer(false).get(0).receive(timeout);
            clientMessage.acknowledge();

            return new HornetQMessage(clientMessage);

        } catch (HornetQException e) {
            LOGGER.error("can't receive message: {}", e, e);
            throw new QueueException(e);
        }
    }

    @Override
    public void registerMessageHandler(MessageHandler messageHandler) {

        try {
            if (messageHandler instanceof org.hornetq.api.core.client.MessageHandler) {
                org.hornetq.api.core.client.MessageHandler handler = (org.hornetq.api.core.client.MessageHandler) messageHandler;
                consumerInfo.put(consumerName,messageHandler);
                List<ClientConsumer> consumer = getConsumer(true);
                for (ClientConsumer clientConsumer : consumer) {
                    clientConsumer.setMessageHandler(handler);
                }
            } else {
                throw new IllegalArgumentException("Using HornetQ consumerThreadLocal you must provide a valid HornetQ massage handler");
            }

        } catch (HornetQException e) {
            LOGGER.error("can't register a MessageHandler: {}", e, e);
            throw new QueueException(e);
        }
    }

    @Override
    public void close() {
        if (consumerThreadLocal != null) {
            try {
                for (ClientConsumer clientConsumer : consumers) {
                    clientConsumer.close();
                }
            } catch (HornetQException e) {
                LOGGER.error("can't close consumerThreadLocal, error: {}", e, e);
            }
        }
    }
}
