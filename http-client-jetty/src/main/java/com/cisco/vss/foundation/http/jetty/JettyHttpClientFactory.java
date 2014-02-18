package com.cisco.vss.foundation.http.jetty;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpClient;
import com.cisco.vss.foundation.http.HttpRequest;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;
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
        return createHttpClient(apiName, HighAvailabilityStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration, enableLoadBalancing);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, Configuration configuration){
        return createHttpClient(apiName, configuration, true);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        return createHttpClient(apiName, highAvailabilityStrategyType, ConfigurationFactory.getConfiguration());
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration){
        return createHttpClient(apiName, highAvailabilityStrategyType, configuration, true);
    }


    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration, boolean enableLoadBalancing){
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
