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

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.SessionFailureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Yair Ogen on 22/09/2014.
 */
public class FoundationQueueFailureListener implements SessionFailureListener{
    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationQueueFailureListener.class);
    private ClientSession hornetQSession = null;

    public FoundationQueueFailureListener(ClientSession hornetQSession) {
        this.hornetQSession = hornetQSession;
    }

    @Override
    public void connectionFailed(HornetQException exception, boolean failedOver) {
        LOGGER.error("failed connection: {}, failing over: {}",exception, failedOver);
//        HornetQMessagingFactory.infiniteRetry();
//        if(hornetQSession != null){
//            try {
//                hornetQSession.close();
//            } catch (HornetQException e) {
//                LOGGER.trace("can't close the session");
//            }
//            HornetQMessagingFactory.sessions.remove(hornetQSession);
//        }

    }

    @Override
    public void beforeReconnect(HornetQException exception) {
        LOGGER.error("beforeReconnect: {}",exception);

    }
}
