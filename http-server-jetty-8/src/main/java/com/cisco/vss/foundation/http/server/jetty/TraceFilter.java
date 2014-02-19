package com.cisco.vss.foundation.http.server.jetty;

import com.cisco.vss.foundation.http.server.AbstractInfraHttpFilter;
import org.apache.log4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter supposed to be the first filter on any chain of filters/servlets
 * that processing HTTP request. It replies to PING request or doing nothing and
 * forwarding this request to the next filter. As it is the first filter it is
 * also can print request and response content for debugging purposes.
 * 
 * 
 * @author Dima Mirkin
 * @author Yair Ogen
 */
public class TraceFilter extends AbstractInfraHttpFilter {

	private int bodyLimit = 1024;
	private final static Logger LOGGER = Logger.getLogger(TraceFilter.class);

	public TraceFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		bodyLimit = Integer.parseInt(getConfigValue(serviceName+".http.traceFilter.maxBodyLength", bodyLimit+""));
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
			LOGGER.trace("Starting trace filter.");
			String originalClient = getOriginalClient(httpRequest);
			LOGGER.info("Received HTTP request from " + originalClient + ":" + request.getRemotePort());

			if (LOGGER.isDebugEnabled()) {
				final TraceResponseWrapper wrappedResponse = new TraceResponseWrapper((HttpServletResponse) response, bodyLimit, serviceName);
				final TraceRequestWrapper wrappedRequest = new TraceRequestWrapper(httpRequest, bodyLimit, serviceName);

				//RESOLUTION OF: CQ 1578469 - SGW Logs contain ^M characters
				String escaped = wrappedRequest.toStringWithBody().replaceAll("\\r\\n", System.getProperty("line.separator"));
				LOGGER.debug("Request:\n" + escaped);
				
				chain.doFilter(wrappedRequest, wrappedResponse);
				
				//RESOLUTION OF: CQ 1578469 - SGW Logs contain ^M characters
				escaped = wrappedResponse.toStringWithBody().replaceAll("\\r\\n", System.getProperty("line.separator"));
				LOGGER.debug("Response:\n" + escaped);
				wrappedResponse.writeBody();
			}else{
				chain.doFilter(request, response);
			}

			
			LOGGER.info("Sending HTTP response to " + originalClient + ":" + request.getRemotePort());

			

			
	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.traceFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return false;
	}

}
