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

import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.cisco.vss.foundation.queue.management.*;
import com.cisco.vss.foundtion.threading.test.MessageProviderT;
import com.google.common.collect.ArrayListMultimap;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.*;
import java.io.IOException;

public class JettyIdentiferServer {

	public static Long startTime;
	
	@Test
	@Ignore
	public void testJettyIdentifierServer() throws Exception{
		MessageProcessor messageProcessor = new MessageProcessorT(new SimpleIdentifier());
		final MessageProviderT messageProvider = new MessageProviderT(messageProcessor);

		Servlet servlet = new Servlet() {

			@Override
			public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
				if (startTime == null){
					startTime = System.currentTimeMillis();
					System.out.println("Started!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}
				
				messageProvider.pushMessage(req);
				//System.out.println("got request");
			}

			@Override
			public void init(ServletConfig config) throws ServletException {
				// TODO Auto-generated method stub

			}

			@Override
			public String getServletInfo() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ServletConfig getServletConfig() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void destroy() {
				// TODO Auto-generated method stub

			}
		};
		ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create();
		servletMap.put("/", servlet);
		JettyHttpServerFactory.INSTANCE.startHttpServer("loadTestServer", servletMap);
		
		MessageProcessorT.countDownLatch.await();
		long endTime = System.currentTimeMillis();
		System.out.println("The test took: " + (endTime - startTime) + " millis, tps=" + (100000/(endTime-startTime)));
		/*
		Field serversField = HighAvailabilityServerFactory.class.getDeclaredField("servers");
		serversField.setAccessible(true);
		Map<String, Server> servers = (Map<String, Server>)serversField.get(null);
		Server server = servers.get("loadTestServer");
		server.join();*/
	}
}
