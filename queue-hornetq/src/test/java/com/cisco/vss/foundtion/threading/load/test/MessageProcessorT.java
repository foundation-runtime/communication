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
import com.cisco.vss.foundation.queue.management.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MessageProcessorT extends AbstractMessageProcessor {

	public static int counter;
	public static int numOfMessages = ConfigurationFactory.getConfiguration().getInt("numberOfMessages");
	public static CountDownLatch countDownLatch = new CountDownLatch(numOfMessages);
	
	public MessageProcessorT(MessageIdentifier messageIdentifier) {
		super(messageIdentifier);
	}

	@Override
	public void process(Object message) {		
		// Do some busy CPU
		List<Integer> sortList = new ArrayList<Integer>(10000);
		Random random = new Random();
		for (int i=0; i<10000; i++){
			sortList.add(random.nextInt(10000));
		}
		Collections.sort(sortList);
		//System.out.println("got request");
		countDownLatch.countDown();
		//System.out.println(countDownLatch.getCount());
	}

}
