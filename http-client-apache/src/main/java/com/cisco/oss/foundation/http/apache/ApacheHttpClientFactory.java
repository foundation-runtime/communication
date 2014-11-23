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
import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpClient;
import com.cisco.oss.foundation.http.HttpRequest;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import org.apache.commons.configuration.Configuration;
import org.apache.http.conn.ssl.X509HostnameVerifier;

/**
 * Created by Yair Ogen on 1/19/14.
 */
public class ApacheHttpClientFactory {

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, boolean enableLoadBalancing){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration(), enableLoadBalancing);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration());
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, X509HostnameVerifier hostnameVerifier){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration(), hostnameVerifier);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing){
        return createHttpClient(apiName, LoadBalancerStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration, enableLoadBalancing, null);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing, X509HostnameVerifier hostnameVerifier){
        return createHttpClient(apiName, LoadBalancerStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration, enableLoadBalancing, hostnameVerifier);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, Configuration configuration){
        return createHttpClient(apiName, configuration, true);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, Configuration configuration, X509HostnameVerifier hostnameVerifier){
        return createHttpClient(apiName, configuration, true, hostnameVerifier);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        return createHttpClient(apiName, highAvailabilityStrategyType, ConfigurationFactory.getConfiguration());
    }


    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration){
        return createHttpClient(apiName, highAvailabilityStrategyType, configuration, true, null);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration, boolean enableLoadBalancing, X509HostnameVerifier hostnameVerifier){
        try {
            HttpClient client = null;
            if(highAvailabilityStrategyType == null){
                client = new ApacheHttpClient(apiName, configuration, enableLoadBalancing, hostnameVerifier);
            }else{
                client = new ApacheHttpClient(apiName, highAvailabilityStrategyType, configuration, enableLoadBalancing, hostnameVerifier);
            }
            return client;
        } catch (Exception e) {
            throw new ClientException(e.toString(),e);
        }

    }

}
