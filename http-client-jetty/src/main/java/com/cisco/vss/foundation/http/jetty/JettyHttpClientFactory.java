package com.cisco.vss.foundation.http.jetty;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpClient;
import com.cisco.vss.foundation.http.HttpRequest;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;

/**
 * Created by Yair Ogen on 1/19/14.
 */
public class JettyHttpClientFactory {

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName){
        return createHttpClient(apiName, null);
    }

    public static HttpClient<HttpRequest,JettyHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        ConfigurationFactory.getConfiguration();
        try {
            HttpClient client = null;
            if(highAvailabilityStrategyType == null){
                client = new JettyHttpClient(apiName);
            }else{
                client = new JettyHttpClient(apiName, highAvailabilityStrategyType);
            }
            return client;
        } catch (Exception e) {
            throw new ClientException(e.toString(),e);
        }
    }
}
