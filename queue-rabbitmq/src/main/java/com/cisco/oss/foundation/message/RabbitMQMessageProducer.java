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
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A RabbitMQ producer wrapper. NOTE: This class is thread safe although wraps RabbitMQ ClientProducer
 * which is not thread safe. The internal implementation is to provide a single threaded producer instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
class RabbitMQMessageProducer extends AbstractMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQMessageProducer.class);

    private Configuration configuration = ConfigurationFactory.getConfiguration();
    private String groupId = "";
    private long expiration = 1800000;


    RabbitMQMessageProducer(String producerName) {
        super(producerName);
        Configuration subset = configuration.subset(producerName);
        queueName = subset.getString("queue.name");

        if (StringUtils.isBlank(queueName)) {
            throw new QueueException("Check Configuration - missing required queue name for producer: " + producerName);
        }

        //update expiration
        expiration = subset.getLong("queue.expiration", 1800000);
        groupId = subset.getString("queue.groupId", "");

        try {
            try {
                RabbitMQMessagingFactory.INIT_LATCH.await();
            } catch (InterruptedException e) {
                LOGGER.error("error waiting for init to finish: " + e);
            }
            Channel channel = RabbitMQMessagingFactory.getChannel();
            channel.exchangeDeclare(queueName, "topic", true, false, false, null);
            LOGGER.info("created rabbitmq producer: {} on exchange: {}", producerName, queueName);
        } catch (IOException e) {
            throw new QueueException("Can't create producer: " + e, e);
        }

    }



    @Override
    public String getProducerImpl() {
        return "RabbitMQ";
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



        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder().headers(messageHeaders).deliveryMode(2).build();
        try {
            RabbitMQMessagingFactory.getChannel().basicPublish(queueName, "", basicProperties, message);
        } catch (AlreadyClosedException e) {
            LOGGER.warn("an error occurred trying to publish message: {}", e);
            RabbitMQMessagingFactory.channelThreadLocal.set(null);
            try {
                RabbitMQMessagingFactory.getChannel().basicPublish(queueName, "", basicProperties, message);
            } catch (Exception e1) {
                startReConnectThread();
                throw new QueueException("an error occurred trying to publish message: " + e1, e1);
            }
        } catch (IOException e) {
            throw new QueueException("can't send message: {}" + e, e);
        }

    }

    private void startReConnectThread() {
        RabbitMQMessagingFactory.triggerReconnectThread();
    }

    @Override
    public void sendMessage(String message, Map<String, Object> messageHeaders) {
        sendMessage(message.getBytes(), messageHeaders);
    }


    @Override
    public void close() {
    }
}
