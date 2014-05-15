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

package com.cisco.vss.foundation.core.test;

import com.cisco.vss.foundation.message.*;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class Consumer1Main {

    public static void main(String[] args) throws Exception {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final MessageConsumer consumer1 = HornetQMessagingFactory.createConsumer("consumer1");
                consumer1.registerMessageHandler(new AbstractHornetQMessageHandler() {
                    @Override
                    public void onMessage(Message message) {
                        System.out.println("[1] " + message.getPayloadAsString());

                    }
                });
            }
        });

        t.setDaemon(false);

        t.start();

        System.in.read();




    }
}