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

package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.http.server.AbstractInfraHttpFilter;
import org.slf4j.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter supposed to be the first filter on any chain of filters/servlets
 * that processing HTTP request. It replies to PING request or doing nothing and
 * forwarding this request to the next filter. As it is the first filter it is
 * also can print request and response content for debugging purposes.
 * 
 * 
 * @author Dima Mirkin
 * @author Yair Ogen
 */
public class TraceFilter extends AbstractInfraHttpFilter {

	private int bodyLimit = 1024;
	private final static Logger LOGGER = LoggerFactory.getLogger(TraceFilter.class);

	public TraceFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		bodyLimit = Integer.parseInt(getConfigValue(serviceName+".http.traceFilter.maxBodyLength", bodyLimit+""));
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
			LOGGER.trace("Starting trace filter.");
			String originalClient = getOriginalClient(httpRequest);
			LOGGER.info("Received HTTP request from " + originalClient + ":" + request.getRemotePort());

			if (LOGGER.isDebugEnabled()) {
				final TraceResponseWrapper wrappedResponse = new TraceResponseWrapper((HttpServletResponse) response, bodyLimit, serviceName);
				final TraceRequestWrapper wrappedRequest = new TraceRequestWrapper(httpRequest, bodyLimit, serviceName);

				//RESOLUTION OF: CQ 1578469 - SGW Logs contain ^M characters
				String escaped = wrappedRequest.toStringWithBody().replaceAll("\\r\\n", System.getProperty("line.separator"));
				LOGGER.debug("Request:\n" + escaped);
				
				chain.doFilter(wrappedRequest, wrappedResponse);
				
				//RESOLUTION OF: CQ 1578469 - SGW Logs contain ^M characters
				escaped = wrappedResponse.toStringWithBody().replaceAll("\\r\\n", System.getProperty("line.separator"));
				LOGGER.debug("Response:\n" + escaped);
				wrappedResponse.writeBody();
			}else{
				chain.doFilter(request, response);
			}

			
			LOGGER.info("Sending HTTP response to " + originalClient + ":" + request.getRemotePort());

			

			
	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.traceFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return false;
	}

}
