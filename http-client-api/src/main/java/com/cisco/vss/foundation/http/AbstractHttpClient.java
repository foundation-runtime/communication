package com.cisco.vss.foundation.http;

import com.cisco.vss.foundation.configuration.CabConfigurationListener;
import com.cisco.vss.foundation.configuration.CabConfigurationListenerRegistry;
import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.loadbalancer.*;
import com.google.common.collect.Lists;
import com.netflix.util.Pair;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract implementation common to all known HttpClient implementations
 * Created by Yair Ogen on 1/16/14.
 */
public abstract class AbstractHttpClient<S extends HttpRequest, R extends HttpResponse> implements HttpClient<S, R> {

    public static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpClient.class);
    protected LoadBalancerStrategy loadBalancerStrategy = new RoundRobinStrategy();
    protected String apiName = "HTTP";
    protected InternalServerProxyMetadata metadata;
    protected Configuration configuration;
    protected boolean enableLoadBalancing = true;

    public AbstractHttpClient(String apiName, Configuration configuration, boolean enableLoadBalancing) {
        this(apiName, LoadBalancerStrategy.STRATEGY_TYPE.ROUND_ROBIN, configuration, enableLoadBalancing);
    }

    public AbstractHttpClient(String apiName, LoadBalancerStrategy.STRATEGY_TYPE strategyType, Configuration configuration, boolean enableLoadBalancing) {
        this.apiName = apiName;
        this.enableLoadBalancing = enableLoadBalancing;
        if(configuration == null){
            this.configuration = ConfigurationFactory.getConfiguration();
        }else{
            this.configuration = configuration;
        }
        loadBalancerStrategy = fromHighAvailabilityStrategyType(strategyType);
        createServerListFromConfig();
        CabConfigurationListenerRegistry.addCabConfigurationListener(new LoadBalancerConfigurationListener());
    }

    private LoadBalancerStrategy fromHighAvailabilityStrategyType(LoadBalancerStrategy.STRATEGY_TYPE strategyType){
        switch (strategyType) {
            case FAIL_OVER:
                return new FailOverStrategy();
            case STICKY_ROUND_ROBIN:
                return new StickyRoundRobinStrategy();
            default:
                return new RoundRobinStrategy();
        }
    }

    @Override
    public R executeWithLoadBalancer(S request) {

        Throwable lastCaugtException = null;
        boolean successfullyInvoked = false;
        R result = null;
        // use a do while loop we need at least one attempt, to know if
        // the invocation was successful or not.
        // the stopping condition is when successfullyInvoked = true.
        do {

            InternalServerProxy serverProxy = null;

            try {
                readWriteLock.readLock().lock();
                serverProxy = loadBalancerStrategy.getServerProxy(request);
            } finally {
                readWriteLock.readLock().unlock();
            }

            if (serverProxy == null) {
                // server proxy will be null if the configuration was not
                // configured properly
                // or if all the servers are passivated.
                loadBalancerStrategy.handleNullserverProxy(apiName, lastCaugtException);
            }

            try {

                request = updateRequestUri(request, serverProxy);

                LOGGER.info("sending request: {}", request.getUri());
                result = executeDirect(request);
                LOGGER.info("got response: {}", result.getRequestedURI());
//                if (lastKnownErrorThreadLocal.get() != null) {
//                    lastCaugtException = handleException(serviceMethod, serverProxy, lastKnownErrorThreadLocal.get());
//                } else {
                serverProxy.setCurrentNumberOfRetries(0);
                serverProxy.setFailedAttemptTimeStamp(0);
//                }

                successfullyInvoked = true;

            } catch (Throwable e) {
                lastCaugtException = loadBalancerStrategy.handleException(apiName, serverProxy, e);
            }
        } while (!successfullyInvoked);

        return result;
    }

    protected S updateRequestUri(S request, InternalServerProxy serverProxy) {

        URI origUri = request.getUri();
        String host = serverProxy.getHost();
        String scheme = origUri.getScheme() == null ? "http" : origUri.getScheme();
        int port = serverProxy.getPort();

        String urlPath = "";
        if (origUri.getRawPath() != null && origUri.getRawPath().startsWith("/")) {
            urlPath = origUri.getRawPath();
        } else {
            urlPath = "/" + origUri.getRawPath();
        }

        URI newURI = null;
        try {
            newURI = new URI(scheme, origUri.getUserInfo(), host, port, urlPath, origUri.getQuery(), origUri.getFragment());
        } catch (URISyntaxException e) {
            throw new ClientException(e.toString());
        }

        S req = (S) request.replaceUri(newURI);
//        try {
//            req = (S) this.clone();
//        } catch (CloneNotSupportedException e) {
//            throw new IllegalArgumentException(e);
//        }
//        req.uri = newURI;
        return req;
    }

    @Override
    public void executeWithLoadBalancer(S request, ResponseCallback<R> responseCallback) {
        execute(request, responseCallback, loadBalancerStrategy, apiName);
    }

//    @Override
//    public void setHighAvailabilityStrategy(LoadBalancerStrategy loadBalancerStrategy) {
//        this.loadBalancerStrategy = loadBalancerStrategy;
//        createServerListFromConfig();
//    }

    public abstract void execute(S request, ResponseCallback<R> responseCallback, LoadBalancerStrategy loadBalancerStrategy, String apiName);

    @Override
    public R execute(S request) {
        if(enableLoadBalancing){
            return executeWithLoadBalancer(request);
        }else{
            return executeDirect(request);
        }
    }

    protected abstract void configureClient();

    @Override
    public String getApiName() {
        return apiName;
    }

    private void createServerListFromConfig() {

        List<InternalServerProxy> serversList = Lists.newCopyOnWriteArrayList();

        metadata = loadServersMetadataConfiguration();

        // based on the data collected from the config file - updates the server
        // list
        AbstractLoadBalancerStrategy.readWriteLock.writeLock().lock();
        try {
            serversList = updateServerListBasedOnConfig(serversList, metadata);
        } finally {
            AbstractLoadBalancerStrategy.readWriteLock.writeLock().unlock();
        }

        if (serversList.isEmpty()) {
            LOGGER.debug("No hosts defined for api: \"" + apiName + "\". Please check your config files!");
        }

        loadBalancerStrategy.setServerProxies(serversList);

    }

    private InternalServerProxyMetadata loadServersMetadataConfiguration() {


        Configuration subset = configuration.subset(apiName);
        final Iterator<String> keysIterator = subset.getKeys();

        // read default values
        int readTimeout = subset.getInt("http." + LoadBalancerConstants.READ_TIME_OUT, LoadBalancerConstants.DEFAULT_READ_TIMEOUT);
        int connectTimeout = subset.getInt("http." + LoadBalancerConstants.CONNECT_TIME_OUT, LoadBalancerConstants.DEFAULT_CONNECT_TIMEOUT);
        long waitingTime = subset.getLong("http." + LoadBalancerConstants.WAITING_TIME, LoadBalancerConstants.DEFAULT_WAITING_TIME);
        int numberOfAttempts = subset.getInt("http." + LoadBalancerConstants.NUMBER_OF_ATTEMPTS, LoadBalancerConstants.DEFAULT_NUMBER_OF_ATTEMPTS);
        long retryDelay = subset.getLong("http." + LoadBalancerConstants.RETRY_DELAY, LoadBalancerConstants.DEFAULT_RETRY_DELAY);

        long idleTimeout = subset.getLong("http." + LoadBalancerConstants.IDLE_TIME_OUT, LoadBalancerConstants.DEFAULT_IDLE_TIMEOUT);
        int maxConnectionsPerAddress = subset.getInt("http." + LoadBalancerConstants.MAX_CONNECTIONS_PER_ADDRESS, LoadBalancerConstants.DEFAULT_MAX_CONNECTIONS_PER_ADDRESS);
        int maxConnectionsTotal = subset.getInt("http." + LoadBalancerConstants.MAX_CONNECTIONS_TOTAL, LoadBalancerConstants.DEFAULT_MAX_CONNECTIONS_TOTAL);
        int maxQueueSizePerAddress = subset.getInt("http." + LoadBalancerConstants.MAX_QUEUE_PER_ADDRESS, LoadBalancerConstants.DEFAULT_MAX_QUEUE_PER_ADDRESS);

        String keyStorePath = subset.getString("http." + LoadBalancerConstants.KEYSTORE_PATH, "");
        String keyStorePassword = subset.getString("http." + LoadBalancerConstants.KEYSTORE_PASSWORD, "");
        String trustStorePath = subset.getString("http." + LoadBalancerConstants.TRUSTSTORE_PATH, "");
        String trustStorePassword = subset.getString("http." + LoadBalancerConstants.TRUSTSTORE_PASSWORD, "");


        final List<String> keys = new ArrayList<String>();

        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            keys.add(key);
        }

        Collections.sort(keys);

        List<Pair<String, Integer>> hostAndPortPairs = new CopyOnWriteArrayList<Pair<String, Integer>>();

        for (String key : keys) {

            if (key.contains(LoadBalancerConstants.HOST)) {

                String host = subset.getString(key);

                // trim the host name
                if (StringUtils.isNotEmpty(host)) {
                    host = host.trim();
                }
                final String portKey = key.replace(LoadBalancerConstants.HOST, LoadBalancerConstants.PORT);
                if (subset.containsKey(portKey)) {
                    int port = subset.getInt(portKey);
                    // save host and port for future creation of server list
                    hostAndPortPairs.add(new Pair<String, Integer>(host, port));
                }
            }

        }

        InternalServerProxyMetadata metadata = new InternalServerProxyMetadata(readTimeout, connectTimeout, idleTimeout, maxConnectionsPerAddress, maxConnectionsTotal, maxQueueSizePerAddress, waitingTime, numberOfAttempts, retryDelay, hostAndPortPairs, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
//        metadata.getHostAndPortPairs().addAll(hostAndPortPairs);
//        metadata.setReadTimeout(readTimeout);
//        metadata.setConnectTimeout(connectTimeout);
//        metadata.setNumberOfRetries(numberOfAttempts);
//        metadata.setRetryDelay(retryDelay);
//        metadata.setWaitingTime(waitingTime);

        return metadata;

    }

    private InternalServerProxy createInternalServerProxy(final InternalServerProxyMetadata metadata, final String hostEntry, final int portEntry) {
        final InternalServerProxy internalServerProxy = new InternalServerProxy(metadata.getWaitingTime(), apiName);
        internalServerProxy.setRetryDelay(metadata.getRetryDelay());
        internalServerProxy.setMaxNumberOfRetries(metadata.getNumberOfRetries());
        internalServerProxy.setHost(hostEntry);
        internalServerProxy.setPort(portEntry);
        return internalServerProxy;
    }

    private List<InternalServerProxy> updateServerListBasedOnConfig(final List<InternalServerProxy> serversList, final InternalServerProxyMetadata metadata) {


        // iterate host and port pairs and create ad new servers to the server
        // list.
        for (Pair<String, Integer> hostPort : metadata.getHostAndPortPairs()) {

            final String hostEntry = hostPort.first();
            final int portEntry = hostPort.second();

            final InternalServerProxy internalServerProxy = createInternalServerProxy(metadata, hostEntry, portEntry);
            serversList.add(internalServerProxy);
        }

        return serversList;
    }

    /**
     * Listener for re-loading the internal list in case of dynamic configuration changes.
     */
    public class LoadBalancerConfigurationListener implements CabConfigurationListener {

        @Override
        public void configurationChanged() {

            LOGGER.debug("configuration has changed");

            List<InternalServerProxy> serverProxies = loadBalancerStrategy.getServerProxies();
            // List<InternalServerProxy> serverProxies = serverProxies2;
            InternalServerProxyMetadata metadata = loadServersMetadataConfiguration();

            List<InternalServerProxy> newServerProxies = Lists.newArrayListWithCapacity(serverProxies.size());

            List<Pair<String, Integer>> hostAndPortPairs = metadata.getHostAndPortPairs();
            for (Pair<String, Integer> hostPort : hostAndPortPairs) {

                String newHost = hostPort.first();
                int newPort = hostPort.second();

                boolean handled = false;

                for (InternalServerProxy serverProxy : serverProxies) {

                    String existingHost = serverProxy.getHost();
                    Integer existingPort = serverProxy.getPort();

                    if (existingHost.equals(newHost) && existingPort.equals(newPort)) {
                        handled = true;
                        newServerProxies.add(serverProxy);
                        break;
                    }
                }
                if (!handled) {

                    try {
                        final InternalServerProxy internalServerProxy = createInternalServerProxy(metadata, newHost, newPort);
                        newServerProxies.add(internalServerProxy);
                    } catch (Exception e) {
                        LOGGER.error("cannot update the internal server proxy list.", e);
                    }

                }
            }

            try {
                AbstractLoadBalancerStrategy.readWriteLock.writeLock().lock();
                if (loadBalancerStrategy.getServerProxies() == null) {
                    // loadBalancerStrategy was reloaded during
                    // configuration change.
                    // probably because there is a strategy parameter in
                    // ConfigurationFactory.getConfiguration().
                    // we need to reset loadBalancerStrategy parameters
                    loadBalancerStrategy.setServerProxies(newServerProxies);
                } else {
                    // just update the existing reference
                    loadBalancerStrategy.getServerProxies().clear();
                    loadBalancerStrategy.getServerProxies().addAll(newServerProxies);
                }
            } finally {
                AbstractLoadBalancerStrategy.readWriteLock.writeLock().unlock();
            }

        }

    }

}
