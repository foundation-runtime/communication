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
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * filter that catches all errors and prevents stack trace or other sensitive data from leaking to the client
 */
public class ErrorHandlingFilter extends AbstractInfraHttpFilter {

	private final static Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingFilter.class);

	public ErrorHandlingFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

		try {
			chain.doFilter(request, response);
		} catch (EofException e) {
			LOGGER.error("EOF Exception caught. Probably client closed connection before the response was sent.");
			// don't send response. no one is waiting for it on the other side
		} catch (Throwable e) {
			LOGGER.error("error serving request: " + e, e);
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		}

	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.errorHandlingFilter.isEnabled";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
