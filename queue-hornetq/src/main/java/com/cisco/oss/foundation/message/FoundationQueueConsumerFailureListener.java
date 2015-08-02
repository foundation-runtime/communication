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
import org.apache.commons.lang3.tuple.Pair;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.*;
import org.hornetq.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Yair Ogen on 22/09/2014.
 */
public class FoundationQueueConsumerFailureListener implements SessionFailureListener {

    private ClientConsumer clientConsumer = null;
    private String queueName = null;
    private org.hornetq.api.core.client.MessageHandler messageHandler = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationQueueConsumerFailureListener.class);

    public void setReconnectProperties(String queueName, ClientConsumer clientConsumer) {
        this.clientConsumer = clientConsumer;
        this.queueName = queueName;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void connectionFailed(HornetQException exception, boolean failedOver) {
        LOGGER.error("failed connection: {}, failing over: {}", exception, failedOver);
        if (!failedOver) {

            boolean done = false;

            while (!done) {

                LOGGER.trace("attempting to reconnect to HornetQ");
                HornetQMessagingFactory.sessionThreadLocal.set(null);
                try {
//                    FoundationQueueConsumerFailureListener foundationQueueConsumerFailureListener = new FoundationQueueConsumerFailureListener();;
                    MessageHandler handler = null;
                    if (this.messageHandler == null) {
                        handler = clientConsumer.getMessageHandler();
                        this.messageHandler = handler;
                    }
                    for (Pair<ClientSession,SessionFailureListener> clientSessionSessionFailureListenerPair : HornetQMessagingFactory.getSession(FoundationQueueConsumerFailureListener.class)) {
                        FoundationQueueConsumerFailureListener foundationQueueConsumerFailureListener = (FoundationQueueConsumerFailureListener)clientSessionSessionFailureListenerPair.getRight();
                        foundationQueueConsumerFailureListener.setReconnectProperties(queueName,clientConsumer);
                        foundationQueueConsumerFailureListener.setMessageHandler(messageHandler);
                        ClientConsumer consumer = clientSessionSessionFailureListenerPair.getLeft().createConsumer(queueName);
                        consumer.setMessageHandler(messageHandler);
                    }
                    done = true;
                } catch (Exception e) {
                    LOGGER.trace("failed to reconnect. retrying...", e);
                    try {
                        Thread.sleep(ConfigurationFactory.getConfiguration().getInt("service.queue.attachRetryDelay", 10000));
                    } catch (InterruptedException e1) {
                        LOGGER.trace("thread interrupted!!!", e1);
                    }
                }
            }

        }
    }

    @Override
    public void beforeReconnect(HornetQException exception) {
        LOGGER.error("beforeReconnect: {}", exception);

    }
}
