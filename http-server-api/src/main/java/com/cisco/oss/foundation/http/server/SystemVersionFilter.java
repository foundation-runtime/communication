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

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.systemversion.SystemVersionFactory;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Thia filter will extract the flow context from a known header and create it if ti doesn't exist.
 */
@Component
@Order(11)
public class SystemVersionFilter extends AbstractInfraHttpFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemVersionFilter.class);

	public SystemVersionFilter(){
		super();
	}

	public SystemVersionFilter(String serviceName){
		super(serviceName);
	}


	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {


        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            String systemVersion = httpServletRequest.getHeader(SystemVersionFactory.SYSTEM_VERSION);
			SystemVersionFactory.setSystemVersion(systemVersion);

		} catch (Exception e) {
			LOGGER.warn("problem setting flow context filter: " + e, e);
		}

        ((HttpServletResponse)response).setHeader(SystemVersionFactory.SYSTEM_VERSION, SystemVersionFactory.getSystemVersion());
        chain.doFilter(request, response);


	}

	
	@Override
	protected String getKillSwitchFlag() {
		return "http.systemVersionFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	

}
