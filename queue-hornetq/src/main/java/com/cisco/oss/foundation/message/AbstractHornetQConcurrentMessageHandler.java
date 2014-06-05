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

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.lang3.StringUtils;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;

/**
 * Basic abstract handler that exposes the foundation message handler but also implements silently the hornetq message handler
 * Created by Yair Ogen on 24/04/2014.
 */
public abstract class AbstractHornetQConcurrentMessageHandler implements MessageHandler, org.hornetq.api.core.client.MessageHandler {

    public AbstractHornetQConcurrentMessageHandler() {
    }

    @Override
    public final void onMessage(ClientMessage message) {
        try {
            message.acknowledge();
        } catch (HornetQException e) {
            throw new QueueException(e);
        }
        String flowContextStr = message.getStringProperty(QueueConstants.FLOW_CONTEXT_HEADER);
        if (StringUtils.isNotBlank(flowContextStr)) {
            FlowContextFactory.deserializeNativeFlowContext(flowContextStr);
        }
        Message msg = new HornetQMessage(message);
        onMessage(msg);
    }

}
