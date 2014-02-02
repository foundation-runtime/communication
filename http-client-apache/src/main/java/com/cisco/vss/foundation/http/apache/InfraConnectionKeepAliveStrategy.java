package com.cisco.vss.foundation.http.apache;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Created by Yair Ogen on 1/26/14.
 */
public class InfraConnectionKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy {

    private long idleTimeout = -1;

    public InfraConnectionKeepAliveStrategy(long idleTimeout){
        this.idleTimeout = idleTimeout;
    }

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long timeout = super.getKeepAliveDuration(response, context);
        if(timeout == -1){
           timeout = idleTimeout;
        }

        return timeout;
    }
}
