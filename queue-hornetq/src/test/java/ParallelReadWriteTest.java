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

import com.twitter.jsr166e.LongAdder;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.*;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yair Ogen on 04/05/2014.
 */
public class ParallelReadWriteTest {

    private static final ThreadLocal<ClientProducer> producer = new ThreadLocal<ClientProducer>();
    private static final ThreadLocal<ClientConsumer> consumer = new ThreadLocal<ClientConsumer>();
    private static final Set<ClientProducer> producers = new HashSet<ClientProducer>();
    private static final Set<ClientConsumer> consumers = new HashSet<ClientConsumer>();

    public static ThreadLocal<ClientSession> sessionThreadLocal = new ThreadLocal<ClientSession>();

    private static ServerLocator serverLocator = null;
    private static Logger LOGGER = LoggerFactory.getLogger("hornetq-test");

    static {
        init();
    }

    private static void init() {
        try {
//            nettyFactory = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName())).createSessionFactory();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("host", "localhost");
            map.put("port", "5445");
            TransportConfiguration server1 = new TransportConfiguration(NettyConnectorFactory.class.getName(), map);

            HashMap<String, Object> map2 = new HashMap<String, Object>();
            map2.put("host", "localhost");
            map2.put("port", "6445");
            TransportConfiguration server2 = new TransportConfiguration(NettyConnectorFactory.class.getName(), map2);

            serverLocator = HornetQClient.createServerLocatorWithHA(server1, server2);
        } catch (Exception e) {
            LOGGER.error("can't create hornetq session: {}", e, e);
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    for (ClientProducer clientProducer : producers) {
                        clientProducer.close();
                    }
                    for (ClientConsumer clientConsumer : consumers) {
                        clientConsumer.close();
                    }
                } catch (HornetQException e) {
                    LOGGER.error("can't close producer, error: {}", e, e);
                }
            }
        });

    }

    public static ClientSession getSession() {
        if (sessionThreadLocal.get() == null) {
            try {
                ClientSession hornetQSession = serverLocator.createSessionFactory().createSession(true, true);
                hornetQSession.start();
                sessionThreadLocal.set(hornetQSession);
            } catch (Exception e) {
                LOGGER.error("can't create hornetq session: {}", e, e);
                throw new RuntimeException(e);
            }
        }
        return sessionThreadLocal.get();
    }

    private ClientProducer getProducer(String queueName) {
        try {
            if (producer.get() == null) {
                ClientProducer clientProducer = getSession().createProducer(queueName);
                producers.add(clientProducer);
                producer.set(clientProducer);
            }
            return producer.get();
        } catch (Exception e) {
            LOGGER.error("can't create queue consumer: {}", e, e);
            throw new RuntimeException(e);
        }
    }

    private ClientConsumer getConsumer(String queueName) {
        try {
            if (consumer.get() == null) {
                ClientConsumer clientConsumer = getSession().createConsumer(queueName);
                consumers.add(clientConsumer);
                consumer.set(clientConsumer);
            }
            return consumer.get();
        } catch (Exception e) {
            LOGGER.error("can't create queue consumer: {}", e, e);
            throw new RuntimeException(e);
        }
    }

    @Test
    @Ignore
    public void parralelReadAndWrite() throws Exception{


        int NUM_ITER = 5000;
        int numberOfConsumers = 2;

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        final CountDownLatch countDownLatch = new CountDownLatch(NUM_ITER);

        long start = System.currentTimeMillis();

        for (int i = 0; i < numberOfConsumers; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getConsumer("foundation.myExampleDirect").setMessageHandler(new MessageHandler() {

                            @Override
                            public void onMessage(ClientMessage message) {
                                countDownLatch.countDown();
                                long count = countDownLatch.getCount();
                                if (count % 1000 == 0) {
                                    System.out.println("[" + Thread.currentThread().getName() + "]. consumer: " + count);
                                }
                            }
                        });
                    } catch (HornetQException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

//                        }
//                    });


        for (int i = 0; i < NUM_ITER; i++) {
            final int counter = i;
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    ClientMessage clientMessage = getSession().createMessage(true);
                    clientMessage.getBodyBuffer().writeString("hi there: " + counter);
                    try {
                        getProducer("foundation.myExampleDirect").send(clientMessage);
                    } catch (HornetQException e) {
                        e.printStackTrace();
                    }
                    if (counter % 1000 == 0) {
                        System.out.println("[" + Thread.currentThread().getName() + "] read/write: index: " + counter);

                    }
                }
            });

        }

        countDownLatch.await();

//        Thread.sleep(5000);

        long end = System.currentTimeMillis();

        double total = (end - start) / 1000D;

        double tps = NUM_ITER / total;

        System.out.println("tps read and write: " + tps);
    }


    @Test
    @Ignore
    public void testParralelSendAndSyncRecieveUsingCore() throws Exception {


        int NUM_ITER = 100;
        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_ITER + 10);
        final CountDownLatch countDownLatch = new CountDownLatch(NUM_ITER);


        long start = System.currentTimeMillis();

        final LongAdder longAdder = new LongAdder();

        for (int i = 0; i < NUM_ITER; i++) {

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    ClientMessage clientMessage = getSession().createMessage(true);
                    clientMessage.getBodyBuffer().writeString("hi there");
                    try {
                        getProducer("myExampleDirect").send(clientMessage);
                    } catch (HornetQException e) {
                        e.printStackTrace();
                    }
                }
            });

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClientMessage clientMessage = getConsumer("myExampleDirect").receive();
                        clientMessage.acknowledge();
                    } catch (HornetQException e) {
                        e.printStackTrace();
                    }
                    countDownLatch.countDown();
                    longAdder.add(1);
                    long sum = longAdder.sum();
                    if (sum % 500 == 0) {
                        System.out.println("read/write index: " + sum);
                    }

                }
            });

        }

        countDownLatch.await();

        long end = System.currentTimeMillis();

        double total = (end - start) / 1000D;

        double tps = NUM_ITER / total;

        System.out.println("tps read and write: " + tps);

        threadPool.shutdown();
    }
}
