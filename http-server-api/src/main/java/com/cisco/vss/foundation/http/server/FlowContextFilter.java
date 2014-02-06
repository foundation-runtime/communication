package com.cisco.vss.foundation.http.server;

import com.cisco.vss.foundation.flowcontext.FlowContextFactory;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class FlowContextFilter extends AbstractInfraHttpFilter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowContextFilter.class);
	
	public FlowContextFilter(String serviceName){
		super(serviceName);
	}

	
	@Override
	public void doFilterImpl(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			HttpServletRequest httpServletRequest = (HttpServletRequest)request;
			String flowCtxtStr = httpServletRequest.getHeader(FLOW_CONTEXT_HEADER);
			if(!Strings.isNullOrEmpty(flowCtxtStr)){
				FlowContextFactory.deserializeNativeFlowContext(flowCtxtStr);
			}else{
				FlowContextFactory.createFlowContext();
			}
			
		} catch (Exception e) {
			LOGGER.warn("problem setting flow context filter: " + e, e);
		}
		chain.doFilter(request, response);
		
	}

	
	@Override
	protected String getKillSwitchFlag() {
		return "http.flowContextFilter.isEnabled";
	}
	
	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
	
	

}
