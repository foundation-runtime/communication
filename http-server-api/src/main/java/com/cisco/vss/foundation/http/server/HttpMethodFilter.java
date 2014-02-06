package com.cisco.vss.foundation.http.server;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class HttpMethodFilter extends AbstractInfraHttpFilter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpMethodFilter.class);
	private final Set<String> methods = new HashSet<String>(5);
	
	public HttpMethodFilter(String serviceName){
		super(serviceName);
		methods.add("TRACE");
		updateAllowedMethodsFromConfig(serviceName);
	}

	
	private void updateAllowedMethodsFromConfig(String serviceName) {
		Configuration configuration = ConfigurationFactory.getConfiguration();
		Configuration subset = configuration.subset("service." + serviceName + ".http.httpMethodFilter.methods");
		@SuppressWarnings("unchecked")
		Iterator<String> keys = subset.getKeys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			methods.add(subset.getString(key).toUpperCase(Locale.getDefault()));
		}

	}


	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		
		String method = httpServletRequest.getMethod();
		if(methods.contains(method.toUpperCase(Locale.getDefault()))){
			LOGGER.error("method {} is not allowed", method);
			((HttpServletResponse)response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}else{			
			chain.doFilter(request, response);
		}
		
	}

	
	@Override
	protected String getKillSwitchFlag() {
		return "http.httpMethodFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	

}
