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

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.monitoring.CommunicationInfo;
import com.cisco.oss.foundation.monitoring.serverconnection.ServerConnectionDetails;
import org.apache.commons.configuration.Configuration;

/**
 * Created by Yair Ogen on 16/06/2014.
 */
public abstract class AbstractMessageProducer implements MessageProducer {

    protected String producerName;
    protected String queueName = "";
    private ServerConnectionDetails serverConnectionDetails = new ServerConnectionDetails(producerName,getProducerImpl(), "N/A", -1, -1);

    public AbstractMessageProducer(String producerName){
        this.producerName = producerName;
    }

    @Override
    public void postSendMessage() {
        if (isMonitoringEnabled()) {
            CommunicationInfo.INSTANCE.transactionStarted(serverConnectionDetails,queueName);
        }
    }

    @Override
    public void preSendMessage() {
        if (isMonitoringEnabled()) {
            CommunicationInfo.INSTANCE.transactionFinished(serverConnectionDetails,queueName, false, "");
        }
    }

    private boolean isMonitoringEnabled() {
        Configuration configuration = ConfigurationFactory.getConfiguration();
        Configuration subset = configuration.subset(producerName);
        boolean isMonitoringEnabled = subset.getBoolean("queue.isMonitoringEnabled",false);
        return isMonitoringEnabled;
    }

    public abstract String getProducerImpl();
}
