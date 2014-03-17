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
import com.cisco.oss.foundation.http.server.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.util.EnumSet;

/**
 * utility class to add common filters to a given jetty context handler
 */
public class HttpServerUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerUtil.class);

	public static void addFiltersToServletContextHandler(String serviceName, HttpThreadPool threadPool, ServletContextHandler context) {

		context.addFilter(new FilterHolder(new FlowContextFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new ErrorHandlingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
        context.addFilter(new FilterHolder(new MonitoringFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
//		addMonitoringFilter(serviceName, threadPool, context);
		context.addFilter(new FilterHolder(new HttpMethodFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new RequestValidityFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new AvailabilityFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new TraceFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));

        if(ConfigurationFactory.getConfiguration().getBoolean(serviceName + "http.pingFilter.isEnabled", true)){
            context.addFilter(new FilterHolder(new PingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
        }else{
            context.addServlet(new ServletHolder(new PingServlet(serviceName)),"/probe");
        }


		context.addFilter(new FilterHolder(new CrossOriginFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));


	}

//	protected static void addMonitoringFilter(String serviceName, ThreadPool threadPool, ServletContextHandler context) {
//		// read monitoring filter class name
//		boolean monitoringFilterAdded = false;
//		String monitoringFilterClassName = ConfigurationFactory.getConfiguration().getString(serviceName + ".http.monitoringFilter.className", null);
//		if (StringUtils.isNotBlank(monitoringFilterClassName)) {
//			try {
//				Class<?> monitoringFilterClass = Class.forName(monitoringFilterClassName);
//
//				@SuppressWarnings("unchecked")
//				Constructor<? extends MonitoringFilter> constructor = (Constructor<? extends MonitoringFilter>) monitoringFilterClass.getConstructor(String.class, ThreadPool.class);
//
//				if (constructor != null) {
//
//					MonitoringFilter monitoringFilterInstance = constructor.newInstance(serviceName, threadPool);
//					context.addFilter(new FilterHolder(monitoringFilterInstance), "/*", EnumSet.allOf(DispatcherType.class));
//					monitoringFilterAdded = true;
//
//				} else {
//					LOGGER.debug("can't find a constructor that meets the base MonitoringFilter Constructor");
//				}
//			} catch (Exception e) {
//				LOGGER.debug("can't instantiate custom monitoring filter, using default. class name passed is {}. error is: {}", monitoringFilterClassName, e.toString());
//			}
//		}
//
//		if (!monitoringFilterAdded) {
//			context.addFilter(new FilterHolder(new MonitoringFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
//		}
//	}


}
