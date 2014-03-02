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

package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.AbstractInfraHttpFilter;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * implementation of CORS - http://en.wikipedia.org/wiki/Cross-origin_resource_sharing
 */
public class CrossOriginFilter extends AbstractInfraHttpFilter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CrossOriginFilter.class);
	
	private org.eclipse.jetty.servlets.CrossOriginFilter crossOriginFilter = new org.eclipse.jetty.servlets.CrossOriginFilter();
	
	private String serviceName;
	
	public CrossOriginFilter(String serviceName){
		super(serviceName);
		try {
			crossOriginFilter.init(new CrossOriginFilterConfig(serviceName));
		} catch (ServletException e) {
			LOGGER.error("can't init crosee origin filter", e);
		}
	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.crossOriginFilter.isEnabled";
	}

	@Override
	protected void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		crossOriginFilter.doFilter(request, response, chain);
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return false;
	}
	
	private static class CrossOriginFilterConfig implements FilterConfig{
		
		private String serviceName;
		
		

		public CrossOriginFilterConfig(String serviceName) {
			this.serviceName = serviceName;
		}

		@Override
		public String getFilterName() {
			return "CrossOriginFilter";
		}

		@Override
		public ServletContext getServletContext() {
			return null;
		}

		@Override
		public String getInitParameter(String name) {
			
			Configuration configuration = ConfigurationFactory.getConfiguration();
			
			String prefix = serviceName + ".http.crossOriginFilter.";
			
			if(org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_ORIGINS_PARAM.equals(name)){
				String paramName = prefix + "allowedOrigins";
				return getArrayAsString(configuration, paramName);			
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_METHODS_PARAM.equals(name)){
				String paramName = prefix + "allowedMethods";
				return getArrayAsString(configuration, paramName);
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_HEADERS_PARAM.equals(name)){				
				String paramName = prefix + "allowedHeaders";
				return getArrayAsString(configuration, paramName);				
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM.equals(name)){
				return configuration.getString(prefix+"preflightMaxAge","1800");
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.ALLOW_CREDENTIALS_PARAM.equals(name)){
				return configuration.getString(prefix+"allowCredentials","true");
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.EXPOSED_HEADERS_PARAM.equals(name)){
				String paramName = prefix + "exposedHeaders";
				return getArrayAsString(configuration, paramName);
			}else if(org.eclipse.jetty.servlets.CrossOriginFilter.CHAIN_PREFLIGHT_PARAM.equals(name)){
				return configuration.getString(prefix+"chainPreflight","false");
			}
			
			return null;
		}

		private String getArrayAsString(Configuration configuration, String paramName) {
			
			List<String> values = Lists.newArrayList();
			Configuration subset = configuration.subset(paramName);
			@SuppressWarnings("unchecked")
			Iterator<String> keys = subset.getKeys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				values.add(subset.getString(key));
			}
			
			Joiner joiner = Joiner.on(',').skipNulls();
			String result = joiner.join(values);
			
			if(Strings.isNullOrEmpty(result)){
				result = null;
			}
			
			return result;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
