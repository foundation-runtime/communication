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

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.google.common.collect.Lists;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the main API tp be used to instantiate new consumers and producers.
 * This class supports a Per Thread lifecycle for HornetQ session, consumers and producers
 * Created by Yair Ogen on 24/04/2014.
 */
public class HornetQMessagingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessagingFactory.class);
    private static final Set<ClientSession> sessions = new HashSet<ClientSession>();
    public static ThreadLocal<ClientSession> sessionThreadLocal = new ThreadLocal<ClientSession>();
    private static ServerLocator serverLocator = null;
    private static Map<String, MessageConsumer> consumers = new ConcurrentHashMap<String, MessageConsumer>();
    private static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();

    static {
        init();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    //allow possible background work to finish
                    Thread.sleep(1000);
                    if (!sessions.isEmpty()) {
                        for (Map.Entry<String, MessageConsumer> consumer : consumers.entrySet()) {
                            consumer.getValue().close();
                        }
                        for (Map.Entry<String, MessageProducer> producer : producers.entrySet()) {
                            producer.getValue().close();
                        }
                        for (ClientSession session : sessions) {
                            session.close();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("can't close HornetQ resources, error: {}", e, e);
                }
            }
        });
    }


    /**
     * create a new session if one doesn't already exist in the ThreadLocal
     *
     * @return a new hornetq session
     */
    public static ClientSession getSession() {
        if (sessionThreadLocal.get() == null) {
            try {
                LOGGER.info("creating a new session");

                //session is created with auto-commit and auto-ack set to true
                ClientSession hornetQSession = serverLocator.createSessionFactory().createSession(true, true);
                hornetQSession.start();
                sessionThreadLocal.set(hornetQSession);
                sessions.add(hornetQSession);
            } catch (Exception e) {
                LOGGER.error("can't create hornetq session: {}", e, e);
                throw new QueueException(e);
            }
        }
        return sessionThreadLocal.get();
    }

    /**
     * build once the hornetq service locator.
     * this is where we read the ost port list from configuration
     */
    private static void init() {
        try {
//            nettyFactory = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName())).createSessionFactory();
            Map<String, Map<String, String>> serverConnections = ConfigUtil.parseComplexArrayStructure("service.queue.connections");
            if (serverConnections != null && !serverConnections.isEmpty()) {
                ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
                Collections.sort(serverConnectionKeys);
                TransportConfiguration[] transportConfigurationsArray = new TransportConfiguration[serverConnectionKeys.size()];
                List<TransportConfiguration> transportConfigurations = new ArrayList<TransportConfiguration>();

                for (String serverConnectionKey : serverConnectionKeys) {

                    Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                    Map<String, Object> map = new HashMap<String, Object>();

                    map.put("host", serverConnection.get("host"));
                    map.put("port", serverConnection.get("port"));


                    transportConfigurations.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), map));

                }

                transportConfigurations.toArray(transportConfigurationsArray);

                serverLocator = HornetQClient.createServerLocatorWithHA(transportConfigurationsArray);
                serverLocator.setRetryInterval(1000);
                serverLocator.setRetryIntervalMultiplier(1);
                serverLocator.setReconnectAttempts(-1);
                try {
                    serverLocator.setAckBatchSize(1);
                } catch (Exception e) {
                    LOGGER.debug("error trying to set ack batch size: {}", e);
                }

            } else {
                throw new IllegalArgumentException("'service.queue.connections' must contain at least on host/port pair.");
            }

        } catch (Exception e) {
            LOGGER.error("can't create hornetq session: {}", e, e);
            throw new QueueException(e);
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
            consumers.put(consumerName, new HornetQMessageConsumer(consumerName));
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
            producers.put(producerName, new HornetQMessageProducer(producerName));
        }
        return producers.get(producerName);
    }


}
