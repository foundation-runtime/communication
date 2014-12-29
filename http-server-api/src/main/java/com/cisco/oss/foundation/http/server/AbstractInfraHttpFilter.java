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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for infra http filters. supports a kill switch specific for each
 * filter.
 * 
 * @author Yair Ogen
 * 
 */
public abstract class AbstractInfraHttpFilter implements Filter {



	protected String serviceName = null;
	private String enabledKey = null;
	private static boolean filterConfigurationDynamicRefreshEnabled = ConfigurationFactory.getConfiguration().getBoolean("http.filterConfigurationDynamicRefreshEnabled", false);
	protected Map<String, String> filterConfigCache = new HashMap<String, String>();
	private Configuration configuration = ConfigurationFactory.getConfiguration();

	public AbstractInfraHttpFilter(String serviceName) {
		this.serviceName = serviceName;
		this.enabledKey = serviceName + "." + getKillSwitchFlag();
        String defaultValue = isEnabledByDefault() + "";
        boolean enabled = getConfigValue(enabledKey, Boolean.valueOf(defaultValue));

    }

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		Boolean firstRequestProcessing = (Boolean)request.getAttribute("firstRequestProcessing");

		if(firstRequestProcessing == null || (firstRequestProcessing != null && firstRequestProcessing)) {

//			String defaultValue = isEnabledByDefault() + "";
			boolean enabled = getConfigValue(enabledKey, isEnabledByDefault());
			if (enabled) {
				doFilterImpl(request, response, chain);
			} else {
				chain.doFilter(request, response);
			}
		}else{
			chain.doFilter(request, response);
		}
	}

    private boolean getConfigValue(String key, boolean defaultValue) {

        if (filterConfigurationDynamicRefreshEnabled) {
            return configuration.getBoolean(key, defaultValue);
        } else {

            if (!filterConfigCache.containsKey(key)) {
                filterConfigCache.put(key, configuration.getBoolean(key, defaultValue)+"");
            }

            return Boolean.valueOf(filterConfigCache.get(key));
        }
    }

	protected String getConfigValue(String key, String defaultValue) {

		if (filterConfigurationDynamicRefreshEnabled) {
			return configuration.getString(key, defaultValue);
		} else {

			if (!filterConfigCache.containsKey(key)) {
				filterConfigCache.put(key, configuration.getString(key, defaultValue));
			}

			return filterConfigCache.get(key);
		}
	}

	protected boolean isEnabledByDefault() {
		return false;
	}

	@Override
	public void destroy() {
	}

	/**
	 * concrete filters must supply their specific kill switch parameter name
	 * 
	 * @return
	 */
	protected abstract String getKillSwitchFlag();

	/**
	 * if filter is enabled, delegate the actual filter work to the concrete
	 * filter
	 */
	protected abstract void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException;

	/**
	 * Determine the original client. If there is an x-forwarded-for header,
	 * take the first host/ip from the list. Else, take the value from the
	 * Request object's remote host.
	 * 
	 * @param request
	 *            The HTTP Servlet request object being handled.
	 * 
	 * @return The original client host or IP value.
	 */
	public static String getOriginalClient(final HttpServletRequest request) {
		return getOriginalClient(request.getRemoteHost(), request.getHeader(HttpServerFactory.X_FORWARD_FOR_HEADER));
	}

	/**
	 * Determine the original client. If there is an x-forwarded-for header,
	 * take the first host/ip from the list. Else, use the remote host.
	 * 
	 * @param remoteHost
	 *            Should be the remote host value retrieved from the HTTP
	 *            Servlet.
	 * @param forwardedForValue
	 *            Should be the value of the x-forwarded-for header, or null if
	 *            there is none.
	 * 
	 * @return The original client host or IP value.
	 */
	public static String getOriginalClient(final String remoteHost, final String forwardedForValue) {
		// if no forwarded for host, just return the remote host
		if (forwardedForValue == null) {
			return remoteHost;
		}

		// remove any accidental white space
		final String trimmedValue = forwardedForValue.trim();

		// if forwarded for host is empty, just return the remote host
		if (trimmedValue.isEmpty()) {
			return remoteHost;
		}

		// We have a forwarded-for value. Use the first entry there
		final String host;
		int commaIndex = trimmedValue.indexOf(',');
		host = commaIndex > 0 ? trimmedValue.substring(0, commaIndex).trim() : trimmedValue;

		return host;
	}

}
