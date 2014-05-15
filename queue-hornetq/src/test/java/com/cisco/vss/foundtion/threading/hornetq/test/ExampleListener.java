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


import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.queue.management.JMSMessageIdentifier;
import com.cisco.vss.foundation.queue.management.MessageIdentifier;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class ExampleListener extends MessageProcessorT {

	public ExampleListener() {
		this(new JMSMessageIdentifier());
	}
	
	public ExampleListener(MessageIdentifier messageIdentifier) {
        super(messageIdentifier);
        Configuration configuration = ConfigurationFactory.getConfiguration();
        identifierProperty = configuration.getString(JMSMessageIdentifier.MESSAGE_IDENTIFIER_PROPERTY);
	}

	private int counter = 0;
	private String identifierProperty;
	
	public int getCounter() {
		return counter;
	}

	public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            try {
            	counter++;
                System.out.println("[Exmaple] - The message is: '" + ((TextMessage) message).getText() + "'");
            }
            catch (JMSException ex) {
            	System.out.println("Got some message");
                throw new RuntimeException(ex);
            }
        }
        else {
        	System.out.println("Got some message");
            throw new IllegalArgumentException("Message must be of type ObjectMessage");
        }
    }

	@Override
	public void process(Object message) {
		// TODO: throw exception if message is not instance of Message

		MessageT testMessage = new MessageT();
		try {
			String identifier = ((Message) message).getStringProperty(identifierProperty);
			if (StringUtils.isNotEmpty(identifier)){
				testMessage.identifier = Integer.parseInt(identifier);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.process(testMessage);
		HornetqMessageContainer.addTestMessage(testMessage);
		onMessage((Message) message);
	}
}
