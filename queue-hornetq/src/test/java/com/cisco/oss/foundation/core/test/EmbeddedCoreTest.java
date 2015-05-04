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

package com.cisco.oss.foundation.core.test;

import com.cisco.oss.foundation.message.*;
import com.twitter.jsr166e.LongAdder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yair Ogen on 09/04/2014.
 */
public class EmbeddedCoreTest {

    @Test
    @Ignore
    public void testSimpleSendRecieveListener() throws Exception {

        int numIter = 1000;
        final CountDownLatch countDownLatch = new CountDownLatch(numIter *2);
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        final MessageProducer producer = HornetQMessagingFactory.createProducer("example");


//        for (int i = 0; i < numIter; i++) {
//            producer.sendMessage("hi there: " + i);
//        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("consumer1");
                consumer.registerMessageHandler(new AbstractHornetQMessageHandler("consumer1") {
                    @Override
                    public void onMessage(Message message) {
//                        System.out.println("[1] " + message.getPayloadAsString());
                        countDownLatch.countDown();
                    }
                });
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("consumer2");
                consumer.registerMessageHandler(new AbstractHornetQMessageHandler("consumer2") {
                    @Override
                    public void onMessage(Message message) {
//                        System.out.println("[2] " + message.getPayloadAsString());
                        countDownLatch.countDown();
                    }
                });
            }
        }).start();

        for (int i = 0; i < numIter; i++) {
            final int counter = i;
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    producer.sendMessage("hi there: " + counter);
                    if (counter % 10 == 0) {
                        System.out.println("[" + Thread.currentThread().getName() + "] read/write: index: " + counter);

                    }
                }
            });


        }


        countDownLatch.await();
    }

    @Test
    @Ignore
    public void testSimpleSendRecieve() throws Exception {

        final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("directConsumer");
        final MessageProducer producer = HornetQMessagingFactory.createProducer("directExample");
        int NUM_ITER = 10000;
        for (int i = 0; i < 10000; i++) {
            producer.sendMessage("hi there");
            consumer.receive().getPayloadAsString();
            if (i % 500 == 0) {
                System.out.println("index: " + i);
            }
        }
        System.out.println("warm up done");

        long start = System.currentTimeMillis();

        for (int i = 0; i < NUM_ITER; i++) {
            producer.sendMessage("hi there");
//            consumer.receive().getPayloadAsString();
            if (i % 500 == 0) {
                System.out.println("index: " + i);
            }
        }

        long end = System.currentTimeMillis();

        double total = (end - start) / 1000D;

        double tps = NUM_ITER / total;

        System.out.println("tps write - no read: " + tps);


        start = System.currentTimeMillis();

        for (int i = 0; i < NUM_ITER; i++) {
            consumer.receive().getPayloadAsString();
//            consumer.receive().getPayloadAsString();
            if (i % 500 == 0) {
                System.out.println("index: " + i);
            }
        }

        end = System.currentTimeMillis();

        total = (end - start) / 1000D;

        tps = NUM_ITER / total;

        System.out.println("tps read - no write: " + tps);


    }

    @Test
    @Ignore
    public void testParralelAsyncReceiveAndSendTopic() {

        try {
//            final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("directConsumer");
            final MessageProducer producer = HornetQMessagingFactory.createProducer("example");

            int NUM_ITER = 5000;
//            int numberOfConsumers = 2;

            ExecutorService threadPool = Executors.newFixedThreadPool(10);

            final CountDownLatch countDownLatch = new CountDownLatch(NUM_ITER*2);
            final Set<String> consumer1Messages = new HashSet<String>();
            final Set<String> consumer2Messages = new HashSet<String>();

            long start = System.currentTimeMillis();

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    final MessageConsumer consumer1 = HornetQMessagingFactory.createConsumer("consumer1");
//                    consumer1.registerMessageHandler(new AbstractHornetQMessageHandler() {
//                        @Override
//                        public void onMessage(Message message) {
////                        System.out.println("[1] " + message.getPayloadAsString());
//                            String payloadAsString = message.getPayloadAsString();
//                            if(consumer1Messages.add(payloadAsString)) {
//                                countDownLatch.countDown();
//                            }else{
//                                System.err.println("@@@@@@@@@@@@@@   " + payloadAsString + "   @@@@@@@@@@@@@@@@@");
//                            }
//                        }
//                    });
//                }
//            }).start();
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    final MessageConsumer consumer2 = HornetQMessagingFactory.createConsumer("consumer2");
//                    consumer2.registerMessageHandler(new AbstractHornetQMessageHandler() {
//                        @Override
//                        public void onMessage(Message message) {
////                        System.out.println("[2] " + message.getPayloadAsString());
//                            String payloadAsString = message.getPayloadAsString();
//                            if(consumer2Messages.add(payloadAsString)) {
//                                countDownLatch.countDown();
//                            }else{
//                                System.err.println("@@@@@@@@@@@@@@   " + payloadAsString + "   @@@@@@@@@@@@@@@@@");
//                            }
//                        }
//                    });
//                }
//            }).start();

//            for (int i=0; i<numberOfConsumers; i++) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        consumer.registerMessageHandler(new AbstractHornetQMessageHandler() {
//                            @Override
//                            public void onMessage(Message message) {
//                                countDownLatch.countDown();
//                                long count = countDownLatch.getCount();
//                                if (count % 1000 == 0) {
//                                    System.out.println("[" + Thread.currentThread().getName() + "]. consumer: " + count);
//                                }
//                            }
//                        });
//                    }
//                }).start();
//                //            Thread.sleep(1000);
//            }

//                        }
//                    });


            for (int i = 0; i < NUM_ITER; i++) {
                final int counter = i;
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        producer.sendMessage("hi there: " + counter);
                        if (counter % 1000 == 0) {
                            System.out.println("[" + Thread.currentThread().getName() + "] read/write: index: " + counter);

                        }
                    }
                });

            }

            countDownLatch.await();

            System.out.println("*******************  "+countDownLatch.getCount()+"   *****************************");

//        Thread.sleep(5000);

            long end = System.currentTimeMillis();

            double total = (end - start) / 1000D;

            double tps = NUM_ITER / total;

            System.out.println("tps read and write: " + tps);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }


    @Test
    @Ignore
    public void testParralelAsyncReceiveAndSend() {

        try {
            final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("directConsumer");
            final MessageProducer producer = HornetQMessagingFactory.createProducer("directExample");

            int NUM_ITER = 50000;
            int numberOfConsumers = 2;

            ExecutorService threadPool = Executors.newFixedThreadPool(10);

            final CountDownLatch countDownLatch = new CountDownLatch(NUM_ITER);

            long start = System.currentTimeMillis();

            for (int i=0; i<numberOfConsumers; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        consumer.registerMessageHandler(new AbstractHornetQMessageHandler("directConsumer") {
                            @Override
                            public void onMessage(Message message) {
                                countDownLatch.countDown();
                                long count = countDownLatch.getCount();
                                if (count % 1000 == 0) {
                                    System.out.println("[" + Thread.currentThread().getName() + "]. consumer: " + count);
                                }
                            }
                        });
                    }
                }).start();
    //            Thread.sleep(1000);
            }

//                        }
//                    });


            for (int i = 0; i < NUM_ITER; i++) {
                final int counter = i;
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        producer.sendMessage("hi there: " + counter);
                        if (counter % 1000 == 0) {
                            System.out.println("[" + Thread.currentThread().getName() + "] read/write: index: " + counter);

                        }
                    }
                });

            }

            countDownLatch.await();

            System.out.println("*******************  "+countDownLatch.getCount()+"   *****************************");

//        Thread.sleep(5000);

            long end = System.currentTimeMillis();

            double total = (end - start) / 1000D;

            double tps = NUM_ITER / total;

            System.out.println("tps read and write: " + tps);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }



    @Test
    @Ignore
    public void testParralelSendAndSyncRecieve() throws Exception {

        final MessageProducer producer = HornetQMessagingFactory.createProducer("myExampleDirect");
        final MessageConsumer consumer = HornetQMessagingFactory.createConsumer("myExampleDirect");

        int NUM_ITER = 100;
        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_ITER + 10);
        final CountDownLatch countDownLatch = new CountDownLatch(NUM_ITER);


        long start = System.currentTimeMillis();

        final LongAdder longAdder = new LongAdder();

        for (int i = 0; i < NUM_ITER; i++) {

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    producer.sendMessage("hi there");
                }
            });

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    consumer.receive().getPayloadAsString();
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

        System.out.println("*******************  "+countDownLatch.getCount()+"   *****************************");

        long end = System.currentTimeMillis();

        double total = (end - start) / 1000D;

        double tps = NUM_ITER / total;

        System.out.println("tps read and write: " + tps);

        threadPool.shutdown();
    }
}
