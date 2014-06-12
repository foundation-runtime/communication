/*
 * Copyright 2014 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.HttpServerFactory;
import com.cisco.oss.foundation.http.server.ServerFailedToStartException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a factory that enables creating servers in a common way. Refer to the wiki to see the full configuration set possible.
 *
 * @author Yair Ogen
 */
public enum JettyHttpServerFactory implements HttpServerFactory, JettyHttpServerFactoryExtensions {

    /**
     * implementing singleton using enum
     */
    INSTANCE;

    private final static Logger LOGGER = LoggerFactory.getLogger(JettyHttpServerFactory.class);

    private static final Map<String, Server> servers = new ConcurrentHashMap<String, Server>();

    private JettyHttpServerFactory() {
    }

    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets) {

        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap, Collections.<EventListener>emptyList());
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, Map<String,String> initParams) {

        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap, Collections.<EventListener>emptyList(), initParams);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filterMap, List<EventListener> eventListeners, Map<String, String> initParams) {
        startHttpServer(serviceName, servlets, filterMap, eventListeners, initParams, "", "", "", "");

    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, List<EventListener> eventListeners) {
        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap,eventListeners);

    }


    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters     - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters) {

        startHttpServer(serviceName, servlets, filters, "", "");
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners) {
        startHttpServer(serviceName, servlets, filters, eventListeners, "", "", "", "");
    }




    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters     - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param keyStorePath - a path to the keystore file
     * @param keyStorePassword - the keystore password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword) {

        startHttpServer(serviceName, servlets, filters, keyStorePath, keyStorePassword, "", "");

    }

    private ServerConnector getServerConnector(String serviceName, Server server, Configuration configuration, String host, int port, int connectionIdleTime, int numberOfAcceptors, int numberOfSelectors, int acceptQueueSize, ConnectionFactory... connectionFactories) {

        ServerConnector connector = new ServerConnector(server, null, null, null, numberOfAcceptors, numberOfSelectors, connectionFactories);

        connector.setAcceptQueueSize(acceptQueueSize);
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(connectionIdleTime);
        return connector;
    }


    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param keyStorePath - a path to the keystore file
     * @param keyStorePassword - the keystore password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, String keyStorePath, String keyStorePassword) {
        startHttpServer(serviceName, servlets, keyStorePath, keyStorePassword, "", "");
    }


    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters     - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param keyStorePath - a path to the keystore file
     * @param keyStorePassword - the keystore password
     * @param trustStorePath - the trust store file path
     * @param trustStorePassword - the trust store password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword){
        startHttpServer(serviceName, servlets, filters, Collections.<EventListener>emptyList(), keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        startHttpServer(serviceName, servlets, filters, eventListeners, new HashMap<String, String>(), keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners, Map<String, String> initParams, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        if (servers.get(serviceName) != null) {
            throw new UnsupportedOperationException("you must first stop stop server: " + serviceName + " before you want to start it again!");
        }

//        ContextHandlerCollection handler = new ContextHandlerCollection();

        ServletContextHandler context = new ServletContextHandler();

        JettyHttpThreadPool jettyHttpThreadPool = new JettyHttpThreadPool(serviceName);

        for (Map.Entry<String, Servlet> entry : servlets.entries()) {


            if (eventListeners!= null && !eventListeners.isEmpty()){
                context.setEventListeners(eventListeners.toArray(new EventListener[0]));
            }

            context.addServlet(new ServletHolder(entry.getValue()), entry.getKey());



//            handler.addHandler(context);
        }

        for (Map.Entry<String, String> initParam : initParams.entrySet()) {
            context.setInitParameter(initParam.getKey(), initParam.getValue());
        }

        HttpServerUtil.addFiltersToServletContextHandler(serviceName, jettyHttpThreadPool, context);

        for (Map.Entry<String, Filter> filterEntry : filters.entries()) {
            context.addFilter(new FilterHolder(filterEntry.getValue()), filterEntry.getKey(), EnumSet.allOf(DispatcherType.class));
        }

        Server server = new Server(jettyHttpThreadPool.threadPool);

        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();

            // set connectors
            String host = configuration.getString(serviceName + ".http.host", "0.0.0.0");
            int port = configuration.getInt(serviceName + ".http.port", 8080);
            int connectionIdleTime = configuration.getInt(serviceName + ".http.connectionIdleTime", 180000);
//            boolean isBlockingChannelConnector = configuration.getBoolean(serviceName + ".http.isBlockingChannelConnector", false);
            int numberOfAcceptors = configuration.getInt(serviceName + ".http.numberOfAcceptors", 1);
            int numberOfSelectors = configuration.getInt(serviceName + ".http.numberOfSelectors", -1);
            int acceptQueueSize = configuration.getInt(serviceName + ".http.acceptQueueSize", 0);

            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setRequestHeaderSize(configuration.getInt(serviceName + ".http.requestHeaderSize", http_config.getRequestHeaderSize()));

            ServerConnector httpConnector = getServerConnector(serviceName, server, configuration, host, port, connectionIdleTime, numberOfAcceptors, numberOfSelectors, acceptQueueSize, new HttpConnectionFactory(http_config));

            boolean useSSLOnly = configuration.getBoolean(serviceName + ".https.useHttpsOnly", false);

            boolean isSSL = StringUtils.isNotBlank(keyStorePath) && StringUtils.isNotBlank(keyStorePassword);
            Connector[] connectors = null;

            if (isSSL) {
                String sslHost = configuration.getString(serviceName + ".https.host", "0.0.0.0");
                int sslPort = configuration.getInt(serviceName + ".https.port", 8090);

                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(keyStorePath);
                sslContextFactory.setKeyStorePassword(keyStorePassword);

                boolean addTrustStoreSupport = StringUtils.isNotEmpty(trustStorePath) && StringUtils.isNotEmpty(trustStorePassword);
                if(addTrustStoreSupport){
                    sslContextFactory.setTrustStorePath(trustStorePath);
                    sslContextFactory.setTrustStorePassword(trustStorePassword);
                    sslContextFactory.setNeedClientAuth(true);
                }

                HttpConfiguration httpsConfig = new HttpConfiguration();
                httpsConfig.setSecurePort(sslPort);
                httpsConfig.setSecureScheme("https");
                http_config.setRequestHeaderSize(configuration.getInt(serviceName + ".http.requestHeaderSize", http_config.getRequestHeaderSize()));


                SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "HTTP/1.1");
                ServerConnector sslConnector = getServerConnector(serviceName, server, configuration, sslHost, sslPort, connectionIdleTime, numberOfAcceptors, numberOfSelectors, acceptQueueSize, sslConnectionFactory, new HttpConnectionFactory(httpsConfig));

                if (useSSLOnly) {
                    connectors = new Connector[]{sslConnector};
                } else {
                    connectors = new Connector[]{httpConnector, sslConnector};
                }
            } else {
                connectors = new Connector[]{httpConnector};

            }

            server.setConnectors(connectors);

            // set servlets/context handlers
//            server.setHandler(handler);
            server.setHandler(context);

            server.start();
            servers.put(serviceName, server);
            LOGGER.info("Http server: {} started on {}", serviceName, port);

            // server.join();
        } catch (Exception e) {
            LOGGER.error("Problem starting the http {} server. Error is {}.", new Object[]{serviceName, e, e});
            throw new ServerFailedToStartException(e);
        }
    }

    /**
     * start a new http server
     *
     * @param serviceName - the http logical service name
     * @param servlets    - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                    <br>Example of usage:
     *                    {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param keyStorePath - a path to the keystore file
     * @param keyStorePassword - the keystore password
     * @param trustStorePath - the trust store file path
     * @param trustStorePassword - the trust store password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }



    /**
     * stop the http server
     *
     * @param serviceName - the http logical service name
     */
    @Override
    public void stopHttpServer(String serviceName) {
        Server server = servers.get(serviceName);
        if (server != null) {
            try {
                server.stop();
                servers.remove(serviceName);
                LOGGER.info("Http server: {} stopped", serviceName);
            } catch (Exception e) {
                LOGGER.error("Problem stoping the http {} server. Error is {}.", serviceName, e);
            }
        }
    }


    @Override
    public void setErrorHandler(String serviceName, ErrorHandler errorHandler) {
        Server server = servers.get(serviceName);
        if (server != null) {
            server.addBean(errorHandler);
        }
    }
}
