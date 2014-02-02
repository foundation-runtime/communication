package com.cisco.vss.foundation.http.server.jetty;

import com.cisco.vss.foundation.http.server.AbstractInfraHttpFilter;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ErrorHandlingFilter extends AbstractInfraHttpFilter {

	private final static Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingFilter.class);

	public ErrorHandlingFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

		try {
			chain.doFilter(request, response);
		} catch (EofException e) {
			LOGGER.error("EOF Exception caught. Probably client closed connection before the response was sent.");
			// don't send response. no one is waiting for it on the other side
		} catch (Throwable e) {
			LOGGER.error("error serving request: " + e, e);
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		}

	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.errorHandlingFilter.isEnabled";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
