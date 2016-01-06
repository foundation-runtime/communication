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

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A RabbitMQ consumerThreadLocal wrapper. NOTE: This class is thread safe although wraps RabbitMQ ClientConsumer
 * which is not thread safe. The internal implementation is to provide a single threaded consumerThreadLocal instance by using ThreadLocal
 * so this class can be used in a multi-threaded environment.
 * Created by Yair Ogen on 24/04/2014.
 */
public class RabbitMQMessageConsumer implements MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQMessageConsumer.class);
    public static final String DLQ = "DLQ";
    private String consumerName = "N/A";
    private Configuration configuration = ConfigurationFactory.getConfiguration();

    private String queueName = "";
    private QueueingConsumer consumer = null;

    private AtomicInteger nextIndex = new AtomicInteger(0);

    RabbitMQMessageConsumer(String consumerName) {
        try {
            this.consumerName = consumerName;
            Configuration subset = configuration.subset(consumerName);
            queueName = subset.getString("queue.name");

            String filter = subset.getString("queue.filter", "");
            boolean isDurable = subset.getBoolean("queue.isDurable", true);
            boolean isSubscription = subset.getBoolean("queue.isSubscription", false);
            long expiration = subset.getLong("queue.expiration", 1800000);
            long maxLength = subset.getLong("queue.maxLength", -1);
            boolean deadLetterIsEnabled = subset.getBoolean("queue.deadLetterIsEnabled", true);
            String subscribedTo = isSubscription ? subset.getString("queue.subscribedTo", "") : queueName;
            String exchangeType = isSubscription ? "topic" : "direct";
            try {
                RabbitMQMessagingFactory.INIT_LATCH.await();
            } catch (InterruptedException e) {
                LOGGER.error("error waiting for init to finish: " + e);
            }
            Channel channel = RabbitMQMessagingFactory.getChannel();
            channel.exchangeDeclare(subscribedTo, exchangeType, isDurable, false, false, null);

            Map<String, Object> args = new HashMap<String, Object>();

            if (maxLength > 0) {
                args.put("x-max-length", maxLength);
            }

            if (expiration > 0) {
                args.put("x-message-ttl", expiration);
            }


            if (deadLetterIsEnabled) {
                channel.exchangeDeclare(DLQ, exchangeType, true, false, false, null);
                args.put("x-dead-letter-exchange", DLQ);
            }

            String queue = channel.queueDeclare(queueName, isDurable, false, false, args).getQueue();

            Map<String, String> filters = ConfigUtil.parseSimpleArrayAsMap(consumerName + ".queue.filters");
            if (filters != null && !filters.isEmpty()) {
                for (String routingKey : filters.values()) {
                    channel.queueBind(queue, subscribedTo, routingKey);
                }
            } else {
                channel.queueBind(queue, subscribedTo, "#");
            }

            consumer = new QueueingConsumer(channel);
//            channel.basicConsume(queueName, true, consumer);
            LOGGER.info("created rabbitmq consumer: {} on exchange: {}, queue-name: {}", consumerName, subscribedTo, queueName);
        } catch (IOException e) {
            throw new QueueException("Can't create consumer: " + e, e);
        }

    }

    @Override
    public Message receive() {

        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            GetResponse getResponse = new GetResponse(delivery.getEnvelope(), delivery.getProperties(), delivery.getBody(), 0);
            RabbitMQMessage rabbitMQMessage = new RabbitMQMessage(getResponse, "");
            return rabbitMQMessage;
        } catch (InterruptedException e) {
            throw new QueueException("can't get new message: " + e, e);
        }

    }

    @Override
    public Message receive(long timeout) {
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery(timeout);
            GetResponse getResponse = new GetResponse(delivery.getEnvelope(), delivery.getProperties(), delivery.getBody(), 0);
            RabbitMQMessage rabbitMQMessage = new RabbitMQMessage(getResponse, "");
            return rabbitMQMessage;
        } catch (InterruptedException e) {
            throw new QueueException("can't get new message: " + e, e);
        }
    }

    public void registerMessageHandler(MessageHandler messageHandler, boolean autoAck) {
        try {
            if (messageHandler instanceof AbstractRabbitMQMessageHandler) {
                String consumerTag = FlowContextFactory.getFlowContext() != null ? FlowContextFactory.getFlowContext().getUniqueId() : "N/A";
                Channel channel = RabbitMQMessagingFactory.getChannel();
                ((AbstractRabbitMQMessageHandler) messageHandler).setChannelNumber(channel.getChannelNumber());
                channel.basicConsume(queueName, autoAck, consumerTag, (Consumer) messageHandler);
            } else {
                throw new IllegalArgumentException("Using RabbitMQ consumerThreadLocal you must provide a valid RabbitMQ massage handler");
            }

        } catch (IOException e) {
//            LOGGER.error("can't register a MessageHandler: {}", e);
            throw new QueueException("can't register a MessageHandler: " + e, e);
        }
    }

    public void ackMessage(Integer channelNumber, Long deliveryTag) {
        RabbitMQMessagingFactory.ackMessage(channelNumber, deliveryTag);
    }

    public void nackMessage(Integer channelNumber, Long deliveryTag) {
        RabbitMQMessagingFactory.nackMessage(channelNumber, deliveryTag);
    }

    @Override
    public void registerMessageHandler(MessageHandler messageHandler) {
        registerMessageHandler(messageHandler, true);
    }

    @Override
    public void close() {
    }

    public String getQueueName() {
        return queueName;
    }
}
