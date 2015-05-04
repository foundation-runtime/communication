/*
 * Copyright 2015 Cisco Systems, Inc.
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

package com.cisco.oss.foundation.core.multithread.test;

import com.cisco.oss.foundation.message.AbstractHornetQConcurrentMessageHandler;
import com.cisco.oss.foundation.message.MessageIdentifier;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestDefaultProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDefaultProcessor.class);

	@Test
	public void testSimpleMessages(){

        AbstractHornetQConcurrentMessageHandler messageProcessor = new MessageProcessorT(null);
//		MessageProvider provider = new MessageProviderT(messageProcessor);
		
		int threadNumber = 45;
		List<MessageT> testMessageList = new ArrayList<MessageT>();
		for (int i=0; i < threadNumber ; i++){
			MessageT message = new MessageT();
			testMessageList.add(message);
//			provider.pushMessage(message);
            messageProcessor.onMessage(message);
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Set<Long> threadNumberSet = new HashSet<Long>();
		for (MessageT message : testMessageList){
			boolean added = threadNumberSet.add(message.threadId);
			if (!added){
				Assert.fail("One thread treat more than one request");
			}
		}
	}
	
	@Test
	public void testIdentifiedMessages(){
		
		MessageIdentifier identifier = new MessageIdentifierT();
        AbstractHornetQConcurrentMessageHandler messageProcessor = new MessageProcessorT(identifier);
//		MessageProvider provider = new MessageProviderT(messageProcessor);
		
		int threadNumber = 5;
		List<MessageT> testMessageList = new ArrayList<MessageT>();
		for (int i=0; i < threadNumber ; i++){
			MessageT message = new MessageT();
			message.identifier = i;
			testMessageList.add(message);
            messageProcessor.onMessage(message);
			message = new MessageT();
			message.identifier = i;
			testMessageList.add(message);
            messageProcessor.onMessage(message);
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < threadNumber; i++) {
			long firstEndTime = testMessageList.get(i).endTime;
			long secondStartTime = testMessageList.get(++i).startTime;
            boolean condition = firstEndTime < secondStartTime;
            if(!condition){
                LOGGER.error("thread: {} did not meet the condition", i);
            }
            Assert.assertTrue(condition);
		}
	}

}
