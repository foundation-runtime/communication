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
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A HornetQ consumer wrapper. NOTE: This class is thread safe although wraps HornetQ ClientConsumer
 * which is not thread safe. The internal implementation is to provide a single threaded consumer instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class HornetQMessageConsumer implements MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessageConsumer.class);
    private static final Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();
    private static final ThreadLocal<ClientConsumer> consumer = new ThreadLocal<ClientConsumer>();
    private String consumerName = "N/A";
    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String queueName = "";


    HornetQMessageConsumer(String consumerName) {
        this.consumerName = consumerName;
    }

    private ClientConsumer getConsumer() {
        try {
            if (consumer.get() == null) {
                String realQueueName = createQueueIfNeeded();
                LOGGER.info("creating new consumer for: {}", consumerName);
                ClientConsumer clientConsumer = HornetQMessagingFactory.getSession().createConsumer(realQueueName);
                consumers.add(clientConsumer);
                consumer.set(clientConsumer);
            }
            return consumer.get();
        } catch (Exception e) {
            LOGGER.error("can't create queue consumer: {}", e, e);
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
                throw new QueueException("Check Configuration - missing required subscribedTo name for consumer[" + consumerName + "] as it is marked as isSubscription=true");
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
                throw new QueueException("Check Configuration - missing required queue name for consumer: " + consumerName);
            }
        }

        String realQueueName = /*"foundation." + */queueName;

        boolean queueExists = false;

        try {
            queueExists = HornetQMessagingFactory.getSession().queueQuery(new SimpleString(realQueueName)).isExists();
        } catch (HornetQException e) {
            queueExists = false;
        }

        if (!queueExists) {
            try {
                HornetQMessagingFactory.getSession().createQueue(isSubscription ? subscribedTo : realQueueName, realQueueName, filter, isDurable);
            } catch (HornetQException e) {
                throw new QueueException("Can't create queue: " + realQueueName + ". Error: " + e, e);
            }

        }

        return realQueueName;

    }

    @Override
    public Message receive() {
        try {

            ClientMessage clientMessage = getConsumer().receive();
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

            ClientMessage clientMessage = getConsumer().receive(timeout);
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
                getConsumer().setMessageHandler((org.hornetq.api.core.client.MessageHandler) messageHandler);
            } else {
                throw new IllegalArgumentException("Using HornetQ consumer you must provide a valid HornetQ massage handler");
            }

        } catch (HornetQException e) {
            LOGGER.error("can't register a MessageHandler: {}", e, e);
            throw new QueueException(e);
        }
    }

    @Override
    public void close() {
        if (consumer != null) {
            try {
                for (ClientConsumer clientConsumer : consumers) {
                    clientConsumer.close();
                }
            } catch (HornetQException e) {
                LOGGER.error("can't close consumer, error: {}", e, e);
            }
        }
    }
}
