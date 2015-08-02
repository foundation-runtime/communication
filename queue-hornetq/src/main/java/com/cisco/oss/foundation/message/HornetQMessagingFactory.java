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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.*;
import org.hornetq.api.core.management.ObjectNameBuilder;
import org.hornetq.api.jms.management.JMSServerControl;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.utils.VersionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * This is the main API tp be used to instantiate new consumers and producers.
 * This class supports a Per Thread lifecycle for HornetQ session, consumers and producers
 * Created by Yair Ogen on 24/04/2014.
 */
public class HornetQMessagingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HornetQMessagingFactory.class);
    private static final List<ClientSession> sessions = new CopyOnWriteArrayList<>();
    public static ThreadLocal<List<Pair<ClientSession, SessionFailureListener>>> sessionThreadLocal = new ThreadLocal<List<Pair<ClientSession, SessionFailureListener>>>();
    public static CountDownLatch INIT_READY = new CountDownLatch(1);

    private static List<ServerLocator> serverLocators = new CopyOnWriteArrayList<>();
    private static List<ClientSession> clientSessions = new CopyOnWriteArrayList<>();
    private static Map<String, MessageConsumer> consumers = new ConcurrentHashMap<String, MessageConsumer>();
    private static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();
    private static TransportConfiguration[] transportConfigurationsArray = null;


    static {

        FoundationConfigurationListenerRegistry.addFoundationConfigurationListener(new FoundationConfigurationListener() {
            @Override
            public void configurationChanged() {
                HornetQMessageConsumer.consumerThreadLocal.remove();
                sessionThreadLocal.remove();
                init();
            }
        });

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

                        for (ClientSession session : clientSessions) {
                            session.close();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("can't close HornetQ resources, error: {}", e, e);
                }
            }
        });
    }

    public static List<Pair<ClientSession, SessionFailureListener>> getSession(Class<? extends SessionFailureListener> sessionFailureListener) {


        if (sessionThreadLocal.get() == null) {
            try {
                LOGGER.debug("creating a new session");
                List<Pair<ClientSession, SessionFailureListener>> hornetQSessions = new ArrayList<>();
                for (ServerLocator serverLocator : serverLocators) {
                    ClientSession hornetQSession = serverLocator.createSessionFactory().createSession(true, true);
                    SessionFailureListener listener = null;
                    if (sessionFailureListener != null) {
                        listener = sessionFailureListener.newInstance();
                        hornetQSession.addFailureListener(listener);
                    }
                    hornetQSession.start();
                    sessions.add(hornetQSession);
                    hornetQSessions.add(Pair.of(hornetQSession, listener));
                }
                sessionThreadLocal.set(hornetQSessions);

            } catch (Exception e) {
                LOGGER.error("can't create hornetq session: {}", e, e);
                throw new QueueException(e);
            }
        }
        return sessionThreadLocal.get();


    }


    /**
     * create a new session if one doesn't already exist in the ThreadLocal
     *
     * @return a new hornetq session
     */
    public static List<Pair<ClientSession, SessionFailureListener>> getSession() {
        return getSession(null);
    }

//    private static boolean sessionIsClosed(ClientSession clientSession) {
//        if(clientSession != null){
//            if(clientSession instanceof DelegatingSession){
//                DelegatingSession delegatingSession = (DelegatingSession)clientSession;
//                try {
//                    Field closed = delegatingSession.getClass().getDeclaredField("closed");
//                    closed.setAccessible(true);
//                    return (boolean)closed.get(delegatingSession);
//                } catch (Exception e) {
//                    LOGGER.trace("unable to get closed state from session: {}", e, e);
//                }
//            }else{
//                return clientSession.isClosed();
//            }
//        }
//        return true;
//    }

    /**
     * build once the hornetq service locator.
     * this is where we read the ost port list from configuration
     */
    private static void init() {
        try {

            cleanup();

//            nettyFactory = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName())).createSessionFactory();
            Configuration configuration = ConfigurationFactory.getConfiguration();
            Configuration subset = configuration.subset("service.queue.connections");

            final Map<String, Map<String, String>> serverConnections = ConfigUtil.parseComplexArrayStructure("service.queue.connections");
            boolean isVersionPrinted = false;
            if (serverConnections != null && !serverConnections.isEmpty()) {
                if (isActiveActiveMode(subset)) {
                    final ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
                    Collections.sort(serverConnectionKeys);
                    for (String serverConnectionKey : serverConnectionKeys) {
                        String host1Param = "service.queue.connections." + serverConnectionKey + ".instance1.host";
                        String port1Param = "service.queue.connections." + serverConnectionKey + ".instance1.port";
                        String jmxPort1Param = "service.queue.connections." + serverConnectionKey + ".instance1.jmxPort";
                        String host2Param = "service.queue.connections." + serverConnectionKey + ".instance2.host";
                        String port2Param = "service.queue.connections." + serverConnectionKey + ".instance2.port";
                        String host1 = configuration.getString(host1Param, null);
                        String port1 = configuration.getString(port1Param, null);
                        String host2 = configuration.getString(host2Param, null);
                        String port2 = configuration.getString(port2Param, null);

                        if (StringUtils.isAnyBlank(host1Param, host2Param, port1Param, port2Param)) {
                            throw new IllegalArgumentException("each HornetQ active active pair must contain all these suffixed {instance1.host, instance1.port, instance2.host, instance2.port} - but some are missing");
                        }


                        if (!isVersionPrinted) {
                            printHQVersion(host1, configuration.getString(jmxPort1Param, "3900"));
                        }
                        transportConfigurationsArray = new TransportConfiguration[2];
                        List<TransportConfiguration> transportConfigurations = new ArrayList<TransportConfiguration>();

                        Map<String, Object> map1 = new HashMap<String, Object>();
                        map1.put("host", host1);
                        map1.put("port", port1);

                        transportConfigurations.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), map1));

                        Map<String, Object> map2 = new HashMap<String, Object>();
                        map2.put("host", host2);
                        map2.put("port", port2);

                        transportConfigurations.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), map2));


                        transportConfigurations.toArray(transportConfigurationsArray);
                        connect();
                    }

                } else {

                    final ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
                    Collections.sort(serverConnectionKeys);

                    printHQVersion(serverConnections, serverConnectionKeys);

                    transportConfigurationsArray = new TransportConfiguration[serverConnectionKeys.size()];
                    List<TransportConfiguration> transportConfigurations = new ArrayList<TransportConfiguration>();


                    for (String serverConnectionKey : serverConnectionKeys) {

                        Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                        Map<String, Object> map = new HashMap<String, Object>();

                        map.put("host", serverConnection.get("host"));
                        map.put("port", serverConnection.get("port"));


                        transportConfigurations.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), map));

                    }

                    transportConfigurations.toArray(transportConfigurationsArray);
                    connect();
                }

            } else {
                throw new IllegalArgumentException("'service.queue.connections' must contain at least on host/port pair.");
            }

            setupConsumers();

        } catch (Exception e) {
            LOGGER.error("can't create hornetq service locator: {}", e, e);
            throw new QueueException(e);
        }

    }

    private static void setupConsumers() {
        for (Map.Entry<String, MessageHandler> consumers : HornetQMessageConsumer.consumerInfo.entrySet()) {
            String consumerName = consumers.getKey();
            MessageHandler messageHandler = consumers.getValue();
            MessageConsumer consumer = HornetQMessagingFactory.createConsumer(consumerName);
            consumer.registerMessageHandler(messageHandler);
        }
    }

    private static void cleanup() {
        if(serverLocators != null){
            for (ServerLocator serverLocator : serverLocators) {
                try {
                    serverLocator.close();
                } catch (Exception e) {
                    LOGGER.debug("can't close server locator: " + e.toString());
                }
            }
            serverLocators.clear();
        }

        if (sessions != null){
            for (ClientSession session : sessions) {
                try {
                    session.close();
                } catch (HornetQException e) {
                    LOGGER.debug("can't close session: " + e.toString());
                }
            }
            sessions.clear();
        }

        if (clientSessions != null){
            for (ClientSession session : clientSessions) {
                try {
                    session.close();
                } catch (HornetQException e) {
                    LOGGER.debug("can't close session: " + e.toString());
                }
            }
            clientSessions.clear();
        }

        if(HornetQMessageConsumer.consumers != null && !HornetQMessageConsumer.consumers.isEmpty()){
            for (ClientConsumer consumer : HornetQMessageConsumer.consumers) {
                try {
                    consumer.close();
                } catch (HornetQException e) {
                    LOGGER.debug("can't close consumer: " + e.toString());
                }
            }
        }

        consumers.clear();
        producers.clear();

//        HornetQMessageConsumer.consumerInfo.clear();
    }

    private static void printHQVersion(final String host, final String port) {
        LOGGER.info("HornetQ version: {}", VersionLoader.getVersion().getVersionName());

        Runnable getVersionFromServer = new Runnable() {
            @Override
            public void run() {
                try {

                    // Step 9. Retrieve the ObjectName of the queue. This is used to identify the server resources to manage
                    ObjectName on = ObjectNameBuilder.DEFAULT.getHornetQServerObjectName();

                    // Step 10. Create JMX Connector to connect to the server's MBeanServer
                    String url = MessageFormat.format("service:jmx:rmi://{0}/jndi/rmi://{0}:{1}/jmxrmi", host, port == null ? "3900" : port);
                    LOGGER.debug("HornetQ Server jmx url: {}", url);
                    JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), new HashMap());

                    // Step 11. Retrieve the MBeanServerConnection
                    MBeanServerConnection mbsc = connector.getMBeanServerConnection();

                    // Step 12. Create a JMSQueueControl proxy to manage the queue on the server
                    JMSServerControl serverControl = MBeanServerInvocationHandler.newProxyInstance(mbsc, on, JMSServerControl.class, false);

                    String serverControlVersion = serverControl.getVersion();
                    LOGGER.info("HornetQ Server version: {}", serverControlVersion);


                } catch (Exception e) {
                    LOGGER.info("can't log server version. error is: {}", e.toString());
                }
            }
        };

        try {

            new Thread(getVersionFromServer).start();

        } catch (Exception e) {
            LOGGER.info("can't log server version. error is: {}", e.toString());
        }
    }

    private static void printHQVersion(final Map<String, Map<String, String>> serverConnections, final ArrayList<String> serverConnectionKeys) {

        if (serverConnectionKeys != null && !serverConnectionKeys.isEmpty()) {

            String host = serverConnections.get(serverConnectionKeys.get(0)).get("host");
            String port = serverConnections.get(serverConnectionKeys.get(0)).get("jmxPort");
            if (port == null) {
                port = "3900";
            }

            printHQVersion(host, port);
        }
    }

    private static boolean isActiveActiveMode(Configuration subset) {
        Iterator<String> keys = subset.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.contains(".instance")) {
                return true;
            }
        }
        return false;
    }

    private static void connect() {

        try {
            connectInternal();
            INIT_READY.countDown();
        } catch (Exception e) {
            LOGGER.warn("can't connect to hornetQ: {}", e);
            infiniteRetry();
        }

    }

    private static void connectInternal() throws Exception {
        ServerLocator serverLocator = HornetQClient.createServerLocatorWithHA(transportConfigurationsArray);
//        serverLocator = serverLocatorWithHA;
        serverLocator.setRetryInterval(100);
        serverLocator.setRetryIntervalMultiplier(2);
        serverLocator.setReconnectAttempts(1);
        serverLocator.setInitialConnectAttempts(1);
        serverLocator.setFailoverOnInitialConnection(true);
        serverLocator.setClientFailureCheckPeriod(5000);
        serverLocator.setConnectionTTL(10000);
        serverLocator.setCallTimeout(10000);


        try {
            serverLocator.setAckBatchSize(1);
        } catch (Exception e) {
            LOGGER.debug("error trying to set ack batch size: {}", e);
        }

        serverLocators.add(serverLocator);


        ClientSession clientSession = serverLocator.createSessionFactory().createSession(true, true);
        clientSession.start();
        clientSession.addFailureListener(new FoundationQueueFailureListener(clientSession));

        clientSessions.add(clientSession);

    }

    static void infiniteRetry() {

        Thread thread = new Thread(new Runnable() {

            private boolean done = false;

            @Override
            public void run() {

                while (!done) {

                    LOGGER.trace("attempting to reconnect to HornetQ");
                    try {
                        connectInternal();
                        LOGGER.trace("reconnect to HornetQ is successful");
                        INIT_READY.countDown();
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
        });
        thread.setName("hornetq-reconnect");
        thread.setDaemon(true);
        thread.start();
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
