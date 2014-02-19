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
 * Filter that filters too long requests. Default size is 100000 (100K). You can
 * override the default by using "http.maxContentLength" parameter in
 * your config file.
 * 
 * @author Yair Ogen
 * 
 */
public class RequestValidityFilter extends AbstractInfraHttpFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestValidityFilter.class);

	public RequestValidityFilter(String serviceName) {
		super(serviceName);
	}

	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//		final int contentLimit = ConfigurationFactory.getConfiguration().getInt(serviceName + ".http.requestValidityFilter.maxContentLength", 100000);
		final int contentLimit = Integer.parseInt(getConfigValue(serviceName + ".http.requestValidityFilter.maxContentLength", "100000"));
		final int contentLength = request.getContentLength();
		if (contentLength < -1 || contentLength > contentLimit) {
			LOGGER.error("The content length of the request, {}, is larger than the max allowed - {}", contentLength, contentLimit);
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

//		try {
			chain.doFilter(request, response);
//		} catch (Throwable e) {
//			LOGGER.error("error processing request. " + e, e);
//			if (response instanceof HttpServletResponse) {
//				HttpServletResponse resp = (HttpServletResponse) response;
//				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
//			}
//		}
	}

	@Override
	protected String getKillSwitchFlag() {
		return "http.requestValidityFilter.isEnabled";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

}
