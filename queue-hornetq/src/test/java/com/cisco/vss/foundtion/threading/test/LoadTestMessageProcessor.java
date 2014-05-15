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

package com.cisco.vss.foundtion.threading.test;


import com.cisco.vss.foundation.queue.management.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestMessageProcessor extends AbstractMessageProcessor {

	public static AtomicInteger numberOfRequests = new AtomicInteger(0);
	
	public LoadTestMessageProcessor(MessageIdentifier messageIdentifier) {
		super(messageIdentifier);
	}

	@Override
	public void process(Object message) {
		if (message instanceof MessageT){
			((MessageT)message).threadId = Thread.currentThread().getId();
			((MessageT)message).startTime = System.nanoTime();
		}
		
		try {			
			File threadFile = new File("./" + Thread.currentThread().getId() + ".txt");
			threadFile.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(threadFile);
			byte[] text = "this a a load test".getBytes();
			fileOutputStream.write(text);
			
			threadFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (message instanceof MessageT){
			((MessageT)message).endTime = System.nanoTime();
		}
		
		numberOfRequests.incrementAndGet();
	}

}
