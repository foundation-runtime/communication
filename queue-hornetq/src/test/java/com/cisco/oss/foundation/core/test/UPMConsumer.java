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

import com.cisco.oss.foundation.message.AbstractHornetQMessageHandler;
import com.cisco.oss.foundation.message.HornetQMessagingFactory;
import com.cisco.oss.foundation.message.Message;
import com.cisco.oss.foundation.message.MessageConsumer;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class UPMConsumer {
    
    public static void main(String[] args) throws Exception {

//        MyRunnable myRunnable = new MyRunnable();
//
//        myRunnable.setDaemon(false);
//
//        myRunnable.start();

        final MessageConsumer consumer1 = HornetQMessagingFactory.createConsumer("upmConsumer");
        consumer1.registerMessageHandler(new AbstractHornetQMessageHandler("upmConsumer") {
            @Override
            public void onMessage(Message message) {
                System.out.println("[1] " + message.getPayloadAsString());

            }
        });

        int i=0;

        System.in.read();

    }

    private static class MyRunnable extends Thread{
        @Override
        public void run() {
            final MessageConsumer consumer1 = HornetQMessagingFactory.createConsumer("upmConsumer");
            consumer1.registerMessageHandler(new AbstractHornetQMessageHandler("upmConsumer") {
                @Override
                public void onMessage(Message message) {
                    System.out.println("[1] " + message.getPayloadAsString());

                }
            });
        }
    }

}