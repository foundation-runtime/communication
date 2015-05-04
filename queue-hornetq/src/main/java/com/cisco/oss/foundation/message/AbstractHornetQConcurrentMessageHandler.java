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

package com.cisco.oss.foundation.message;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic abstract handler that exposes the foundation message handler but also implements silently the hornetq message handler
 * Created by Yair Ogen on 24/04/2014.
 */
public abstract class AbstractHornetQConcurrentMessageHandler extends AbstractHornetQMessageHandler implements ConcurrentMessageHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHornetQConcurrentMessageHandler.class);
    private MessageIdentifier messageIdentifier = null;
    private MessageDispatcher messageDispatcher = new LockMessageDispatcher(this);
    private Map<String, Object> onWorkIdentifierMap;

    public AbstractHornetQConcurrentMessageHandler(String consumerName) {
        super(consumerName);
        onWorkIdentifierMap = new ConcurrentHashMap<String, Object>();
    }

    public AbstractHornetQConcurrentMessageHandler(String consumerName, MessageIdentifier messageIdentifier) {
        super(consumerName);
        this.messageIdentifier = messageIdentifier;
        onWorkIdentifierMap = new ConcurrentHashMap<String, Object>();
    }

    @Override
    public final void onMessage(Message message) {
        messageDispatcher.handleMessage(message);
    }

    @Override
    public void onRecieveMessage(Message message) {
        LOGGER.trace("Received message '{}'", message);
    }

    /**
     * A message is dispatchable in case there is no other message with the same identifier that is in 'process' now.
     * If a message has 'null' identifier a true value will be return.
     */
    public boolean isDispatchable(Message message) {
        if(messageIdentifier != null) {
            String identifier = messageIdentifier.getIdentifier(message);
            LOGGER.trace("Message identifier is: {}", identifier);
            if (StringUtils.isNotEmpty(identifier)){
                return !onWorkIdentifierMap.containsKey(identifier);
            }
        }
        return true;
    }


    public void onDispatchMessage(Message message) {
        LOGGER.trace("Dispatch message '{}'", message);
        if(messageIdentifier != null){
            String identifier = messageIdentifier.getIdentifier(message);
            if (StringUtils.isNotEmpty(identifier)){
                onWorkIdentifierMap.put(identifier, 0);
            }
        }
    }

    public void onCompleteMessage(Message message) {
        LOGGER.trace("Complete message '{}'", message);
        if(messageIdentifier != null){
            String identifier = messageIdentifier.getIdentifier(message);
            if (StringUtils.isNotEmpty(identifier)){
                onWorkIdentifierMap.remove(identifier);
            }
        }
    }

    public void onException(Message message, Throwable throwable) {
        if(messageIdentifier != null){
            String identifier = messageIdentifier.getIdentifier(message);
            if (StringUtils.isNotEmpty(identifier)){
                onWorkIdentifierMap.remove(identifier);
            }
        }
    }


}
