package com.cisco.vss.foundation.http.apache;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpClient;
import com.cisco.vss.foundation.http.HttpRequest;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;
import org.apache.commons.configuration.Configuration;

/**
 * Created by Yair Ogen on 1/19/14.
 */
public class ApacheHttpClientFactory {

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName){
        return createHttpClient(apiName, ConfigurationFactory.getConfiguration());
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, Configuration configuration){
        return createHttpClient(apiName, HighAvailabilityStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration);
    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType, Configuration configuration){
        try {
            HttpClient client = null;
            if(highAvailabilityStrategyType == null){
                client = new ApacheHttpClient(apiName, configuration);
            }else{
                client = new ApacheHttpClient(apiName, highAvailabilityStrategyType, configuration);
            }
            return client;
        } catch (Exception e) {
            throw new ClientException(e.toString(),e);
        }

    }

    public static HttpClient<HttpRequest,ApacheHttpResponse> createHttpClient(String apiName, HighAvailabilityStrategy.STRATEGY_TYPE highAvailabilityStrategyType){
        return createHttpClient(apiName, highAvailabilityStrategyType, ConfigurationFactory.getConfiguration());
    }
}
