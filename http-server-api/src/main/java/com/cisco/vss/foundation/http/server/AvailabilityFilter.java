package com.cisco.vss.foundation.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter will return HTTP 503 - server busy if the currently used thread pool is low on threads
 * @author Yair Ogen
 *
 */
public class AvailabilityFilter extends AbstractInfraHttpFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityFilter.class);

	private HttpThreadPool threadPool;

	public AvailabilityFilter(String serviceName, HttpThreadPool threadPool) {
		super(serviceName);
		this.threadPool = threadPool;
	}


	/* ------------------------------------------------------------ */
	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		if (!threadPool.isLowOnThreads()) {
			chain.doFilter(request, response);
		} else {
			LOGGER.warn("Low number of threads: returning HTTP 503 error code");
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
	}


	@Override
	protected String getKillSwitchFlag() {
		return "http.availabilityFilter.isEnabled";
	}


	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	
	
	


}
