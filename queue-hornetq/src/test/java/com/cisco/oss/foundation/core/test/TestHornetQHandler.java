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
import com.cisco.oss.foundation.message.Message;

/**
 * Created by Yair Ogen on 24/04/2014.
 */
public class TestHornetQHandler extends AbstractHornetQMessageHandler {

    public TestHornetQHandler(String consumerName){
        super(consumerName);
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(message.getPayloadAsString());
    }

    @Override
    public String getProtocol() {
        return "TestHornetQHandler";
    }

    @Override
    public String getServiceDescription() {
        return "DUMMY";
    }
}
