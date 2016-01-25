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
import com.cisco.oss.foundation.configuration.FoundationConfigurationListener;
import com.cisco.oss.foundation.configuration.FoundationConfigurationListenerRegistry;
import com.google.common.collect.Lists;
import com.rabbitmq.client.*;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the main API tp be used to instantiate new consumers and producers.
 * This class supports a Per Thread lifecycle for RabbitMQ session, consumers and producers
 * Created by Yair Ogen on 24/04/2014.
 */
public class RabbitMQMessagingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQMessagingFactory.class);
    private static Map<String, MessageConsumer> consumers = new ConcurrentHashMap<String, MessageConsumer>();
    private static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();
    public static ThreadLocal<Channel> channelThreadLocal = new ThreadLocal<>();
    //    private static List<Channel> channels = new CopyOnWriteArrayList<>();
    private static PriorityBlockingQueue<AckNackMessage> messageAckQueue = new PriorityBlockingQueue<AckNackMessage>(10000);
    private static Map<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();
    private static Connection connection = null;
    private static AtomicBoolean IS_RECONNECT_THREAD_RUNNING = new AtomicBoolean(false);
    static AtomicBoolean IS_CONNECTED = new AtomicBoolean(false);
    static CountDownLatch INIT_LATCH = new CountDownLatch(1);
    static AtomicBoolean IS_BLOCKED = new AtomicBoolean(false);


    static {

        FoundationConfigurationListenerRegistry.addFoundationConfigurationListener(new FoundationConfigurationListener() {
            @Override
            public void configurationChanged() {
                connect();
            }
        });

        init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    //allow possible background work to finish
                    Thread.sleep(1000);

                    for (Channel channel : channels.values()) {
                        channel.close();
                    }

                    connection.close();

                } catch (Exception e) {
                    LOGGER.error("can't close RabbitMQ resources, error: {}", e, e);
                }
            }
        });

        Thread rabbitAckThread = new Thread("rabbitAckThread") {
            @Override
            public void run() {

                while (true) {
                    try {

                        AckNackMessage message = messageAckQueue.take();

                        Channel channel = channels.get(message.channelNumber);
                        if (channel != null && channel.isOpen()) {
                            if (message.ack)
                                channel.basicAck(message.deliveryTag, false);
                            else
                                channel.basicNack(message.deliveryTag, false, false);
                        }

                    } catch (Exception e) {
                        LOGGER.error(e.toString(), e);
                    }
                }
            }
        };
        rabbitAckThread.setDaemon(true);
        rabbitAckThread.start();
    }


    /**
     * build once the RabbitMQ service locator.
     * this is where we read the ost port list from configuration
     */
    private static void init() {
        try {
            connect();
        } catch (Exception e) {
            LOGGER.warn("Initial connect has failed. Attempting reconnect in another thread.");
            triggerReconnectThread();
        }
    }

    static void connect() {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setAutomaticRecoveryEnabled(true);
            connectionFactory.setTopologyRecoveryEnabled(true);

            Configuration configuration = ConfigurationFactory.getConfiguration();
            Configuration subsetBase = configuration.subset("service.queue");
            Configuration subsetSecurity = subsetBase.subset("security");

            String userName = subsetSecurity.getString("userName");
            String password = subsetSecurity.getString("password");
            boolean isEnabled = subsetSecurity.getBoolean("isEnabled");

            final Map<String, Map<String, String>> serverConnections = ConfigUtil.parseComplexArrayStructure("service.queue.connections");
            final ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
            Collections.sort(serverConnectionKeys);

            if (isEnabled) {
                connectionFactory.setUsername(userName);
                connectionFactory.setPassword(password);

            }

            List<Address> addresses = new ArrayList<>(5);

            for (String serverConnectionKey : serverConnectionKeys) {

                Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                String host = serverConnection.get("host");
                int port = Integer.parseInt(serverConnection.get("port"));
                addresses.add(new Address(host, port));
//              connectionFactory.setHost(host);
//              connectionFactory.setPort(Integer.parseInt(port));
            }
            Address[] addrs = new Address[0];
            connection = connectionFactory.newConnection(addresses.toArray(addrs));
            connection.addBlockedListener(new BlockedListener() {
                public void handleBlocked(String reason) throws IOException {
                    LOGGER.error("RabbitMQ connection is now blocked. Port: {}, Reason: {}", connection.getPort(), reason);
                    IS_BLOCKED.set(true);
                }

                public void handleUnblocked() throws IOException {
                    LOGGER.info("RabbitMQ connection is now un-blocked. Port: {}", connection.getPort());
                    IS_BLOCKED.set(false);
                }
            });
            IS_CONNECTED.set(true);
            INIT_LATCH.countDown();

        } catch (Exception e) {
            LOGGER.error("can't create RabbitMQ Connection: {}", e, e);
//            triggerReconnectThread();
            throw new QueueException(e);
        }
    }

    static Channel getChannel() {
        try {
            if (channelThreadLocal.get() == null) {
                if (connection != null) {
                    Channel channel = connection.createChannel();
                    channelThreadLocal.set(channel);
                    channels.put(channel.getChannelNumber(), channel);
                } else {
                    throw new QueueException("RabbitMQ appears to be down. Please try again later.");
                }
            }

            return channelThreadLocal.get();
        } catch (IOException e) {
            throw new QueueException("can't create channel: " + e.toString(), e);
        }

    }

    public static void ackMessage(Integer channelNumber, Long deliveryTag) {
        messageAckQueue.add(new AckNackMessage(channelNumber, deliveryTag, true));
    }

    public static void nackMessage(Integer channelNumber, Long deliveryTag) {
        messageAckQueue.add(new AckNackMessage(channelNumber, deliveryTag, false));
    }

    static class AckNackMessage implements Comparable<AckNackMessage> {
        private final Integer channelNumber;
        private final Long deliveryTag;
        private final boolean ack;

        public AckNackMessage(Integer channelNumber, Long deliveryTag, boolean ack) {
            this.channelNumber = channelNumber;
            this.deliveryTag = deliveryTag;
            this.ack = ack;
        }

        @Override
        public int compareTo(AckNackMessage o) {
            if (o == null)
                return 1;

            return this.channelNumber.compareTo(o.channelNumber);

        }
    }


    /**
     * create a new consumer if one doesn't already exist in the ThreadLocal
     * the consumer will bonded to an address with a queue-name as defined in the configuration.
     * the configuration subset is defined by finding a subset starting with the given consumer name.
     * E.g. consumer name = consumer1
     * Config:
     * consumer1.queue.name=consumer1
     * consumer1.queue.filter=key1='value2'
     * consumer1.queue.isSubscription=true
     * consumer1.queue.subscribedTo=myExample
     */
    public static MessageConsumer createConsumer(String consumerName) {
        if (!consumers.containsKey(consumerName)) {
            consumers.put(consumerName, new RabbitMQMessageConsumer(consumerName));
        }
        return consumers.get(consumerName);
    }


    /**
     * create a new producer if one doesn't already exist in the ThreadLocal
     * the producer will be bonded to an address with an address-name as defined in the configuration.
     * the configuration subset is defined by finding a subset starting with the given producer name.
     * E.g. producer name = example
     * Config:
     * example.queue.name=myExample
     */
    public static MessageProducer createProducer(String producerName) {
        if (!producers.containsKey(producerName)) {
            producers.put(producerName, new RabbitMQMessageProducer(producerName));
        }
        return producers.get(producerName);
    }

    static void triggerReconnectThread() {
        if (IS_RECONNECT_THREAD_RUNNING.compareAndSet(false, true)) {
            Thread reconnectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!IS_CONNECTED.get()) {
                        try {
                            connect();
                        } catch (Exception e) {
                            LOGGER.trace("reconnect failed: " + e);
                            try {
                                Thread.sleep(ConfigurationFactory.getConfiguration().getInt("service.queue.attachRetryDelay", 10000));
                            } catch (InterruptedException e1) {
                                LOGGER.trace("thread interrupted!!!", e1);
                            }
                        }
                    }
                    IS_RECONNECT_THREAD_RUNNING.set(false);

                }
            }, "RabbitMQ-Reconnect");

            reconnectThread.setDaemon(false);
            reconnectThread.start();

        }
    }

    public static boolean deleteQueue(String queueName){
        try {
            getChannel().queueDelete(queueName);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete queue: {}", e);
            return false;
        }
    }

    public static boolean deleteQueue(String queueName, boolean deleteOnlyIfNotUsed, boolean deeltenlyIfNotEmpty){
        try {
            getChannel().queueDelete(queueName, deleteOnlyIfNotUsed, deeltenlyIfNotEmpty);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete queue: {}", e);
            return false;
        }
    }

    public static boolean deleteExchange(String exchangeName){
        try {
            getChannel().exchangeDelete(exchangeName);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete exchange: {}", e);
            return false;
        }
    }


    public static boolean deleteExchange(String exchangeName, boolean deleteOnlyIfNotUsed){
        try {
            getChannel().exchangeDelete(exchangeName, deleteOnlyIfNotUsed);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete exchange: {}", e);
            return false;
        }
    }


}
