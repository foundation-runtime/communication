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

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestExecutor {

	/**
	 * This test ensure Executor works well and create and instantiate ONLY X thread, while X is the maximum pool size
	 * Currently the test should be run on debug mode with breakpoint where the Executor create and instantiate new Worker thread.
	 * TODO: With AOP there is ability to count automatically how many times the 'new Worker()' code is happen. 
	 */
	@Test
	public void testExecutoWithOneThread(){
		
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		int index = 0;
		for (; index < 4; index ++)
			executor.execute(new EventListener());
		
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class EventListener implements Runnable {

		@Override
		public void run() {
			
		}
		
	}

}
