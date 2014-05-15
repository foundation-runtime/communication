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

package com.cisco.vss.foundation.message;

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.cisco.vss.foundation.queue.QueueException;
import com.google.common.collect.Lists;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Yair Ogen on 24/04/2014.
 */
public class HornetQMessagingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessagingFactory.class);
    private static final Set<ClientSession> sessions = new HashSet<ClientSession>();
    public static ThreadLocal<ClientSession> sessionThreadLocal = new ThreadLocal<ClientSession>();
    //    private static ClientSessionFactory nettyFactory = null;
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

    public static ClientSession getSession() {
        if (sessionThreadLocal.get() == null) {
            try {
                LOGGER.info("creating session");
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

            } else {
                throw new IllegalArgumentException("'service.queue.connections' must contain at least on host/port pair.");
            }

        } catch (Exception e) {
            LOGGER.error("can't create hornetq session: {}", e, e);
            throw new QueueException(e);
        }

    }

    public static MessageConsumer createConsumer(String consumerName) {
        if (!consumers.containsKey(consumerName)) {
            consumers.put(consumerName, new HornetQMessageConsumer(consumerName));
        }
        return consumers.get(consumerName);
    }


    public static MessageProducer createProducer(String producerName) {
        if (!producers.containsKey(producerName)) {
            producers.put(producerName, new HornetQMessageProducer(producerName));
        }
        return producers.get(producerName);
    }


}
