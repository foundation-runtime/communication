package com.cisco.oss.foundation.http;

/**
 * The basic interface to foundation http clients.
 * This interface allows you to choose between the following options:
 * </br>1. execute a request directly using executeDirect - assumes a fully constructed url
 * </br>2. execute a request with LoadBalancer using executeWithLoadBalancer - assumes a partially constructed uri
 * </br>3. execute a request directly or with load balancing using execute - decision is based on the 'enableLoadBalancing' flag in the Httpclient factories.
 *
 * This class uses Generics to bind to specific HttpRequest and HttpResponse types. The actual binding is done via the Client factory implementations.
 * Refer to wiki to see possible configurations for clients.
 * Created by Yair Ogen on 1/6/14.
 */
public interface HttpClient<S extends HttpRequest, R extends HttpResponse> {

    /**
     * execute a request either directly or by load balancing.
     * @param request - the http request
     * @return the http response
     */
    R execute(S request);

    /**
     * execute a request directly - assumes a fully constructed url
     * @param request - the http request
     * @return the http response
     */
    R executeDirect(S request);

    /**
     * execute with load balancing. by default RoundRobin will be used. this api assumes partial uri in the request
     * @param request - the http request
     * @return the http response
     */
    R executeWithLoadBalancer(S request);

    /**
     * execute with load balancing asynchronously. by default RoundRobin will be used. this api assumes partial uri in the request
     * @param request - the http request
     * @param responseCallback - teh call back that will get the response asynchronously.
     */
    void executeWithLoadBalancer(S request, ResponseCallback<R> responseCallback);

    /**
     * get the api name used to built this client instance.
     * @return
     */
    String getApiName();


}
