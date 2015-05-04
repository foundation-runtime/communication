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

import com.cisco.oss.foundation.message.AbstractHornetQMessageHandler;
import com.cisco.oss.foundation.message.HornetQMessagingFactory;
import com.cisco.oss.foundation.message.Message;
import com.cisco.oss.foundation.message.MessageConsumer;
import org.hornetq.core.message.impl.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class Consumer2Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer2Main.class);

    public static void main(String[] args) throws Exception {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final MessageConsumer consumer1 = HornetQMessagingFactory.createConsumer("consumer2");
                consumer1.registerMessageHandler(new AbstractHornetQMessageHandler("consumer2") {
                    @Override
                    public void onMessage(Message message) {
                        LOGGER.info("message: {}, HHID: {}, groupId: {}", message.getPayloadAsString(), message.getProperties().get("HHID"), message.getProperties().get(MessageImpl.HDR_GROUP_ID.toString()));

                    }
                });
            }
        });

        t.setDaemon(false);

        t.start();

        System.in.read();

    }
}