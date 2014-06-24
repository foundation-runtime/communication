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

import com.cisco.oss.foundation.message.HornetQMessagingFactory;
import com.cisco.oss.foundation.message.MessageProducer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yair Ogen on 23/04/2014.
 */
public class ProducerMain {

    public static void main(String[] args) throws Exception {

        final MessageProducer producer = HornetQMessagingFactory.createProducer("example");

        while(true){

            byte[] bytes = new byte[100];
            System.in.read(bytes);
//            producer.sendMessage("hello: " + new String(bytes));
            sendMessage(producer, bytes,"1");
            sendMessage(producer, bytes,"1");
            sendMessage(producer, bytes,"1");
            sendMessage(producer, bytes,"1");
            sendMessage(producer, bytes,"1");


            sendMessage(producer, bytes,"2");
            sendMessage(producer, bytes,"2");
            sendMessage(producer, bytes,"2");
            sendMessage(producer, bytes,"2");
            sendMessage(producer, bytes,"2");


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