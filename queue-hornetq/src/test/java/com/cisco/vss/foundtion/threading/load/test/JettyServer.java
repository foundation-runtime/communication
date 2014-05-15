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
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.google.common.collect.ArrayListMultimap;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class JettyServer {
	
	public class MyServlet implements Servlet {
		public final int numOfMessages = ConfigurationFactory.getConfiguration().getInt("numberOfMessages");
		public final CountDownLatch countDownLatch = new CountDownLatch(numOfMessages);
		
		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException {
			
			if (firstMessage.compareAndSet(false, true)){
				return;
			}
			
			if (startTime == null){
				startTime = System.currentTimeMillis();
			}

			// Do some busy CPU
			List<Integer> sortList = new ArrayList<Integer>(10000);
			Random random = new Random();
			for (int i=0; i<10000; i++){
				sortList.add(random.nextInt(10000));
			}
			Collections.sort(sortList);

			countDownLatch.countDown();
			//System.out.println(countDownLatch.getCount());
		}

		@Override
		public void init(ServletConfig config) throws ServletException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ServletConfig getServletConfig() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getServletInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			
		}
	}

	

	public AtomicBoolean firstMessage = new AtomicBoolean(false);
	public Long startTime;
	
	@After
	public void after(){
		System.out.println(counter);
	}
	
	private static AtomicInteger counter = new AtomicInteger(0); 
	@Test
	@Ignore
	public void testServer() throws IOException, IllegalArgumentException, IllegalAccessException, InterruptedException, SecurityException, NoSuchFieldException{

		MyServlet servlet = new MyServlet();
		ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create();
		servletMap.put("/", servlet);
		JettyHttpServerFactory.INSTANCE.startHttpServer("loadTestServer", servletMap);

		servlet.countDownLatch.await();
		long endTime = System.currentTimeMillis();
		System.out.println("The test took: " + (endTime - startTime) + " millis, tps=" + (servlet.numOfMessages/(endTime-startTime)));
		
		/*Field serversField = HighAvailabilityServerFactory.class.getDeclaredField("servers");
		serversField.setAccessible(true);
		Map<String, Server> servers = (Map<String, Server>)serversField.get(null);
		Server server = servers.get("loadTestServer");
		server.join();*/
	}
}
