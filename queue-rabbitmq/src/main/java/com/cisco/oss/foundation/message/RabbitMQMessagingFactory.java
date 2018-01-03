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
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.StandardMetricsCollector;
import com.rabbitmq.client.impl.nio.NioParams;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.event.ChannelListener;
import net.jodah.lyra.event.ConnectionListener;
import net.jodah.lyra.event.ConsumerListener;
import net.jodah.lyra.util.Duration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


/* Since channel is kept per Thread and we don't have control on its life cycle (its the caller Thread),
   we want to make sure it released properly when the Thread is released so we won't have zombie channels in the system */
class ChannelWrapper {
    private Channel channel;

    ChannelWrapper(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    protected void finalize() throws Throwable
    {
        boolean closeChannelEnabled = ConfigurationFactory.getConfiguration().
                getBoolean("service.rabbitmq.closeChannelOnThreadRelease.isEnabled", false);

        if (closeChannelEnabled){
            RabbitMQMessagingFactory.closeChannelAndRemoveFromMap(channel, "thread finalize");
        }
    }
}


/**
 * This is the main API tp be used to instantiate new consumers and producers.
 * This class supports a Per Thread lifecycle for RabbitMQ session, consumers and producers
 * Created by Yair Ogen on 24/04/2014.
 */
public class RabbitMQMessagingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQMessagingFactory.class);
    static Map<String, MessageConsumer> consumers = new ConcurrentHashMap<String, MessageConsumer>();
    static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();
    public static ThreadLocal<ChannelWrapper> channelThreadLocal = new ThreadLocal<>();
    //    private static List<Channel> channels = new CopyOnWriteArrayList<>();
    private static PriorityBlockingQueue<AckNackMessage> messageAckQueue = new PriorityBlockingQueue<AckNackMessage>(10000);
    static Map<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();
    private static Connection connection = null;
    private static AtomicBoolean IS_RECONNECT_THREAD_RUNNING = new AtomicBoolean(false);
    static AtomicBoolean IS_CONNECTED = new AtomicBoolean(false);
    static CountDownLatch INIT_LATCH = new CountDownLatch(1);
    static AtomicBoolean IS_BLOCKED = new AtomicBoolean(false);
    static AtomicBoolean IS_CONNECCTION_OR_CHANNEL_UP = new AtomicBoolean(false);
    static AtomicBoolean IS_CONSUMER_UP = new AtomicBoolean(false);


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
                        closeChannelAndRemoveFromMap(channel, "shutdown hook");
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
                                channel.basicNack(message.deliveryTag, false, message.requeue);
                        }


                    } catch (Exception e) {
                        LOGGER.error("Problem in ACK Thread: {}", e.toString(), e);
                    }
                }
            }
        };
        rabbitAckThread.setDaemon(true);
        rabbitAckThread.start();
    }

    protected static void closeChannelAndRemoveFromMap(Channel channel, String reason) {

        // Note: once channel was closed, any action on the object will result with AlreadyClosedException :(
        try {
            if (channel != null) {
                int channelNumber = channel.getChannelNumber();
                LOGGER.info("closeChannel " + channelNumber + " reason: " + reason);
                channel.close(200, reason);
                channels.remove(channelNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (AlreadyClosedException e){
            e.printStackTrace();
        }
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

            Configuration configuration = ConfigurationFactory.getConfiguration();

            final Map<String, Map<String, String>> serverConnections = ConfigUtil.parseComplexArrayStructure("service.rabbitmq.connections");
            final ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
            Collections.sort(serverConnectionKeys);

            int maxRetryAttempts = configuration.getInt("service.rabbitmq.maxRetryAttempts", 1000);

            Config config = new Config()
                    .withRecoveryPolicy(new RecoveryPolicy()
                            .withBackoff(Duration.seconds(1), Duration.seconds(30))
                            .withMaxAttempts(maxRetryAttempts))
                    .withConnectionRecoveryPolicy(new RecoveryPolicy().withBackoff(Duration.seconds(1), Duration.seconds(30))
                            .withMaxAttempts(maxRetryAttempts))
                    .withConsumerRecovery(true)
                    .withExchangeRecovery(true)
                    .withQueueRecovery(true)
                    .withConnectionListeners(new ConnectionListener() {
                        @Override
                        public void onCreate(Connection connection) {
                            LOGGER.trace("connection create: {}", connection);
                        }

                        @Override
                        public void onCreateFailure(Throwable failure) {
                            LOGGER.error("connection create failed: {}", failure.toString(), failure);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);
                        }

                        @Override
                        public void onRecoveryStarted(Connection connection) {
                            LOGGER.trace("connection recovery started: {}", connection);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);

                        }

                        @Override
                        public void onRecovery(Connection connection) {
                            LOGGER.trace("connection recovered: {}", connection);
                        }

                        @Override
                        public void onRecoveryCompleted(Connection connection) {
                            LOGGER.trace("connection recovery completed: {}", connection);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(true);
                        }

                        @Override
                        public void onRecoveryFailure(Connection connection, Throwable failure) {
                            LOGGER.error("connection recovery failed: {}", failure.toString(), failure);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);
                        }
                    })
                    .withChannelListeners(new ChannelListener() {
                        @Override
                        public void onCreate(Channel channel) {
                            LOGGER.trace("channel create: {}", channel);
                        }

                        @Override
                        public void onCreateFailure(Throwable failure) {
                            LOGGER.error("channel create failed: {}", failure.toString(), failure);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);
                        }

                        @Override
                        public void onRecoveryStarted(Channel channel) {
                            LOGGER.trace("channel recovery started: {}", channel);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);
                        }

                        @Override
                        public void onRecovery(Channel channel) {
                            LOGGER.trace("channel recovered: {}", channel);
                        }

                        @Override
                        public void onRecoveryCompleted(Channel channel) {
                            LOGGER.trace("channel recovery completed: {}", channel);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(true);
                        }

                        @Override
                        public void onRecoveryFailure(Channel channel, Throwable failure) {
                            LOGGER.error("channel recovery failed: {}", failure.toString(), failure);
                            IS_CONNECCTION_OR_CHANNEL_UP.set(false);
                        }
                    })
                    .withConsumerListeners(new ConsumerListener() {
                        @Override
                        public void onRecoveryStarted(Consumer consumer, Channel channel) {
                            LOGGER.trace("consumer create. consumer: {}, channel: {}", consumer, channel);
                            IS_CONSUMER_UP.set(false);
                        }

                        @Override
                        public void onRecoveryCompleted(Consumer consumer, Channel channel) {
                            LOGGER.trace("consumer recovery completed: {}, channel: {}", consumer, channel);
                            IS_CONSUMER_UP.set(true);
                        }

                        @Override
                        public void onRecoveryFailure(Consumer consumer, Channel channel, Throwable failure) {
                            LOGGER.error("consumer recovery failed. consumer: {}, channel: {}, error: {}", consumer, channel, failure.toString(), failure);
                            IS_CONSUMER_UP.set(false);
                        }
                    });


            config.getRecoverableExceptions().add(UnknownHostException.class);
            config.getRecoverableExceptions().add(NoRouteToHostException.class);

            List<Address> addresses = new ArrayList<>(5);

            for (String serverConnectionKey : serverConnectionKeys) {

                Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                String host = serverConnection.get("host");
                int port = Integer.parseInt(serverConnection.get("port"));
                addresses.add(new Address(host, port));
            }
            Address[] addrs = new Address[0];

            ConnectionOptions options = new ConnectionOptions()
                    .withAddresses(addresses.toArray(addrs));

            final ConnectionFactory connectionFactory = options.getConnectionFactory();
            connectionFactory.setAutomaticRecoveryEnabled(false);
            connectionFactory.setTopologyRecoveryEnabled(false);

            final boolean metricsAndMonitoringIsEnabled = configuration.getBoolean("service.rabbitmq.metricsAndMonitoringJmx.isEnabled", false);

            if (metricsAndMonitoringIsEnabled){
                MetricRegistry registry = new MetricRegistry();
                StandardMetricsCollector metrics = new StandardMetricsCollector(registry);
                connectionFactory.setMetricsCollector(metrics);

                JmxReporter reporter = JmxReporter
                        .forRegistry(registry)
                        .inDomain("com.rabbitmq.client.jmx")
                        .build();
                reporter.start();
            }

            final boolean useNio = configuration.getBoolean("service.rabbitmq.useNio", false);

            if (useNio) {
                NioParams nioParams = new NioParams();

                final Integer nbIoThreads = configuration.getInteger("service.rabbitmq.nio.nbIoThreads", null);
                final Integer readByteBufferSize = configuration.getInteger("service.rabbitmq.nio.readByteBufferSize", null);
                final Integer writeByteBufferSize = configuration.getInteger("service.rabbitmq.nio.writeByteBufferSize", null);
                final Integer writeEnqueuingTimeoutInMs = configuration.getInteger("service.rabbitmq.nio.writeEnqueuingTimeoutInMs", null);
                final Integer writeQueueCapacity = configuration.getInteger("service.rabbitmq.nio.writeQueueCapacity", null);

                if (nbIoThreads != null) {
                    nioParams.setNbIoThreads(nbIoThreads);
                }
                if (readByteBufferSize != null) {
                    nioParams.setReadByteBufferSize(readByteBufferSize);
                }
                if (writeByteBufferSize != null) {
                    nioParams.setWriteByteBufferSize(writeByteBufferSize);
                }
                if (writeEnqueuingTimeoutInMs != null) {
                    nioParams.setWriteEnqueuingTimeoutInMs(writeEnqueuingTimeoutInMs);
                }
                if (writeQueueCapacity != null) {
                    nioParams.setWriteQueueCapacity(writeQueueCapacity);
                }

//                nioParams.setNioExecutor()
//                nioParams.setThreadFactory()

                options.withNio().withNioParams(nioParams);
            }

            Configuration subsetBase = configuration.subset("service.rabbitmq");
            Configuration subsetSecurity = subsetBase.subset("security");

            int requestHeartbeat = subsetBase.getInt("requestHeartbeat", 10);
            options.withRequestedHeartbeat(Duration.seconds(requestHeartbeat));

            String userName = subsetSecurity.getString("userName");
            String password = subsetSecurity.getString("password");
            boolean isEnabled = subsetSecurity.getBoolean("isEnabled");


            if (isEnabled) {
                options
                        .withUsername(userName)
                        .withPassword(password);

            }

            connection = Connections.create(options, config);


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

            connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    LOGGER.error("Connection shutdown detected. Reason: {}", cause.toString(), cause);
                    IS_CONNECTED.set(false);
                }
            });

            IS_CONNECTED.set(true);
            IS_CONNECCTION_OR_CHANNEL_UP.set(true);
            IS_CONSUMER_UP.set(true);
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
                if (connection != null && connection.isOpen()) {
                    Channel channel = connection.createChannel();
                    channelThreadLocal.set(new ChannelWrapper(channel));
                    channels.put(channel.getChannelNumber(), channel);
                } else {
                    throw new QueueException("RabbitMQ appears to be down. Please try again later.");
                }
            }
            return channelThreadLocal.get().getChannel();
        } catch (IOException e) {
            throw new QueueException("can't create channel: " + e.toString(), e);
        }

    }

    static Channel createChannelWithoutTL() {
        try {
            if (connection != null && connection.isOpen()) {
                Channel channel = connection.createChannel();
                channels.put(channel.getChannelNumber(), channel);
                return channel;
            } else {
                throw new QueueException("RabbitMQ appears to be down. Please try again later.");
            }
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

    public static void nackMessage(Integer channelNumber, Long deliveryTag, boolean requeue) {
        messageAckQueue.add(new AckNackMessage(channelNumber, deliveryTag, false, requeue));
    }

    static class AckNackMessage implements Comparable<AckNackMessage> {
        private final Integer channelNumber;
        private final Long deliveryTag;
        private final boolean ack;
        private final boolean requeue;

        public AckNackMessage(Integer channelNumber, Long deliveryTag, boolean ack, boolean requeue) {
            this.channelNumber = channelNumber;
            this.deliveryTag = deliveryTag;
            this.ack = ack;
            this.requeue = requeue;
        }

        public AckNackMessage(Integer channelNumber, Long deliveryTag, boolean ack) {
            this.channelNumber = channelNumber;
            this.deliveryTag = deliveryTag;
            this.ack = ack;
            this.requeue = false;
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
            boolean withoutThreadLocal = ConfigurationFactory.getConfiguration().
                    getBoolean("service.rabbitmq.setConsumerWithoutThreadLocal.isEnabled", false);
            MessageConsumer consumer;
            if (withoutThreadLocal){
                consumer = new RabbitMQMessageConsumerWithoutTL(consumerName);
            }else{
                consumer = new RabbitMQMessageConsumer(consumerName);
            }
            consumers.put(consumerName, consumer);
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
                                Thread.sleep(ConfigurationFactory.getConfiguration().getInt("service.rabbitmq.attachRetryDelay", 10000));
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

    public static boolean deleteQueue(String queueName) {
        try {
            getChannel().queueDelete(queueName);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete queue: {}", e);
            return false;
        }
    }

    public static boolean deleteQueue(String queueName, boolean deleteOnlyIfNotUsed, boolean deeltenlyIfNotEmpty) {
        try {
            getChannel().queueDelete(queueName, deleteOnlyIfNotUsed, deeltenlyIfNotEmpty);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete queue: {}", e);
            return false;
        }
    }

    public static boolean deleteExchange(String exchangeName) {
        try {
            getChannel().exchangeDelete(exchangeName);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete exchange: {}", e);
            return false;
        }
    }


    public static boolean deleteExchange(String exchangeName, boolean deleteOnlyIfNotUsed) {
        try {
            getChannel().exchangeDelete(exchangeName, deleteOnlyIfNotUsed);
            return true;
        } catch (IOException e) {
            LOGGER.warn("can't delete exchange: {}", e);
            return false;
        }
    }

    public static Boolean isConnectionOrChannelUp() {
        return IS_CONNECCTION_OR_CHANNEL_UP.get();
    }

    public static Boolean isConsumerUp() {
        return IS_CONSUMER_UP.get();
    }


}
