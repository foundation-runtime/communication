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

import org.hornetq.jms.client.HornetQTopic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

public class BlockQ {

	static ApplicationContext context;
	public static int counter = 0;
	
	public static void main(String[] atgs) throws Exception{
		context = new ClassPathXmlApplicationContext("/META-INF/blockQueueContext.xml");
		//createSubscriber();
		JmsTemplate jmsTemplate = context.getBean("jmsTemplate",JmsTemplate.class);
		
		
		//long timeToLive = TimeUnit.HOURS.toMillis((long)1);
		
		System.out.println(System.currentTimeMillis() + " Start process");
		while (true){			
			// Send message			
			try {				
				//jmsTemplate.setTimeToLive(timeToLive); // default = 0
				jmsTemplate.send(new HornetQTopic("NewTopic"), new MessageCreator() {
		             @Override
		             public Message createMessage(Session session) throws JMSException {
		                    TextMessage message = session.createTextMessage("hello yo yo yo" + counter);
		                    
		                    //String myUniqueID = "This is my unique id 123";   // Could use a UUID for this
		                    //message.setStringProperty("_HQ_DUPL_ID", myUniqueID);
		                    message.setStringProperty("myHeader", "456");
		                    //message.setJMSExpiration(expiration)
		                    return message;
		             }
		        });
				jmsTemplate.getSessionAcknowledgeMode();
				System.out.println(System.currentTimeMillis() + " Finish send message: " + counter++);
			} finally {
			}
		}
		//createSubscriber();
		
	}
	
	public static void createSubscriber() throws InterruptedException{
		DefaultMessageListenerContainer listenerContainer = context.getBean("jmsContainerOne", DefaultMessageListenerContainer.class);
		Thread.sleep(70000);
		listenerContainer.shutdown();
	}
}
