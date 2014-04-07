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

package com.cisco.oss.foundation.http.apache.test;

import com.cisco.oss.foundation.http.*;
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory;
import com.cisco.oss.foundation.http.apache.ApacheHttpResponse;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.cisco.oss.foundation.loadbalancer.NoActiveServersException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Yair Ogen on 1/21/14.
 */
@Ignore
public class JavaClientSample {

    static PropertiesConfiguration propsConfiguration = null;

    @BeforeClass
    public static void init(){
        try {
            propsConfiguration = new PropertiesConfiguration(TestApacheClient.class.getResource("/config.properties"));
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test (expected = NoActiveServersException.class)
    public void testClient(){
        HttpClient clientTest = ApacheHttpClientFactory.createHttpClient("muku");

        HttpRequest request = HttpRequest.newBuilder()
                .httpMethod(HttpMethod.GET)
                .contentType("text/html")
                .uri("http://www.google.com")
                .header("","")
                .header("","")
                .header("","")
                .queryParams("","")
                .lbKey("")
                //.uri(new URI(null))
                .build();

        HttpResponse response = clientTest.executeWithLoadBalancer(request);

        System.out.println(response.getResponseAsString());
    }

    @Test
    public void testClient2() throws IOException {
        HttpClient clientTest = ApacheHttpClientFactory.createHttpClient("clientTest", LoadBalancerStrategy.STRATEGY_TYPE.FAIL_OVER, propsConfiguration);
        HttpRequest request = HttpRequest.newBuilder()
                .httpMethod(HttpMethod.GET)
                .uri("http://www.google.com")
                .build();
        HttpResponse response = clientTest.execute(request);
        String responseAsString = response.getResponseAsString();
        Assert.assertTrue(responseAsString.contains("<!doctype html>"));
        System.out.println(responseAsString);


        clientTest.executeWithLoadBalancer(request, new MyResponseHandler());

    }

    private static class MyResponseHandler implements ResponseCallback<ApacheHttpResponse>{
        @Override
        public void completed(ApacheHttpResponse response) {
            String responseAsString = response.getResponseAsString();
        }

        @Override
        public void failed(Throwable e) {

        }

        @Override
        public void cancelled() {

        }
    }
}
