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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that filters too long requests. Default size is 100000 (100K). You can
 * override the default by using "http.maxContentLength" parameter in
 * your config file.
 * 
 * @author Yair Ogen
 * 
 */
public class RequestValidityFilter extends AbstractInfraHttpFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestValidityFilter.class);

	public RequestValidityFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//		final int contentLimit = ConfigurationFactory.getConfiguration().getInt(serviceName + ".http.requestValidityFilter.maxContentLength", 100000);
		final int contentLimit = Integer.parseInt(getConfigValue(serviceName + ".http.requestValidityFilter.maxContentLength", "100000"));
		final int contentLength = request.getContentLength();
		if (contentLength < -1 || contentLength > contentLimit) {
			LOGGER.error("The content length of the request, {}, is larger than the max allowed - {}", contentLength, contentLimit);
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

//		try {
			chain.doFilter(request, response);
//		} catch (Throwable e) {
//			LOGGER.error("error processing request. " + e, e);
//			if (response instanceof HttpServletResponse) {
//				HttpServletResponse resp = (HttpServletResponse) response;
//				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
//			}
//		}
	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.requestValidityFilter.isEnabled";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
