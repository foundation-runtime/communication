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

import com.cisco.vss.foundation.queue.Consumer;
import com.cisco.vss.foundation.queue.ConsumerFactory;
import com.cisco.vss.foundation.queue.Producer;
import com.cisco.vss.foundation.queue.ProducerFactory;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestMasterSlaveRedundency {

	@Test
	public void testMaster() throws InterruptedException{
		//String topicName = "MasterSlaveTopic";
		String topicName = "UPM-COGECO";
		
		ExampleListener exampleListener = new ExampleListener();
		Consumer consumer = ConsumerFactory.createTopicDurableConsumer(topicName, exampleListener, "master", "slave");
		Producer producer = ProducerFactory.getProducer();
		
		// beginListener
		consumer.beginListener();
		consumer.beginListener();
		
		// Send message
		producer.sendToTopic(topicName, "test");
		
		
		// Get message
		Thread.sleep(100);
		Assert.assertEquals(1, exampleListener.counter);
		
		// stopListener
		consumer.stopListener();
		consumer.stopListener();
		
		// Send message
		producer.sendToTopic(topicName, "test");
		
		// Don't get message
		Thread.sleep(100);
		Assert.assertEquals(1, exampleListener.counter);
		
		// beginListener
		consumer.beginListener();
		
		// Get message
		Thread.sleep(100);
		Assert.assertEquals(2, exampleListener.counter);
	}
}
