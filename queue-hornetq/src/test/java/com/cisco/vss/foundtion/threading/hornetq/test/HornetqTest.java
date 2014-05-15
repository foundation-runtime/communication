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

package com.cisco.vss.foundtion.threading.hornetq.test;

import com.cisco.oss.foundation.test.util.ConfigurationForTest;
import com.cisco.vss.foundation.queue.Consumer;
import com.cisco.vss.foundation.queue.Producer;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;

//@Ignore
public class HornetqTest {

	@Test
	public void testSimpleMessages(){
		HornetqMessageContainer.clearTestMessageList();
		ApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/testCabThreadingMessageContext.xml");		
		Consumer consumer = (Consumer)context.getBean("myListenerContainer");
		
		Producer producer = context.getBean(Producer.class);
		int threadNumber = 5;
		try {
			for (int i = 0; i < threadNumber; i++) {
				producer.sendToTopic("devExampleTopic", "myMessage " + i);
			}
		} catch (Throwable t){
			System.out.println("Catch exception: " + t.getMessage());
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		consumer.stopListener();
		
		
		List<MessageT> testMessageList = HornetqMessageContainer.getTestMessageList();
		Set<Long> threadNumberSet = new HashSet<Long>();
		for (MessageT message : testMessageList){
			boolean added = threadNumberSet.add(message.threadId);
			if (!added){
				Assert.fail("One thread treat more than one request");
			}
		}
	}
	
	@Test
	public void testDefaultIdentifierMessages(){
		HornetqMessageContainer.clearTestMessageList();
		ApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/testCabThreadingMessageContext.xml");		
		Consumer consumer = (Consumer)context.getBean("myListenerContainer");
		
		Map<String, Object> properties = new HashMap<String, Object>();
		Producer producer = context.getBean(Producer.class);
		int threadNumber = 5;
		try {
			for (int i = 0; i < threadNumber; i++) {
				properties.put("hh", i);
				producer.sendToTopic("devExampleTopic", "myMessage " + i, properties);
				producer.sendToTopic("devExampleTopic", "myMessage " + i, properties);
			}
		} catch (Throwable t){
			System.out.println("Catch exception: " + t.getMessage());
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		consumer.stopListener();
		
		
		List<MessageT> testMessageList = HornetqMessageContainer.getTestMessageList();
		Map<Integer, List<MessageT>> testMap = new HashMap<Integer, List<MessageT>>();
		for (MessageT message : testMessageList){
			if (! testMap.containsKey(message.identifier)){
				testMap.put(message.identifier, new ArrayList<MessageT>());
			}
			testMap.get(message.identifier).add(message);
		}
		for (Integer identifier : testMap.keySet()){
			List<MessageT> identifierMessages = testMap.get(identifier);
			long firstEndTime = identifierMessages.get(0).endTime;
			long secondStartTime = identifierMessages.get(1).startTime;
			Assert.assertTrue(firstEndTime < secondStartTime);
		}
	}
	
	@Test
	public void testNonDefaultIdentifierMessages(){
		HornetqMessageContainer.clearTestMessageList();
		ConfigurationForTest.setTestConfigFile("myTestConfig.properties");
		ApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/testCabThreadingMessageContext.xml");		
		Consumer consumer = (Consumer)context.getBean("myListenerContainer");
		
		Map<String, Object> properties = new HashMap<String, Object>();
		Producer producer = context.getBean(Producer.class);
		int threadNumber = 5;
		try {
			for (int i = 0; i < threadNumber; i++) {
				properties.put("notDefaultIdentifier", i);
				producer.sendToTopic("devExampleTopic", "myMessage " + i, properties);
				producer.sendToTopic("devExampleTopic", "myMessage " + i, properties);
			}
		} catch (Throwable t){
			System.out.println("Catch exception: " + t.getMessage());
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		consumer.stopListener();
		
		
		List<MessageT> testMessageList = HornetqMessageContainer.getTestMessageList();
		Map<Integer, List<MessageT>> testMap = new HashMap<Integer, List<MessageT>>();
		for (MessageT message : testMessageList){
			if (! testMap.containsKey(message.identifier)){
				testMap.put(message.identifier, new ArrayList<MessageT>());
			}
			testMap.get(message.identifier).add(message);
		}
		for (Integer identifier : testMap.keySet()){
			List<MessageT> identifierMessages = testMap.get(identifier);
			long firstEndTime = identifierMessages.get(0).endTime;
			long secondStartTime = identifierMessages.get(1).startTime;
			Assert.assertTrue(firstEndTime < secondStartTime);
		}
	}
}
