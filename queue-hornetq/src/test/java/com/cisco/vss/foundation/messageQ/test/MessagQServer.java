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

import com.nds.cab.monitor.mamaclient.MAMAClient;
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
import java.util.concurrent.*;


public class MessagQServer {
	
	private static ApplicationContext context;
	
/*	public static void main(String[] args) throws InterruptedException {
		// Send message
		context = new ClassPathXmlApplicationContext("/META-INF/cabMessageQueueContext.xml");
		DefaultMessageListenerContainer listenerContainer = context.getBean("jmsContainerOne", DefaultMessageListenerContainer.class);
		System.out.println("started hornetQ consumer listener");
		try {
			JmsTemplate jmsTemplate = context.getBean("jmsTemplate",JmsTemplate.class);
			jmsTemplate.send(new HornetQTopic("/hep/purchaseTransactions"), new MessageCreator() {
	             @Override
	             public Message createMessage(Session session) throws JMSException {
	                    TextMessage message = session.createTextMessage("hello yo yo yo");
	                    message.setStringProperty("myHeader", "456");
	                    return message;
	             }
	        });
			System.out.println("Sent message to hornetQ server");
			
			// Get message
			Thread.sleep(10000);
		} finally {
			listenerContainer.shutdown();
		}
	}*/
	
	public static void main(String[] atgs) throws Exception{
		MAMAClient mamaClient = new MAMAClient("ch-perf-dt3", 27034);

		int sleep = 20000;
		int counter = 0;
		while (true){
			// Start hornetQ server
			mamaClient.Execute("su - root -c \"cd /opt/hornetq/hornetq/bin; ./run.sh&\"", 10);
			System.out.println("started hornetQ server");
			Thread.sleep(sleep);
			
			// Send message
			context = new ClassPathXmlApplicationContext("/META-INF/cabMessageQueueContext.xml");
			DefaultMessageListenerContainer listenerContainer = context.getBean("jmsContainerOne", DefaultMessageListenerContainer.class);
			System.out.println("started hornetQ consumer listener");
			try {
				
				ExecutorService service = Executors.newSingleThreadExecutor();
				Future<Boolean> result = service.submit(new SendMessage());
				result.get(10, TimeUnit.SECONDS);
				
				System.out.println("Sent message to hornetQ server");
				
				// Get message
				Thread.sleep(10000);
			} finally {
				listenerContainer.shutdown();
			}
			
			// Stop hornetQ server
			mamaClient.Execute("su - root -c \"cd /opt/hornetq/hornetq/bin; ./stop.sh\"", 10);
			System.out.println("Stoped hornetQ server");
			Thread.sleep(sleep);
			System.out.println("Finished test number: " + counter++);
		}
	}
	
	public static class SendMessage implements Callable<Boolean> {

		public SendMessage(){}
		
		@Override
		public Boolean call() throws Exception {
			JmsTemplate jmsTemplate = context.getBean("jmsTemplate",JmsTemplate.class);
			jmsTemplate.send(new HornetQTopic("/hep/purchaseTransactions"), new MessageCreator() {
	             @Override
	             public Message createMessage(Session session) throws JMSException {
	                    TextMessage message = session.createTextMessage("hello yo yo yo");
	                    message.setStringProperty("myHeader", "456");
	                    return message;
	             }
	        });
			return true;
		}
		
	}
}
