package com.cisco.oss.foundation.http.server;

import org.apache.log4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter replies to PING request if request contains
 * <code>NDS-Proxy-Ping</code> header.
 * 
 * @author Dima Mirkin
 * @author Yair Ogen
 */
public class PingFilter extends AbstractInfraHttpFilter {

	private static final String PING_HEADER = "NDS-Proxy-Ping";
	private final static Logger LOGGER = Logger.getLogger(PingFilter.class);
	
	public PingFilter(String serviceName) {
		super(serviceName);
	}


	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
//		LOGGER.trace("Starting ping filter");
//        Configuration configuration = ConfigurationFactory.getConfiguration();
//        boolean enableLogging = configuration.getBoolean(serviceName + "http.pingFilter.enableLogging",false);
        boolean enableLogging = Boolean.valueOf(getConfigValue(serviceName + "http.pingFilter.enableLogging","false"));
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		if (null == httpRequest.getHeader(PING_HEADER)) {
			// if not ping request - do nothing, just forward through the chain
			chain.doFilter(request, response);
		} else {
            if (enableLogging) {
                LOGGER.debug("HTTP Ping received from " + getOriginalClient(httpRequest));
            }
            ((HttpServletResponse) response).setDateHeader("Date", System.currentTimeMillis());
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK);
		}
	}
	
	@Override
	protected String getKillSwitchFlag() {
		return "http.pingFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
