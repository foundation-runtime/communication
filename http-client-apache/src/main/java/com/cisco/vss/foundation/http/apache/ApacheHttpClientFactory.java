package com.cisco.vss.foundation.http.apache;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpClient;
import com.cisco.vss.foundation.http.HttpRequest;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;

/**
 * Created by Yair Ogen on 1/19/14.
 */
public class ApacheHttpClientFactory {

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName){
        return createHttpClient(apiName, null);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        ConfigurationFactory.getConfiguration();
        try {
            HttpClient client = null;
            if(highAvailabilityStrategyType == null){
                client = new ApacheHttpClient(apiName);
            }else{
                client = new ApacheHttpClient(apiName, highAvailabilityStrategyType);
            }
            return client;
        } catch (Exception e) {
            throw new ClientException(e.toString(),e);
        }
    }
}
