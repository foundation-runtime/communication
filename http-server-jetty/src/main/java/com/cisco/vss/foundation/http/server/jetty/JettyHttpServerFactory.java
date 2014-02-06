package com.cisco.vss.foundation.http.server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.nds.cab.infra.configuration.ConfigurationFactory;
import com.nds.cab.infra.highavailability.HighAvailabilityConstants;
import com.nds.cab.infra.highavailability.http.exporter.InfraJettyInvokerServiceExporter;
import com.nds.cab.infra.highavailability.rmi.exporter.InfraRmiServiceExporter;
import com.nds.cab.infra.highavailability.rmi.exporter.InfraRmiServiceExporter.ServerRecoveryDaemon;
import com.nds.cab.infra.inet.utils.IpUtils;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.BlockingChannelConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a factory that enables creating servers in a common way
 * 
 * @author Yair Ogen
 */
public enum JettyHttpServerFactory implements HttpServerFactory{

    INSTANCE;

	private final static Logger LOGGER = LoggerFactory.getLogger(com.cisco.vss.foundation.http.server.JettyServerFactory.class);

	private static final Map<String, Server> servers = new ConcurrentHashMap<String, Server>();

	private JettyHttpServerFactory() {
	}

	/**
	 * start a new http server
	 * 
	 * @param serviceName - the http logical service name
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 */
    @Override
	public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets) {

		QueuedThreadPool queuedThreadPool = HttpServerUtil.getDefaultThreadPool(serviceName);

		ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create();
		startHttpServer(serviceName, queuedThreadPool, servlets, filterMap);
	}


	/**
	 * start a new http server
	 *
	 * @param serviceName - the http logical service name
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 * @param filters - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
	 */
    @Override
	public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters) {

		QueuedThreadPool queuedThreadPool = HttpServerUtil.getDefaultThreadPool(serviceName);
		startHttpServer(serviceName, queuedThreadPool, servlets, filters);
	}

	/**
	 * start a new http server
	 *
	 * @param serviceName - the http logical service name
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 * @param filters - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
	 * @param sslSelectChannelConnector - a connector to support https
	 */
	public void startHttpServer(String serviceName, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, String keyStorePath, String keyStorePassword) {

		QueuedThreadPool queuedThreadPool = HttpServerUtil.getDefaultThreadPool(serviceName);
		startHttpServer(serviceName, queuedThreadPool, servlets, filters, sslSelectChannelConnector);
	}


	/**
	 * start a new http server
	 *
	 * @param serviceName - the http logical service name
	 * @param threadPool - the jetty specific thread pool
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 * @param filters - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
	 * @param sslSelectChannelConnector - a connector to support https
	 */
	public static void startHttpServer(String serviceName, ThreadPool threadPool, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters, SslSelectChannelConnector sslSelectChannelConnector) {


		ContextHandlerCollection handler = new ContextHandlerCollection();

		for (Map.Entry<String, Servlet> entry : servlets.entries()) {
			ServletContextHandler context = new ServletContextHandler();
			context.addServlet(new ServletHolder(entry.getValue()), entry.getKey());

			HttpServerUtil.addFiltersToServletContextHandler(serviceName, threadPool, context);

			for (Map.Entry<String, Filter> filterEntry : filters.entries()) {
				context.addFilter(new FilterHolder(filterEntry.getValue()), filterEntry.getKey(), EnumSet.allOf(DispatcherType.class));
			}

			handler.addHandler(context);
		}

		startHttpServer(serviceName, threadPool, handler, sslSelectChannelConnector);


	}

	/**
	 * start a new http server
	 *
	 * @param serviceName - the http logical service name
	 * @param threadPool - the jetty specific thread pool
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 * @param filters - a mapping between filter path and filter instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create()}
	 */
	public static void startHttpServer(String serviceName, ThreadPool threadPool, ListMultimap<String, Servlet> servlets, ListMultimap<String, Filter> filters) {

		startHttpServer(serviceName, threadPool, servlets, filters, null);

	}

	/**
	 * start a new http server
	 * @param serviceName - the http logical service name
	 * @param threadPool - the jetty specific thread pool
	 * @param servlets - a mapping between servlet path and servlet instance. This mapping uses the google collections {@link com.google.common.collect.ListMultimap}.
	 * <br>Example of usage:
	 * {@code ArrayListMultimap<String,Servlet> servletMap = ArrayListMultimap.create()}
	 */
	public static void startHttpServer(String serviceName, ThreadPool threadPool, ListMultimap<String, Servlet> servlets) {
		ArrayListMultimap<String,Filter> filterMap = ArrayListMultimap.create();
		startHttpServer(serviceName, threadPool, servlets, filterMap);

	}

	/**
	 * start a new http server
	 * @param serviceName - the http logical service name
	 * @param handler - the jetty handler
	 * @param sslSelectChannelConnector - a connector to support https
	 */
	public static void startHttpServer(String serviceName, ServletContextHandler handler, SslSelectChannelConnector sslSelectChannelConnector) {
		QueuedThreadPool threadPool = HttpServerUtil.getDefaultThreadPool(serviceName);
		HttpServerUtil.addFiltersToServletContextHandler(serviceName, threadPool, handler);
		startHttpServer(serviceName, threadPool, handler, sslSelectChannelConnector);
	}

	/**
	 * start a new http server - recommended to use this method when using REST frameworks.
	 * @param serviceName - the http logical service name
	 * @param handler - the jetty ServletContextHandler specific handler
	 */
	public static void startHttpServer(String serviceName, ServletContextHandler handler) {
		startHttpServer(serviceName, handler, null);
	}

	/**
	 * start a new http server
	 * @param serviceName - the http logical service name
	 * @param handler - the jetty handler
	 */
	public static void startHttpServer(String serviceName, Handler handler) {
		QueuedThreadPool queuedThreadPool = HttpServerUtil.getDefaultThreadPool(serviceName);
		startHttpServer(serviceName, queuedThreadPool, handler);
	}

	/**
	 * start a new http server
	 * this method is synchronized to bypass a thread safety bug in java Util class.
	 * see here for more info: http://bugs.sun.com/view_bug.do?bug_id=6427854
	 * @param serviceName - the http logical service name
	 * @param threadPool - the jetty specific thread pool
	 * @param handler - the jetty handler
	 * @param sslSelectChannelConnector - a connector to support https
	 */
	public synchronized static void startHttpServer(String serviceName, ThreadPool threadPool, Handler handler, SslSelectChannelConnector sslSelectChannelConnector) {


		if (servers.get(serviceName) != null) {
			throw new UnsupportedOperationException("you must first stop stop server: " + serviceName + " before you want to start it again!");
		}

		Server server = new Server();

		try {
			Configuration configuration = ConfigurationFactory.getConfiguration();

			// set connectors
			String host = configuration.getString("service." + serviceName + ".http.host", IpUtils.getLocalHost());
			int port = configuration.getInt("service." + serviceName + ".http.port", 8080);
			int connectionIdleTime = configuration.getInt("service." + serviceName + ".http.connectionIdleTime", 180000);
			boolean isBlockingChannelConnector = configuration.getBoolean("service." + serviceName + ".http.isBlockingChannelConnector",false);
			int numberOfAcceptors = configuration.getInt("service." + serviceName + ".http.numberOfAcceptors", 1);
			int acceptQueueSize = configuration.getInt("service." + serviceName + ".http.acceptQueueSize", 0);

			AbstractConnector connector = null;
			if(isBlockingChannelConnector){
				connector = new BlockingChannelConnector();
			}else{
				connector = new SelectChannelConnector();
			}

			connector.setAcceptQueueSize(acceptQueueSize);
			connector.setAcceptors(numberOfAcceptors);
			connector.setPort(port);
			connector.setHost(host);
			connector.setMaxIdleTime(connectionIdleTime);
			connector.setRequestHeaderSize(configuration.getInt("service." + serviceName + ".http.requestHeaderSize", connector.getRequestHeaderSize()));


			Connector[] connectors = null;

			if(sslSelectChannelConnector == null){
				connectors = new Connector[] { connector };
			}else{
				String sslHost = configuration.getString("service." + serviceName + ".https.host", IpUtils.getLocalHost());
				int sslPort = configuration.getInt("service." + serviceName + ".https.port", 8090);

				sslSelectChannelConnector.setHost(sslHost);
				sslSelectChannelConnector.setPort(sslPort);

				connectors = new Connector[] { connector, sslSelectChannelConnector };
			}


			server.setConnectors(connectors);

			// set thread pool
			server.setThreadPool(threadPool);

			// set servlets/context handlers
			server.setHandler(handler);

			server.start();
			servers.put(serviceName, server);
			LOGGER.info("Http server: {} started on {}", serviceName, port);

			// server.join();
		} catch (Exception e) {
			LOGGER.error("Problem starting the http {} server. Error is {}.", new Object[]{serviceName, e,e});
            throw new ServerFailedToStartException(e);
		}


	}

	/**
	 *
	 * @param serviceName - the http logical service name
	 * @param threadPool - the jetty specific thread pool
	 * @param handler - the jetty specific handler
	 */
	public static void startHttpServer(String serviceName, ThreadPool threadPool, Handler handler) {
		startHttpServer(serviceName, threadPool, handler, null);
	}

	/**
	 * stop the http server
	 * @param serviceName - the http logical service name
	 */
	public static void stopHttpServer(String serviceName) {
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

	/**
	 * start an RMI server and have a listener ready to get rmi requests. This
	 * implementation uses an infra implementation, and uses a wrapper for SUN
	 * RMI.
	 *
	 * @param port
	 *            - the port to listen to.
	 * @param serviceName
	 *            - the service name for the RMI registry. should be identical
	 *            to the service name used in the client side.
	 * @param serviceInterface
	 *            - the interface exposed to the client as the RMI interface.
	 * @param serviceImpl
	 *            - the server side implementation object that is to handle the
	 *            incoming RMI requests. This must be a class that implements
	 *            the interface defined in the "serviceInterface" parameter.
	 * @param configuration
	 *            - the configuration to be used by the RMI service exporter.
	 *            This should hold default values for the ports to be used. If
	 *            you use a proprietary config file, you should create a
	 *            PropertiesConfiguration object that wraps this config file.
	 * @throws java.rmi.RemoteException
	 *             thrown if any remote error has occurred.
	 */
	public static void startRMIServer(final String serviceName, final Class<?> serviceInterface, final Object serviceImpl) throws RemoteException {
		startRMIServer(serviceName, serviceInterface, serviceImpl, null);
	}

	/**
	 * start an RMI server and have a listener ready to get rmi requests. This
	 * implementation uses an infra implementation, and uses a wrapper for SUN
	 * RMI.
	 *
	 * @param port
	 *            - the port to listen to.
	 * @param serviceName
	 *            - the service name for the RMI registry. should be identical
	 *            to the service name used in the client side.
	 * @param serviceInterface
	 *            - the interface exposed to the client as the RMI interface.
	 * @param serviceImpl
	 *            - the server side implementation object that is to handle the
	 *            incoming RMI requests. This must be a class that implements
	 *            the interface defined in the "serviceInterface" parameter.
	 * @param configuration
	 *            - the configuration to be used by the RMI service exporter.
	 *            This should hold default values for the ports to be used. If
	 *            you use a proprietary config file, you should create a
	 *            PropertiesConfiguration object that wraps this config file.
	 * @param serviceDescription
	 *            - the descirption of this service. used in monitoring API.
	 * @throws java.rmi.RemoteException
	 *             thrown if any remote error has occurred.
	 */
	public static void startRMIServer(final String serviceName, final Class<?> serviceInterface, final Object serviceImpl, final String serviceDescription) throws RemoteException {

		Configuration configuration = ConfigurationFactory.getConfiguration();
		String rpcImpl = configuration.getString(HighAvailabilityConstants.RPC_SERVER);

		if (HighAvailabilityConstants.RMI_SERVER.equals(rpcImpl)) {
			final InfraRmiServiceExporter infraRmiServiceExporter = new InfraRmiServiceExporter();
			try {
				infraRmiServiceExporter.setConfiguration(ConfigurationFactory.getConfiguration());
				infraRmiServiceExporter.setServiceName(serviceName);
				infraRmiServiceExporter.setServiceInterface(serviceInterface);
				infraRmiServiceExporter.setService(serviceImpl);
				infraRmiServiceExporter.setServiceDescription(serviceDescription);
			} catch (Exception e) {
				throw new RemoteException("Error Starting RMI server! " + e.toString(), e);
			}
			infraRmiServiceExporter.afterPropertiesSet();
		} else {
			// http implementation
			InfraJettyInvokerServiceExporter infraJettyInvokerServiceExporter = new InfraJettyInvokerServiceExporter();

			infraJettyInvokerServiceExporter.setConfiguration(ConfigurationFactory.getConfiguration());
			infraJettyInvokerServiceExporter.setServiceName(serviceName);
			infraJettyInvokerServiceExporter.setServiceInterface(serviceInterface);
			infraJettyInvokerServiceExporter.setService(serviceImpl);
			infraJettyInvokerServiceExporter.setApplicationContext(new ClassPathXmlApplicationContext("classpath:/META-INF/cabRemoteInvocationInfraServerContext.xml"));
			// infraJettyInvokerServiceExporter.setServiceDescription(serviceDescription);
			infraJettyInvokerServiceExporter.afterPropertiesSet();

		}

	}

	/**
	 * Re-Start a new RMI server for spring users.<br>
	 * NOTE: This method should only be used if the rmi server was already
	 * started once by regular spring usage (e.g. injection).<br>
	 * DO NOT USE THIS METHOD AS THE FIRST INITIALIZATION OF A SERVER. In such a
	 * case use the overloading API of startRMIServer or use the spring
	 * injection.
	 *
	 * @param serviceName
	 *            the service name of the rmi server you wish to start.
	 * @throws java.rmi.RemoteException
	 *             thrown in case rmi server cannot be started.
	 */
	public static void restartRMIServer(final String serviceName) throws RemoteException {

		final InfraRmiServiceExporter exporter = (InfraRmiServiceExporter) InfraRmiServiceExporter.STOPPED_SERVERS.get(serviceName);

		if (exporter == null) {

			LOGGER.info("\"" + serviceName + "\" does not appear to be in the already stopped server list. HENCE IT CANNOT BE STOPPED!");

		} else {

			exporter.afterPropertiesSet();
			InfraRmiServiceExporter.STOPPED_SERVERS.remove(serviceName);

		}

	}

	/**
	 * Re-start all the rmi servers that are stopped in this process.
	 *
	 * @throws java.rmi.RemoteException
	 */
	public static void restartAllRMIServers() throws RemoteException {

		Set<String> serviceNames = InfraRmiServiceExporter.STOPPED_SERVERS.keySet();

		for (String serviceName : serviceNames) {
			restartRMIServer(serviceName);
		}

	}

	/**
	 * Stop all the registered in this process.
	 */
	public static void stopAllRMIServers() {

		try {

			final Configuration updatedConfiguration = ConfigurationFactory.getConfiguration();

			final int port = updatedConfiguration.getInt(HighAvailabilityConstants.DEFAULT_PORT);

			final Registry registry = getRegistry(port);

			Set<String> keys = InfraRmiServiceExporter.SERVICE_NAMES.keySet();

			for (String serviceName : keys) {
				stopRmiServer(serviceName, port, registry);
			}

		} catch (Exception e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.warn("could not un register a service. Exception is: " + e.toString(), e);
			} else {
				LOGGER.warn("could not un register a service. Exception is: " + e.toString());
			}
		}

	}

	public static void stopServerRecoveryThread(final String serviceName) {

		InfraRmiServiceExporter exporter = InfraRmiServiceExporter.SERVICE_NAMES.get(serviceName);
		if (exporter != null) {
			ServerRecoveryDaemon daemon = exporter.getDaemon();
			if (daemon != null) {
				if (daemon.isAlive()) {
					daemon.setIsStopping(true);
				}
			} else {
				LOGGER.debug("Cannot find a Daemon thread for RMI server under the name: " + serviceName);
			}
		} else {
			LOGGER.warn("Cannot find a RMI server under the name: " + serviceName);
		}

	}

	/**
	 * stop a specific RMI server.
	 * 
	 * @param serviceName
	 */
	public static void stopRMIServer(final String serviceName) {
		try {
			final Configuration configuration = ConfigurationFactory.getConfiguration();

			final int port = configuration.getInt(HighAvailabilityConstants.DEFAULT_PORT);

			final Registry registry = getRegistry(port);

			if (registry == null) {
				LOGGER.warn("Cannot stop RMI Server: " + serviceName + " As the registry process is not running!");
			} else {
				stopRmiServer(serviceName, port, registry);
			}
		} catch (Exception e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("could not un register the service: " + serviceName + ". Exception is: " + e.toString());
			}
		}
	}

	private static void stopRmiServer(final String serviceName, final int port, final Registry registry) throws RemoteException, NotBoundException, AccessException, NoSuchObjectException {

		registry.unbind(serviceName);

		unExportRemoteObject(serviceName);

		LOGGER.info("un registered service: " + serviceName + ", from port: " + port);

		InfraRmiServiceExporter exporter = InfraRmiServiceExporter.SERVICE_NAMES.remove(serviceName);
		InfraRmiServiceExporter.STOPPED_SERVERS.put(serviceName, exporter);
	}

	/**
	 * @param port
	 *            - the registry port
	 * @return
	 */
	private static Registry getRegistry(final int port) {

		try {
			// try to get existing registry
			final Registry registry = LocateRegistry.getRegistry(port);

			// test the registry
			registry.list();

			return registry;
		} catch (RemoteException ex) {
			// if registry does not exist, return null.
			return null;
		}
	}

	/**
	 * kill the rmiregistry when in windows only. This method should be called
	 * from unit tests only, as if not called may cause the junit process to
	 * stay "hang". call this method from your tearDown or @AfterClass method.
	 */
	public static void killRmiRegistry() {

		final String osName = System.getProperty("os.name").toLowerCase(Locale.getDefault());
		if (osName.contains("windows")) {
			try {
				Process process = new ProcessBuilder("taskkill", "/F", "/IM", "rmiregistry.exe").start();
				int rc = process.waitFor();
				if (rc != 0) {
					LOGGER.info("ERROR: kill rmiregistry returned with code: " + rc);
				}
			} catch (IOException e) {
				LOGGER.info("ERROR: cannot kill rmiregistry: " + e.toString());
			} catch (InterruptedException e) {
				LOGGER.info("ERROR: cannot kill rmiregistry: " + e.toString());
			}
		}
	}

	private static void unExportRemoteObject(final String serviceName) throws NoSuchObjectException {

		final InfraRmiServiceExporter exporter = InfraRmiServiceExporter.SERVICE_NAMES.get(serviceName);
		if (exporter == null) {
			LOGGER.info("cannot un export service: " + serviceName + " as is does not exist in started services list.");
		} else {
			final Remote remote = exporter.getExportedObject();
			UnicastRemoteObject.unexportObject(remote, true);
		}
	}

}
