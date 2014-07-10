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

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class HttpMethodFilter extends AbstractInfraHttpFilter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpMethodFilter.class);
	private final Set<String> methods = new HashSet<String>(5);
	
	public HttpMethodFilter(String serviceName){
		super(serviceName);
		methods.add("TRACE");
		updateAllowedMethodsFromConfig(serviceName);
	}

	
	private void updateAllowedMethodsFromConfig(String serviceName) {
		Configuration configuration = ConfigurationFactory.getConfiguration();
		Configuration subset = configuration.subset(serviceName + ".http.httpMethodFilter.methods");
		@SuppressWarnings("unchecked")
		Iterator<String> keys = subset.getKeys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			methods.add(subset.getString(key).toUpperCase(Locale.getDefault()));
		}

	}


	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		
		String method = httpServletRequest.getMethod();
		if(methods.contains(method)){
			LOGGER.error("method {} is not allowed", method);
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}else{			
			chain.doFilter(request, response);
		}
		
	}

	
	@Override
	protected String getKillSwitchFlag() {
		return "http.httpMethodFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	

}
