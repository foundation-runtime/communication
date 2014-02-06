package com.cisco.vss.foundation.http.server;

import com.nds.cab.infra.configuration.ConfigurationFactory;
import com.nds.cab.infra.highavailability.HighAvailabilityConstants;
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

	public static void addFiltersToServletContextHandler(String serviceName, ThreadPool threadPool, ServletContextHandler context) {

		context.addFilter(new FilterHolder(new FlowContextFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new ErrorHandlingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		addMonitoringFilter(serviceName, threadPool, context);
		context.addFilter(new FilterHolder(new HttpMethodFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new RequestValidityFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new AvailabilityFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
		// context.addFilter(new FilterHolder(new DOSFilter(serviceName,
		// threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new TraceFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));

        if(ConfigurationFactory.getConfiguration().getBoolean("service." + serviceName + "http.pingFilter.isEnabled", true)){
            context.addFilter(new FilterHolder(new PingFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));
        }else{
            context.addServlet(new ServletHolder(new PingServlet(serviceName)),"/probe");
        }


		context.addFilter(new FilterHolder(new CrossOriginFilter(serviceName)), "/*", EnumSet.allOf(DispatcherType.class));


	}

	protected static void addMonitoringFilter(String serviceName, ThreadPool threadPool, ServletContextHandler context) {
		// read monitoring filter class name
		boolean monitoringFilterAdded = false;
		String monitoringFilterClassName = ConfigurationFactory.getConfiguration().getString("service." + serviceName + ".http.monitoringFilter.className", null);
		if (StringUtils.isNotBlank(monitoringFilterClassName)) {
			try {
				Class<?> monitoringFilterClass = Class.forName(monitoringFilterClassName);

				@SuppressWarnings("unchecked")
				Constructor<? extends MonitoringFilter> constructor = (Constructor<? extends MonitoringFilter>) monitoringFilterClass.getConstructor(String.class, ThreadPool.class);

				if (constructor != null) {

					MonitoringFilter monitoringFilterInstance = constructor.newInstance(serviceName, threadPool);
					context.addFilter(new FilterHolder(monitoringFilterInstance), "/*", EnumSet.allOf(DispatcherType.class));
					monitoringFilterAdded = true;

				} else {
					LOGGER.debug("can't find a constructor that meets the base MonitoringFilter Constructor");
				}
			} catch (Exception e) {
				LOGGER.debug("can't instantiate custom monitoring filter, using default. class name passed is {}. error is: {}", monitoringFilterClassName, e.toString());
			}
		}

		if (!monitoringFilterAdded) {
			context.addFilter(new FilterHolder(new MonitoringFilter(serviceName, threadPool)), "/*", EnumSet.allOf(DispatcherType.class));
		}
	}

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

    public static String getOriginalClient(final HttpServletRequest request) {
        return getOriginalClient(request.getRemoteHost(), request.getHeader(HighAvailabilityConstants.X_FORWARD_FOR_HEADER));
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
