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

import com.cisco.oss.foundation.message.MessageProducer;
import com.cisco.oss.foundation.message.RabbitMQMessagingFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class RabbitMQProducerMain {

    public static void main(String[] args) throws Exception {

        final MessageProducer producer = RabbitMQMessagingFactory.createProducer("example");

        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        while(true){

            final byte[] bytes = new byte[100];
            System.in.read(bytes);
//            producer.sendMessage("hello: " + new String(bytes));

//            for (int i=0; i < 500; i++) {
//                sendMessage(producer, bytes,"1");
//                Thread.sleep(1000);
//            }
            Runnable target = new Runnable() {
                @Override
                public void run() {
                    try {
                        sendMessage(producer, bytes, "1");
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                }
            };

//            new Thread(target).start();

            threadPool.execute(target);



//            sendMessage(producer, bytes,"1");
//            sendMessage(producer, bytes,"1");
//            sendMessage(producer, bytes,"1");
//            sendMessage(producer, bytes,"1");
//
//
//            sendMessage(producer, bytes,"2");
//            sendMessage(producer, bytes,"2");
//            sendMessage(producer, bytes,"2");
//            sendMessage(producer, bytes,"2");
//            sendMessage(producer, bytes,"2");


        }


    }

    private static Map<String, Object> sendMessage(MessageProducer producer, byte[] bytes, String hhId) {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("key1","value2");
//        props.put(MessageImpl.HDR_GROUP_ID.toString(),hhId);
        props.put("HHID",hhId);
        producer.sendMessage("hello: " + new String(bytes), props);
        return props;
    }
}