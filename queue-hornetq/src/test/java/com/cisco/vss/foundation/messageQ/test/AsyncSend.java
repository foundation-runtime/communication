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

import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.SendAcknowledgementHandler;
import org.hornetq.jms.client.HornetQSession;
import org.hornetq.jms.client.HornetQTopic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.*;

public class AsyncSend {

	static ApplicationContext context;
	static JmsTemplate jmsTemplate;

	public static void main(String[] args) throws JMSException, InterruptedException{
		Connection connection = null;
		try {
			context = new ClassPathXmlApplicationContext("/META-INF/blockQueueContext.xml");

			jmsTemplate = context.getBean("jmsTemplate",JmsTemplate.class);
			int counter = 0;
			ConnectionFactory cf = jmsTemplate.getConnectionFactory();
			connection = cf.createConnection();
			
			// Step 6. Create a JMS Session
	         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

	         // Step 7. Set the handler on the underlying core session

	         ClientSession coreSession = ((HornetQSession)session).getCoreSession();

	        coreSession.setSendAcknowledgementHandler(new MySendAcknowledgementsHandler());

	         // Step 6. Create a JMS Message Producer
	         MessageProducer producer = session.createProducer(new HornetQTopic("NewTopic"));

	        //producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

	         // Step 7. Send 5000 messages, the handler will get called asynchronously some time later after the messages
	         // are sent.

	         final int numMessages = 500000;

	         int i=0;
	         while(true)
	         {
	            javax.jms.Message jmsMessage = session.createMessage();
	            jmsMessage.setStringProperty("myHeader", "456");

	            producer.send(jmsMessage);

	            System.out.println("Sent message " + i++);
	         }

/*			
			
			
			
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			ClientSession coreSession = ((HornetQSession)session).getCoreSession();

			coreSession.setSendAcknowledgementHandler(new MySendAcknowledgementsHandler());
			
			for (int i=0 ; i < 100 ; i++){
				TextMessage message = session.createTextMessage("TEST!" + counter++);
				message.setStringProperty("myHeader", "456");
				MessageProducer producer = coreSession.createProducer(new HornetQTopic("NewTopic"));
				producer.send(message);
				System.out.println("Send message");
			}

*/			//Thread.sleep(70000);
		} finally {
			connection.close();
		}
	}

	private static class MySendAcknowledgementsHandler implements SendAcknowledgementHandler
	{
		int count = 0;

		@Override
		public void sendAcknowledged(org.hornetq.api.core.Message message) {
			System.out.println("Received send acknowledgement for message " + count++);
		}
	}


}
