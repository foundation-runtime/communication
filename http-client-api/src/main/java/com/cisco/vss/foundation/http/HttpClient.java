package com.cisco.vss.foundation.http;

import com.cisco.vss.foundation.loadbalancer.HighAvailabilityStrategy;

import java.io.IOException;

/**
 * Created by Yair Ogen on 1/6/14.
 */
public interface HttpClient<S extends HttpRequest, R extends HttpResponse> {

    R execute(S request);

    R executeDirect(S request);

    R executeWithLoadBalancer(S request);

    void executeWithLoadBalancer(S request, ResponseCallback<R> responseCallback);

    String getApiName();


}
