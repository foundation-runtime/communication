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

package com.cisco.oss.foundation.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter replies to PING request if request contains
 * <code>NDS-Proxy-Ping</code> header.
 * 
 * @author Dima Mirkin
 * @author Yair Ogen
 */
public class PingFilter extends AbstractInfraHttpFilter {

	private static final String PING_HEADER = "x-Ping";
	private final static Logger LOGGER = LoggerFactory.getLogger(PingFilter.class);
	
	public PingFilter(String serviceName) {
		super(serviceName);
	}


	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        boolean enableLogging = Boolean.valueOf(getConfigValue(serviceName + "http.pingFilter.enableLogging","false"));
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		String pingHeader = getConfigValue(serviceName + "http.proxyPingFilterHeader", PING_HEADER);
		if (null == httpRequest.getHeader(pingHeader)) {
			// if not ping request - do nothing, just forward through the chain
			chain.doFilter(request, response);
		} else {
            if (enableLogging) {
                LOGGER.debug("HTTP Ping received from " + getOriginalClient(httpRequest));
            }
            ((HttpServletResponse) response).setDateHeader("Date", System.currentTimeMillis());
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK);
		}
	}
	
	@Override
	protected String getKillSwitchFlag() {
		return "http.pingFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
