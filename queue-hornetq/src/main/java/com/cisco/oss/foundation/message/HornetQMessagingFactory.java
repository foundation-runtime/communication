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
import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.google.common.collect.Lists;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.core.client.SessionFailureListener;
import org.hornetq.api.core.management.ObjectNameBuilder;
import org.hornetq.api.jms.management.JMSQueueControl;
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
    private static final Set<ClientSession> sessions = new HashSet<ClientSession>();
    public static ThreadLocal<ClientSession> sessionThreadLocal = new ThreadLocal<ClientSession>();
    private static ServerLocator serverLocator = null;
    private static ClientSession clientSession = null;
    private static Map<String, MessageConsumer> consumers = new ConcurrentHashMap<String, MessageConsumer>();
    private static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();
    private static TransportConfiguration[] transportConfigurationsArray = null;
    public static CountDownLatch INIT_READY = new CountDownLatch(1);


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


    public static ClientSession getSession(SessionFailureListener sessionFailureListener) {


        if (sessionThreadLocal.get() == null) {
//        if (sessionThreadLocal.get() == null || sessionIsClosed(sessionThreadLocal.get())) {
            try {
                LOGGER.debug("creating a new session");

                //session is created with auto-commit and auto-ack set to true
                ClientSession hornetQSession = serverLocator.createSessionFactory().createSession(true, true);
                if (sessionFailureListener != null) {
                    hornetQSession.addFailureListener(sessionFailureListener);
                }
                hornetQSession.start();
                sessionThreadLocal.set(hornetQSession);
                sessions.add(hornetQSession);
//            } catch (HornetQException e) {
//                LOGGER.warn("can't create hornetq session: {}", e, e);
//                infiniteRetry();
//                throw new QueueException(e);
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
    public static ClientSession getSession() {
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
//            nettyFactory = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName())).createSessionFactory();
            final Map<String, Map<String, String>> serverConnections = ConfigUtil.parseComplexArrayStructure("service.queue.connections");
            if (serverConnections != null && !serverConnections.isEmpty()) {
                final ArrayList<String> serverConnectionKeys = Lists.newArrayList(serverConnections.keySet());
                Collections.sort(serverConnectionKeys);
                transportConfigurationsArray = new TransportConfiguration[serverConnectionKeys.size()];
                List<TransportConfiguration> transportConfigurations = new ArrayList<TransportConfiguration>();





                for (String serverConnectionKey : serverConnectionKeys) {

                    Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                    Map<String, Object> map = new HashMap<String, Object>();

                    map.put("host", serverConnection.get("host"));
                    map.put("port", serverConnection.get("port"));


                    transportConfigurations.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), map));

                }

                LOGGER.info("HornetQ version: {}", VersionLoader.getVersion().getVersionName());

                Runnable getVersionFromServer = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(serverConnectionKeys != null && !serverConnectionKeys.isEmpty()){

                                String host = serverConnections.get(serverConnectionKeys.get(0)).get("host");
                                String port = serverConnections.get(serverConnectionKeys.get(0)).get("jmxPort");

                                // Step 9. Retrieve the ObjectName of the queue. This is used to identify the server resources to manage
                                ObjectName on = ObjectNameBuilder.DEFAULT.getHornetQServerObjectName();

                                // Step 10. Create JMX Connector to connect to the server's MBeanServer
                                String url = MessageFormat.format("service:jmx:rmi://{0}/jndi/rmi://{0}:{1}/jmxrmi", host, port);
                                LOGGER.debug("HornetQ Server jmx url: {}", url);
                                JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), new HashMap());

                                // Step 11. Retrieve the MBeanServerConnection
                                MBeanServerConnection mbsc = connector.getMBeanServerConnection();

                                // Step 12. Create a JMSQueueControl proxy to manage the queue on the server
                                JMSServerControl serverControl = MBeanServerInvocationHandler.newProxyInstance(mbsc, on, JMSServerControl.class, false);

                                String serverControlVersion = serverControl.getVersion();
                                LOGGER.info("HornetQ Server version: {}", serverControlVersion);

                            }
                        } catch (Exception e) {
                            LOGGER.info("can't log server version. error is: {}", e.toString());
                        }
                    }
                };



                try {

//                    runWithTimeout(getVersionFromServer, 2, TimeUnit.SECONDS);
                    new Thread(getVersionFromServer).start();

                } catch (Exception e) {
                    LOGGER.info("can't log server version. error is: {}", e.toString());
                }


                transportConfigurations.toArray(transportConfigurationsArray);
                connect();

            } else {
                throw new IllegalArgumentException("'service.queue.connections' must contain at least on host/port pair.");
            }

        } catch (Exception e) {
            LOGGER.error("can't create hornetq service locator: {}", e, e);
            throw new QueueException(e);
        }

    }

    public static void runWithTimeout(final Runnable runnable, long timeout, TimeUnit timeUnit) throws Exception {
        runWithTimeout(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        }, timeout, timeUnit);
    }

    public static <T> T runWithTimeout(Callable<T> callable, long timeout, TimeUnit timeUnit) throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<T> future = executor.submit(callable);
        try {
            return future.get(timeout, timeUnit);
        } catch (ExecutionException e) {
            //unwrap the root cause
            Throwable t = e.getCause();
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new IllegalStateException(t);
            }
        }finally {
            executor.shutdown();
        }
    }

    private static void connect() {
//        if (serverLocator != null) {
//            try {
//            serverLocator.close();
//            } catch (Exception e) {
//                LOGGER.trace("problem closing jms connection: {}", e);
//            }
//        }

        serverLocator = HornetQClient.createServerLocatorWithHA(transportConfigurationsArray);
        serverLocator.setRetryInterval(100);
        serverLocator.setRetryIntervalMultiplier(2);
        serverLocator.setReconnectAttempts(1);
        serverLocator.setInitialConnectAttempts(1);
        try {
            serverLocator.setAckBatchSize(1);
        } catch (Exception e) {
            LOGGER.debug("error trying to set ack batch size: {}", e);
        }

        try {

            clientSession = serverLocator.createSessionFactory().createSession(true, true);
            clientSession.start();
            clientSession.addFailureListener(new FoundationQueueFailureListener(clientSession));
            INIT_READY.countDown();
        } catch (Exception e) {
            LOGGER.warn("can't connect to hornetQ: {}", e);
            infiniteRetry();
        }

    }

    static void infiniteRetry() {

        Thread thread = new Thread(new Runnable() {

            private boolean done = false;

            @Override
            public void run() {

                while (!done) {

                    LOGGER.trace("attempting to reconnect to HornetQ");
                    try {
                        connect();
                        done = true;
                    } catch (Exception e) {
                        LOGGER.trace("failed to reconnect. retrying...", e);
                        try {
                            Thread.sleep(ConfigurationFactory.getConfiguration().getInt("service.queue.attachRetryDelay", 60000));
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
