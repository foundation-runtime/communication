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

package com.cisco.oss.foundation.http.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpClient;
import com.cisco.oss.foundation.http.HttpRequest;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import org.apache.commons.configuration.Configuration;

/**
 * Created by Yair Ogen on 1/19/14.
 */
public class JettyHttpClientFactory {

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, boolean enableLoadBalancing){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration(), enableLoadBalancing);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration());
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing){
        return createHttpClient(apiName, LoadBalancerStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration, enableLoadBalancing);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, Configuration configuration){
        return createHttpClient(apiName, configuration, true);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        return createHttpClient(apiName, highAvailabilityStrategyType, ConfigurationFactory.getConfiguration());
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration){
        return createHttpClient(apiName, highAvailabilityStrategyType, configuration, true);
    }


    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration, boolean enableLoadBalancing){
        ConfigurationFactory.getConfiguration();
        try {
            HttpClient client = null;
            if(highAvailabilityStrategyType == null){
                client = new JettyHttpClient(apiName, configuration, enableLoadBalancing);
            }else{
                client = new JettyHttpClient(apiName, highAvailabilityStrategyType, configuration, enableLoadBalancing);
            }
            return client;
        } catch (Exception e) {
            throw new ClientException(e.toString(),e);
        }
    }
}
