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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
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
    private static List<Channel> channels = new CopyOnWriteArrayList<>();
    private static Connection connection = null;
    static AtomicBoolean IS_RECONNECT_THREAD_RUNNING = new AtomicBoolean(false);
    static CountDownLatch INIT_LATCH = new CountDownLatch(1);



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

                    for (Channel channel : channels) {
                        channel.close();
                    }

                    connection.close();

                } catch (Exception e) {
                    LOGGER.error("can't close RabbitMQ resources, error: {}", e, e);
                }
            }
        });
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

            if(isEnabled){
                connectionFactory.setUsername(userName);
                connectionFactory.setPassword(password);

            }

            List<Address> addresses = new ArrayList<>(5);

            for (String serverConnectionKey : serverConnectionKeys) {

                Map<String, String> serverConnection = serverConnections.get(serverConnectionKey);

                String host = serverConnection.get("host");
                int port = Integer.parseInt(serverConnection.get("port"));
                addresses.add(new Address(host,port));
//              connectionFactory.setHost(host);
//              connectionFactory.setPort(Integer.parseInt(port));
            }
            Address[] addrs = new Address[0];
            connection = connectionFactory.newConnection(addresses.toArray(addrs));
            IS_RECONNECT_THREAD_RUNNING.set(false);
            INIT_LATCH.countDown();
            INIT_LATCH = new CountDownLatch(1);

        } catch (Exception e) {
            LOGGER.error("can't create RabbitMQ Connection: {}", e, e);
            triggerReconnectThread();
            throw new QueueException(e);
        }
    }

    static Channel getChannel(){
        try {
            if(channelThreadLocal.get() == null){
                Channel channel = connection.createChannel();
                channelThreadLocal.set(channel);
                channels.add(channel);
            }

            return channelThreadLocal.get();
        } catch (IOException e) {
            throw new QueueException("can't create channel: " + e.toString(), e);
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

    static void triggerReconnectThread (){
      if (IS_RECONNECT_THREAD_RUNNING.compareAndSet(false,true)){
          Thread reconnectThread = new Thread(new Runnable() {
              @Override
              public void run() {

                  boolean isInReconnect = IS_RECONNECT_THREAD_RUNNING.get();
                  while(isInReconnect){
                      try {
                          connect();
                      } catch (Exception e) {
                          LOGGER.trace("reconnect failed: " + e);
                          try {
                              Thread.sleep(ConfigurationFactory.getConfiguration().getInt("service.queue.attachRetryDelay", 10000));
                              isInReconnect = IS_RECONNECT_THREAD_RUNNING.get();
                          } catch (InterruptedException e1) {
                              LOGGER.trace("thread interrupted!!!", e1);
                          }
                      }
                  }

              }
          }, "RabbitMQ-Reconnect");

          reconnectThread.start();

      }
    }


}
