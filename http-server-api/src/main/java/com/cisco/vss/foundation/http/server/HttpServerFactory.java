package com.cisco.vss.foundation.http.server;

import com.google.common.collect.ListMultimap;

import javax.servlet.Filter;
import javax.servlet.Servlet;

/**
 * Created by Yair Ogen on 2/5/14.
 */
public interface HttpServerFactory {

    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets);

    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters);

    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword)

}
