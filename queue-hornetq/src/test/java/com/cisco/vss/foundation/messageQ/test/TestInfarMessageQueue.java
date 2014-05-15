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

package com.cisco.vss.foundation.messageQ.test;

import com.cisco.vss.foundation.queue.Producer;
import com.cisco.vss.foundation.queue.ProducerFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class TestInfarMessageQueue {

	ApplicationContext context;

	@Test
	@Ignore
	public void testCodeInit() throws Exception{
		ProducerFactory.getProducer().sendToTopic("kuku", "dummy message");
	}

	/*@Test
	public void testServerStartAfterClient(){
		// Stop servers on ch-perf-dt1 and ch-perf-dt2
		
		//
	}*/
	
	//@Test
	@Ignore
	public void testBasicFeatures() throws Exception{

		//System.out.println(Long.parseLong("99999999999999999999999999999999999999999999999999999999999999999999999999999999"));
		
		//HighAvailabilityClientFactory.createRMIServiceInstance("test", STRATEGY_TYPE.ROUND_ROBIN, Consumer.class, "consumer");
		context = new ClassPathXmlApplicationContext("/META-INF/testCabMessageQueueContext.xml");

		context.getBean("myListenerContainer");


		//System.out.println("Start listener");

//		Thread.sleep(10000);

		Producer producer = context.getBean(Producer.class);

		System.out.println("start servers");
		System.in.read();
		
		
		int numMessages = 3;
		Map<String, Object> messageProp = new HashMap<String, Object>();
//		messageProp.put("pointer", "/households[householdId=?]");
		try {
			for (int i = 0; i < numMessages; i++) {
				producer.sendToTopic("myTopicExampleQueue", "myMessage " + i);
				System.out.println("Sent message: " + i);
			}
		} catch (Throwable t){
			System.out.println("Catch exception: " + t.toString());
		}

		System.out.println("stop the server by logging into jconsole");
		System.in.read();
		System.in.read();
		

		for (int i = 0; i < numMessages; i++) {
			producer.sendToTopic("myTopicExampleQueue", "myMessage " + i);
			System.out.println("Sent message: " + i);
		}
		
		Thread.sleep(3000);
	}
}
