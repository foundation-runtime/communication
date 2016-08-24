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

package com.cisco.oss.foundation.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter will return HTTP 503 - server busy if the currently used thread pool is low on threads
 * @author Yair Ogen
 *
 */
//@Component
//@Order(70)
public class AvailabilityFilter extends AbstractInfraHttpFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityFilter.class);

	private HttpThreadPool threadPool;

	public AvailabilityFilter(String serviceName, HttpThreadPool threadPool) {
		super(serviceName);
		this.threadPool = threadPool;
	}


	/* ------------------------------------------------------------ */
	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		if (!threadPool.isLowOnThreads()) {
			chain.doFilter(request, response);
		} else {
			LOGGER.warn("Low number of threads: returning HTTP 503 error code");
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
	}


	@Override
	protected String getKillSwitchFlag() {
		return "http.availabilityFilter.isEnabled";
	}


	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	
	
	


}
