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

package com.cisco.oss.foundation.core.test;

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.message.AbstractRabbitMQMessageHandler;
import com.cisco.oss.foundation.message.Message;
import com.cisco.oss.foundation.message.MessageConsumer;
import com.cisco.oss.foundation.message.RabbitMQMessagingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class RabbitMQConsumerMain2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumerMain2.class);
    
    public static void main(String[] args) throws Exception {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                FlowContextFactory.createFlowContext();
                final MessageConsumer consumer1 = RabbitMQMessagingFactory.createConsumer("consumer2");
                consumer1.registerMessageHandler(new AbstractRabbitMQMessageHandler("consumer2") {
                    @Override
                    public void onMessage(Message message) {
                        LOGGER.info("message: {}", message.getPayloadAsString());

                    }
                });
            }
        });

        t.setDaemon(false);

        t.start();

//        MessageConsumer consumer1 = RabbitMQMessagingFactory.createConsumer("consumer2");
//        Message message = consumer1.receive();
//        LOGGER.info("got message: " + message.getPayloadAsString());

        System.in.read();

    }

}