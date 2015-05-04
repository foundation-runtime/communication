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
import com.cisco.oss.foundation.monitoring.services.ServiceDetails;
import org.apache.commons.configuration.Configuration;

/**
 * Created by Yair Ogen on 15/06/2014.
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    private String consumerName;
    private ServiceDetails serviceDetails = new ServiceDetails(getServiceDescription(),consumerName,getProtocol(),-1);

    public AbstractMessageHandler(String consumerName){
        this.consumerName = consumerName;
    }

//    @Override
    public void preMessageProcessing(Message message) {
        if(isMonitoringEnabled()){
            CommunicationInfo.INSTANCE.transactionStarted(serviceDetails,consumerName);
        }
    }

//    @Override
    public void postMessageProcessing(Message message) {
        if(isMonitoringEnabled()){
            CommunicationInfo.INSTANCE.transactionFinished(serviceDetails,consumerName,false, "N/A");
        }
    }

    private boolean isMonitoringEnabled() {
        Configuration configuration = ConfigurationFactory.getConfiguration();
        Configuration subset = configuration.subset(consumerName);
        boolean isMonitoringEnabled = subset.getBoolean("queue.isMonitoringEnabled",false);
        return isMonitoringEnabled;
    }

    public abstract String getServiceDescription();

    public abstract String getProtocol();
}
