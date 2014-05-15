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

package com.cisco.vss.foundtion.threading.load.test;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.*;
import com.cisco.oss.foundation.http.jetty.JettyHttpClientFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class HttpTestClient {

	int numofMessages=ConfigurationFactory.getConfiguration().getInt("numberOfMessages");
	int sleepTime = ConfigurationFactory.getConfiguration().getInt("sleepTime");
	public CountDownLatch countDownLatch = new CountDownLatch(numofMessages + 1);
	HttpClient httpClient = JettyHttpClientFactory.createHttpClient("service.loadTestClient");
	
	@Test
	@Ignore
	public void testClient() throws Exception{
		sendMessage(0);
		
		Thread.sleep(sleepTime);
		
		long testDuration = ConfigurationFactory.getConfiguration().getLong("testDuration", 5*600);
		long finishTime = System.currentTimeMillis() + (testDuration);
		Random random = new Random();
		for (int i=0; i< numofMessages; i++){
		    //contentExchange.addRequestHeader("id", String.valueOf(random.nextInt(500)));
			sendMessage(i);
		}
		
		countDownLatch.await();
	}
	
	public void sendMessage(int counter) throws Exception{

	 
	    //when using the HttpClient we provide you should not supply the full UTL, only the URI.
	    //the infrastructure will add the host port info for you internally using a dynamic proxy.
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri("/")
                .entity("\"Hello World\"")
                .httpMethod(HttpMethod.POST)
                .header("id", String.valueOf(counter))
                .build();

        httpClient.executeWithLoadBalancer(httpRequest, new ResponseCallback() {
            @Override
            public void completed(HttpResponse response) {
                countDownLatch.countDown();
            }

            @Override
            public void failed(Throwable e) {
                countDownLatch.countDown();
            }

            @Override
            public void cancelled() {

            }
        });


    }
}
