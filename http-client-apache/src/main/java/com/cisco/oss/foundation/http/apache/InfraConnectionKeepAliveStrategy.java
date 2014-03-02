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

package com.cisco.oss.foundation.http.apache;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Created by Yair Ogen on 1/26/14.
 */
public class InfraConnectionKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy {

    private long idleTimeout = -1;

    public InfraConnectionKeepAliveStrategy(long idleTimeout){
        this.idleTimeout = idleTimeout;
    }

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long timeout = super.getKeepAliveDuration(response, context);
        if(timeout == -1){
           timeout = idleTimeout;
        }

        return timeout;
    }
}
