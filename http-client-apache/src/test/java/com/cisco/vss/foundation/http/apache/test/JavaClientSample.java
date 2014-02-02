package com.cisco.vss.foundation.http.apache.test;

import com.cisco.vss.foundation.http.*;
import com.cisco.vss.foundation.http.apache.ApacheHttpClientFactory;
import com.cisco.vss.foundation.loadbalancer.FailOverStrategy;
import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;
import com.cisco.vss.foundation.loadbalancer.NoActiveServersException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

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
        HttpClient clientTest = ApacheHttpClientFactory.createHttpClient("clientTest", HighAvailabilityStrategy.STRATEGY_TYPE.FAIL_OVER);
        HttpRequest request = HttpRequest.newBuilder()
                .httpMethod(HttpMethod.GET)
                .uri("http://www.google.com")
                .build();
        HttpResponse response = clientTest.execute(request);
        String responseAsString = response.getResponseAsString();
        Assert.assertTrue(responseAsString.contains("<!doctype html>"));
        System.out.println(responseAsString);
    }
}
