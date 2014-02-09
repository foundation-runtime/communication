package com.cisco.vss.foundation.http.server.jetty;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.server.*;
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

public class HttpServerUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerUtil.class);

	public static void addFiltersToServletContextHandler(String serviceName, HttpThreadPool threadPool, ServletContextHandler context) {

		context.addFilter(new FilterHolder(new FlowContextFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new ErrorHandlingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
//		addMonitoringFilter(serviceName, threadPool, context);
		context.addFilter(new FilterHolder(new HttpMethodFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new RequestValidityFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new AvailabilityFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
//		context.addFilter(new FilterHolder(new TraceFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));

        if(ConfigurationFactory.getConfiguration().getBoolean("service." + serviceName + "http.pingFilter.isEnabled", true)){
            context.addFilter(new FilterHolder(new PingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
        }else{
            context.addServlet(new ServletHolder(new PingServlet(serviceName)),"/probe");
        }


		context.addFilter(new FilterHolder(new CrossOriginFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));


	}

//	protected static void addMonitoringFilter(String serviceName, ThreadPool threadPool, ServletContextHandler context) {
//		// read monitoring filter class name
//		boolean monitoringFilterAdded = false;
//		String monitoringFilterClassName = ConfigurationFactory.getConfiguration().getString("service." + serviceName + ".http.monitoringFilter.className", null);
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

	public static QueuedThreadPool getDefaultThreadPool(String serviceName) {
		Configuration configuration = ConfigurationFactory.getConfiguration();
		QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
		int minThreads = configuration.getInt("service." + serviceName + ".http.minThreads", 100);
		int maxThreads = configuration.getInt("service." + serviceName + ".http.maxThreads", 1000);
		if (minThreads <= 0) {
			throw new IllegalArgumentException("http server min number of threads must be greater than 0");
		}
		queuedThreadPool.setMaxThreads(maxThreads);
		queuedThreadPool.setMinThreads(minThreads);
		return queuedThreadPool;
	}


}
