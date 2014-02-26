package com.cisco.oss.foundation.http.apache.test;

import com.cisco.oss.foundation.http.*;
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory;
import com.cisco.oss.foundation.http.apache.ApacheHttpResponse;
import com.cisco.oss.foundation.loadbalancer.LoadBalancerStrategy;
import com.cisco.oss.foundation.loadbalancer.NoActiveServersException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Yair Ogen on 1/21/14.
 */
public class JavaClientSample {

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
        HttpClient clientTest = ApacheHttpClientFactory.createHttpClient("clientTest", LoadBalancerStrategy.STRATEGY_TYPE.FAIL_OVER);
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
