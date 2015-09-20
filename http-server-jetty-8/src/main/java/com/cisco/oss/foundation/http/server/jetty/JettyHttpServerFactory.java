/*
 * Copyright 2015 Cisco Systems, Inc.
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

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.directory.RegistrationManager;
import com.cisco.oss.foundation.directory.ServiceDirectory;
import com.cisco.oss.foundation.directory.entity.OperationalStatus;
import com.cisco.oss.foundation.directory.entity.ProvidedServiceInstance;
import com.cisco.oss.foundation.directory.exception.ServiceException;
import com.cisco.oss.foundation.directory.impl.DirectoryServiceClient;
import com.cisco.oss.foundation.http.server.HttpServerFactory;
import com.cisco.oss.foundation.http.server.ServerFailedToStartException;
import com.cisco.oss.foundation.ip.utils.IpUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.nio.BlockingChannelConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
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
 * This is a factory that enables creating servers in a common way
 *
 * @author Yair Ogen
 */
public enum JettyHttpServerFactory implements HttpServerFactory, JettyHttpServerFactoryExtensions {

    INSTANCE;

    private static List<EventListener> eventListeners = new ArrayList<>(3);

    private final static Logger LOGGER = LoggerFactory.getLogger(JettyHttpServerFactory.class);

    static {
        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();
            String sdHost = configuration.getString("service.sd.host", "");
            int sdPort = configuration.getInt("service.sd.port", -1);

            if (StringUtils.isNotBlank(sdHost)) {
                ServiceDirectory.getServiceDirectoryConfig().setProperty(DirectoryServiceClient.SD_API_SD_SERVER_FQDN_PROPERTY, sdHost);
            }

            if (sdPort > 0) {
                ServiceDirectory.getServiceDirectoryConfig().setProperty(DirectoryServiceClient.SD_API_SD_SERVER_PORT_PROPERTY, sdPort);
            }
        } catch (Exception e) {
            LOGGER.error("Can't assign service Directory host and port properties: {}", e, e);
        }
    }

    private static final Map<String, Pair<Server, ProvidedServiceInstance>> servers = new ConcurrentHashMap<String, Pair<Server, ProvidedServiceInstance>>();

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
        startHttpServer(serviceName, servlets, filterMap);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, Map<String, String> initParams) {

        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap, Collections.<EventListener>emptyList(), initParams);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filterMap, List<EventListener> eventListeners, Map<String, String> initParams) {
        startHttpServer(serviceName, servlets, filterMap, eventListeners, initParams, "", "", "", "");

    }


    /**
     * @param serviceName    - the http logical service name
     * @param servlets       - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                       <br>Example of usage:
     *                       {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param eventListeners event listeners to be applied on this server
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, List<EventListener> eventListeners) {
        ArrayListMultimap<String, Filter> filterMap = ArrayListMultimap.create();
        startHttpServer(serviceName, servlets, filterMap, eventListeners);
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

    /**
     * @param serviceName    - the http logical service name
     * @param servlets       - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                       <br>Example of usage:
     *                       {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters        - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                       <br>Example of usage:
     *                       {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param eventListeners event listeners to be applied on this server
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners) {
        startHttpServer(serviceName, servlets, filters, eventListeners, "", "", "", "");
    }

    /**
     * @param serviceName      - the http logical service name
     * @param servlets         - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                         <br>Example of usage:
     *                         {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters          - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                         <br>Example of usage:
     *                         {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param keyStorePath     - a path to the keystore file
     * @param keyStorePassword - the keystore password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword) {

        startHttpServer(serviceName, servlets, filters, keyStorePath, keyStorePassword, "", "");

    }


    /**
     * @param serviceName      - the http logical service name
     * @param servlets         - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                         <br>Example of usage:
     *                         {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param keyStorePath     - a path to the keystore file
     * @param keyStorePassword - the keystore password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, String keyStorePath, String keyStorePassword) {
        startHttpServer(serviceName, servlets, keyStorePath, keyStorePassword, "", "");
    }

    /**
     * @param serviceName        - the http logical service name
     * @param servlets           - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                           <br>Example of usage:
     *                           {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters            - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                           <br>Example of usage:
     *                           {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param keyStorePath       - a path to the keystore file
     * @param keyStorePassword   - the keystore password
     * @param trustStorePath     - the trust store file path
     * @param trustStorePassword - the trust store password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        startHttpServer(serviceName, servlets, filters, Collections.<EventListener>emptyList(), keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }

    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        startHttpServer(serviceName, servlets, filters, eventListeners, new HashMap<String, String>(), keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }


    /**
     * @param serviceName        - the http logical service name
     * @param servlets           - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                           <br>Example of usage:
     *                           {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param filters            - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                           <br>Example of usage:
     *                           {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
     * @param eventListeners     event listeners to be applied on this server
     * @param keyStorePath       - a path to the keystore file
     * @param keyStorePassword   - the keystore password
     * @param trustStorePath     - the trust store file path
     * @param trustStorePassword - the trust store password
     */
    @Override
    public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, List<EventListener> eventListeners, Map<String, String> initParams, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {

        if (servers.get(serviceName) != null) {
            throw new UnsupportedOperationException("you must first stop stop server: " + serviceName + " before you want to start it again!");
        }

        Configuration configuration = ConfigurationFactory.getConfiguration();
        boolean sessionManagerEnabled = configuration.getBoolean(serviceName + ".http.sessionManagerEnabled", false);

        ContextHandlerCollection handler = new ContextHandlerCollection();

        JettyHttpThreadPool jettyHttpThreadPool = new JettyHttpThreadPool(serviceName);

        Map<String, Map<String, String>> servletMapping = ConfigUtil.parseComplexArrayStructure(serviceName + ".http.servletsMapping");
        Map<String, ServletContextHandler> contextMap = new HashMap<>();

        int servletNum = -1;
        for (Map.Entry<String, Servlet> entry : servlets.entries()) {
            servletNum++;
            ServletContextHandler context = new ServletContextHandler();

            if (sessionManagerEnabled) {
                context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            }

            String uri = entry.getKey();

            if (servletMapping != null && !servletMapping.isEmpty()) {
                contextMap.put(uri, context);
            }

            context.setContextPath(configuration.getString(serviceName + ".http.servletContextPath", "/"));

            if (eventListeners != null && !eventListeners.isEmpty()) {
                if(servletMapping != null && !servletMapping.isEmpty() && eventListeners.size() == servlets.size()){
                    context.setEventListeners(new EventListener[]{eventListeners.get(servletNum)});
                }else{
                    context.setEventListeners(eventListeners.toArray(new EventListener[0]));
                }
                for (EventListener eventListener : JettyHttpServerFactory.eventListeners) {
                    context.addEventListener(eventListener);
                }
            }

            context.addServlet(new ServletHolder(entry.getValue()), uri);

            for (Map.Entry<String, String> initParam : initParams.entrySet()) {
                context.setInitParameter(initParam.getKey(), initParam.getValue());
            }


            HttpServerUtil.addFiltersToServletContextHandler(serviceName, jettyHttpThreadPool, context);

            for (Map.Entry<String, Filter> filterEntry : filters.entries()) {
                context.addFilter(new FilterHolder(filterEntry.getValue()), filterEntry.getKey(), EnumSet.allOf(DispatcherType.class));
            }

            handler.addHandler(context);
        }

        Server server = new Server();
        server.setSendServerVersion(false);

        try {


            List<Connector> connectors = new ArrayList<>(3);
            List<String> startupLogs = new ArrayList<>(3);


            // set connectors
            String host = configuration.getString(serviceName + ".http.host", "0.0.0.0");
            int port = configuration.getInt(serviceName + ".http.port", 8080);
            int connectionIdleTime = configuration.getInt(serviceName + ".http.connectionIdleTime", 180000);
            boolean isBlockingChannelConnector = configuration.getBoolean(serviceName + ".http.isBlockingChannelConnector", false);
            int numberOfAcceptors = configuration.getInt(serviceName + ".http.numberOfAcceptors", Runtime.getRuntime().availableProcessors());
            int acceptQueueSize = configuration.getInt(serviceName + ".http.acceptQueueSize", 0);
            boolean serviceDirectoryEnabled = configuration.getBoolean(serviceName + ".http.serviceDirectory.isEnabled", false);

            if (servletMapping != null && !servletMapping.isEmpty()) {


                for (Map<String, String> servletToConnctor : servletMapping.values()) {
                    String logicalName = servletToConnctor.get("logicalName");
                    String uriMapping = servletToConnctor.get("uriMapping");
                    String hostForConnector = servletToConnctor.get("host");
                    int portForConnector = Integer.parseInt(servletToConnctor.get("port"));

                    ServletContextHandler servletContextHandler = contextMap.get(uriMapping);
                    servletContextHandler.setVirtualHosts(new String[]{"@" + logicalName});
                    boolean isSSL = Boolean.valueOf(servletToConnctor.get("isSSL"));
                    getConnectors(startupLogs, connectors, isSSL, logicalName, serviceName, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword, configuration, hostForConnector, portForConnector, connectionIdleTime, isBlockingChannelConnector, numberOfAcceptors, acceptQueueSize);
                    servletContextHandler.setConnectorNames(new String[]{logicalName});
                }


            } else {
                boolean useHttpsOnly = configuration.getBoolean(serviceName + ".https.useHttpsOnly", false);
                getConnectors(startupLogs, connectors, useHttpsOnly, null, serviceName, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword, configuration, host, port, connectionIdleTime, isBlockingChannelConnector, numberOfAcceptors, acceptQueueSize);

            }

            server.setConnectors(connectors.toArray(new Connector[0]));

            // set thread pool
            server.setThreadPool(jettyHttpThreadPool.threadPool);

            // set servlets/context handlers
            server.setHandler(handler);



            server.start();
            ProvidedServiceInstance instance = null;
            if (serviceDirectoryEnabled) {
                instance = registerWithSDServer(serviceName, host, port);
            }

            servers.put(serviceName, Pair.of(server, instance));

            for (String startupLog : startupLogs) {
                LOGGER.info(startupLog);
            }

            // server.join();
        } catch (Exception e) {
            LOGGER.error("Problem starting the http {} server. Error is {}.", new Object[]{serviceName, e, e});
            throw new ServerFailedToStartException(e);
        }
    }

    private void getConnectors(List<String> startupLogs, List<Connector> connectors, boolean useHttpsOnly, String logicalName, String serviceName, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword, Configuration configuration, String host, int port, int connectionIdleTime, boolean isBlockingChannelConnector, int numberOfAcceptors, int acceptQueueSize) {

        AbstractConnector connector = null;
        if (isBlockingChannelConnector) {
            connector = new BlockingChannelConnector();
        } else {
            connector = new SelectChannelConnector();
        }

        connector.setAcceptQueueSize(acceptQueueSize);
        connector.setAcceptors(numberOfAcceptors);
        connector.setPort(port);
        connector.setHost(host);
        connector.setMaxIdleTime(connectionIdleTime);
        connector.setRequestHeaderSize(configuration.getInt(serviceName + ".http.requestHeaderSize", connector.getRequestHeaderSize()));

        if (logicalName != null) {
            connector.setName(logicalName);
        }


        boolean isSSL = StringUtils.isNotBlank(keyStorePath) && StringUtils.isNotBlank(keyStorePassword);
        int sslPort = -1;

        SslSelectChannelConnector sslSelectChannelConnector = null;

        if (isSSL) {
            String sslHost = configuration.getString(serviceName + ".https.host", "0.0.0.0");
            sslPort = configuration.getInt(serviceName + ".https.port", 8090);

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keyStorePath);
            sslContextFactory.setKeyStorePassword(keyStorePassword);

            boolean addTrustStoreSupport = StringUtils.isNotEmpty(trustStorePath) && StringUtils.isNotEmpty(trustStorePassword);
            if (addTrustStoreSupport) {
                sslContextFactory.setTrustStore(trustStorePath);
                sslContextFactory.setTrustStorePassword(trustStorePassword);
                sslContextFactory.setNeedClientAuth(true);
            }

            sslSelectChannelConnector = new SslSelectChannelConnector(sslContextFactory);
            sslSelectChannelConnector.setHost(sslHost);
            sslSelectChannelConnector.setPort(sslPort);
            if (logicalName != null) {
                sslSelectChannelConnector.setName(logicalName);
            }

            if (useHttpsOnly) {
                sslSelectChannelConnector.setAcceptQueueSize(acceptQueueSize);
                sslSelectChannelConnector.setAcceptors(numberOfAcceptors);
                sslSelectChannelConnector.setMaxIdleTime(connectionIdleTime);
                sslSelectChannelConnector.setRequestHeaderSize(configuration.getInt(serviceName + ".http.requestHeaderSize", sslSelectChannelConnector.getRequestHeaderSize()));
                connectors.add(sslSelectChannelConnector);
                startupLogs.add("Https server: " + serviceName + " started on " + sslPort);
            } else {
                startupLogs.add("Https server: " + serviceName + " started on " + sslPort);
                startupLogs.add("Http server: " + serviceName + " started on " + port);
                connectors.add(connector);
                connectors.add(sslSelectChannelConnector);
            }
        } else {
            startupLogs.add("Http server: " + serviceName + " started on " + port);
            connectors.add(connector);

        }
    }

    /**
     * @param serviceName        - the http logical service name
     * @param servlets           - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
     *                           <br>Example of usage:
     *                           {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
     * @param keyStorePath       - a path to the keystore file
     * @param keyStorePassword   - the keystore password
     * @param trustStorePath     - the trust store file path
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
        Pair<Server, ProvidedServiceInstance> pair = servers.get(serviceName);
        if (pair != null) {
            Server server = pair.getLeft();
            if (server != null) {
                try {
                    server.stop();
                    servers.remove(serviceName);
                    LOGGER.info("Http server: {} stopped", serviceName);
                } catch (Exception e) {
                    LOGGER.error("Problem stopping the http {} server. Error is {}.", serviceName, e);
                }
            }
            ProvidedServiceInstance instance = pair.getRight();
            if (instance != null) {
                unRegisterInstance(serviceName, instance);
            }
        }
    }

    @Override
    public void setErrorHandler(String serviceName, ErrorHandler errorHandler) {
        Server server = servers.get(serviceName).getLeft();
        if (server != null) {
            server.addBean(errorHandler);
        }
    }

//    @Override
    public void setAdditionalListeners(List<EventListener> eventListeners) {
        this.eventListeners = eventListeners;
    }

    private ProvidedServiceInstance registerWithSDServer(final String serviceName, String host, int port) throws Exception {
        // Get a RegistrationManager instance from ServiceDirectory.
// The ServiceDirectory will load a default ServiceDirectoryConfig and instantialize a RegistrationManager instance.
        final RegistrationManager registrationManager = ServiceDirectory.getRegistrationManager();


// Construct the service instance. serviceName and providerId together uniquely identify a service instance where providerId is defined as "address-port".
        final ProvidedServiceInstance instance = new ProvidedServiceInstance(serviceName, host, port);

// Setting the service instance URI. URI is defined as tcp://address:port for the TCP end point
        String serverHost = "0.0.0.0".equals(host) ? IpUtils.getIpAddress() : host;
        instance.setUri("http://" + serverHost + ":" + port + "");
        instance.setAddress(serverHost);
        instance.setPort(port);

        instance.setStatus(OperationalStatus.UP);

        registrationManager.registerService(instance, null);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                unRegisterInstance(serviceName, instance);
            }
        });

        return instance;


    }

    private void unRegisterInstance(String serviceName, ProvidedServiceInstance instance) {
        try {
            final RegistrationManager registrationManager = ServiceDirectory.getRegistrationManager();
            registrationManager.unregisterService(instance.getServiceName(), instance.getProviderId());
        } catch (ServiceException e) {
            LOGGER.info("Problem stopping the http {} server. Probably already un-registered. Error is {}.", serviceName, e);
        }
    }

}
