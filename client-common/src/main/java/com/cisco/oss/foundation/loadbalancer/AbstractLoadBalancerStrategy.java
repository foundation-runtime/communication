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

package com.cisco.oss.foundation.loadbalancer;

import com.cisco.oss.foundation.directory.LookupManager;
import com.cisco.oss.foundation.directory.NotificationHandler;
import com.cisco.oss.foundation.directory.ServiceDirectory;
import com.cisco.oss.foundation.directory.entity.ServiceInstance;
import com.cisco.oss.foundation.directory.exception.ServiceException;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This Class is the abstract implementation of the load balancing strategy.
 *
 * @author Yair Ogen
 */
public abstract class AbstractLoadBalancerStrategy<S extends ClientRequest> implements LoadBalancerStrategy<S> {

    public static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLoadBalancerStrategy.class);
    private static final long serialVersionUID = -4787963395573781601L;
    protected List<InternalServerProxy> serverProxies;
    long waitingTime;
    String clientName;
    long retryDelay;
    int numberOfAttempts;
    private String serviceName = "UNKNOWN";
    private boolean serviceDirectoryEnabled = false;
    private LookupManager lookupManager = null;

    public AbstractLoadBalancerStrategy(String serviceName, boolean serviceDirectoryEnabled, long waitingTime, String clientName, long retryDelay, int numberOfAttempts) {
        this.serviceName = serviceName;
        this.serviceDirectoryEnabled = serviceDirectoryEnabled;
        this.waitingTime = waitingTime;
        this.clientName = clientName;
        this.retryDelay = retryDelay;
        this.numberOfAttempts = numberOfAttempts;
        if (isServiceDirectoryEnabled()) {
            initSDLookupManager();
        }
    }

    private void initSDLookupManager() {
        try {
            lookupManager = ServiceDirectory.getLookupManager();
            lookupManager.addNotificationHandler(serviceName, new NotificationHandler() {
                @Override
                public void serviceInstanceAvailable(ServiceInstance serviceInstance) {
                    String host = serviceInstance.getAddress();
                    int port = serviceInstance.getPort();
                    InternalServerProxy internalServerProxy = createInternalServerProxy(host, port);

                    AbstractLoadBalancerStrategy.readWriteLock.writeLock().lock();
                    try {
                        serverProxies.add(internalServerProxy);
                    } finally {
                        AbstractLoadBalancerStrategy.readWriteLock.writeLock().unlock();
                    }

                    LOGGER.info(internalServerProxy + " is now available");

                }

                @Override
                public void serviceInstanceUnavailable(ServiceInstance serviceInstance) {
                    String host = serviceInstance.getAddress();
                    int port = serviceInstance.getPort();
                    InternalServerProxy internalServerProxy = createInternalServerProxy(host, port);
                    AbstractLoadBalancerStrategy.readWriteLock.writeLock().lock();
                    try {
                        serverProxies.remove(internalServerProxy);
                    } finally {
                        AbstractLoadBalancerStrategy.readWriteLock.writeLock().unlock();
                    }
                    LOGGER.info(internalServerProxy + " is now un-available");
                }

                @Override
                public void serviceInstanceChange(ServiceInstance serviceInstance) {

                }
            });
        } catch (ServiceException e) {
            LOGGER.error("can't init SD lookup manager: {}", e);
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isServiceDirectoryEnabled() {
        return serviceDirectoryEnabled;
    }

    public Throwable handleException(final String apiName, final InternalServerProxy serverProxy, final Throwable throwable) {

        // if the caught exception is of timeout nature, audit and throw a new
        // RequestTimeoutException directly to the client
        handleTimeout(apiName, throwable);

        // in these cases only, throw the exception to the caller:
        // 1. if the exception is instanceof NoActiveServersIOException - used
        // in UDP support.
        // 2. if not instanceof RemoteAccessException and not instanceof
        // IOException - in these cases continue trying with the next servers.
        String hostPort = serverProxy != null ? " at [" + serverProxy.getHost() + ":" + serverProxy.getPort() + "]" : "";
        String errorMessage = "Failed to invoke  '" + apiName + "' " + hostPort;
        String warnMessage = "Error occurred while invoking '" + apiName + "' " + hostPort;

        if (throwable instanceof UnresolvedAddressException) {
            LOGGER.debug("retrying in special case of 'UnresolvedAddressException'");
        } else {

//			final boolean firstInChain = ConfigurationFactory.getConfiguration().getBoolean(LoadBalancerConstants.DEFAULT_FIRST_IN_CHAIN);

            // don't try and find another active server if:
            // 1. the exception you caught is NoActiveServersDeadEndException
            // 2. the exception you caught is NoActiveServersIOException
            // 3. the exception you caught is (not RemoteAccessException) and
            // (not IOException) and (not NoActiveServersException but is
            // firstInChain)
            // if you caught NoActiveServersException but you are NOT
            // firstInChain - then throw back error and don't try to reconnect
            if (throwable instanceof NoActiveServersDeadEndException || throwable instanceof NoActiveServersIOException
                    || (!(throwable instanceof RemoteAccessException) && !(throwable instanceof NoActiveServersException) && !(throwable instanceof IOException) && (throwable.getCause() != null && throwable.getCause().getCause() != null && !(throwable.getCause().getCause() instanceof IOException)) && (throwable.getCause() != null && !(throwable.getCause() instanceof IOException)) || (/*!firstInChain &&*/ throwable instanceof NoActiveServersException))) {
                LOGGER.error(errorMessage, throwable);
                throw new ClientException(throwable.toString(), throwable);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            // LOGGER.warn(errorMessage + ". Exception is: " + throwable);
            LOGGER.warn(warnMessage + "(attempt " + serverProxy.getCurrentNumberOfAttempts() + ", will retry in " + serverProxy.getRetryDelay() + " milliseconds)", throwable);
        } else {
            LOGGER.warn(warnMessage + "(attempt " + serverProxy.getCurrentNumberOfAttempts() + ", will retry in " + serverProxy.getRetryDelay() + " milliseconds)");
        }

        serverProxy.processFailureAttempt();

        if (serverProxy.getCurrentNumberOfAttempts() >= serverProxy.getMaxNumberOfAttempts()) {
            serverProxy.passivate();
        }

        return throwable;
    }

    private void handleTimeout(final String apiName, final Throwable throwable) {
        if (throwable instanceof RequestTimeoutException) {
            throw (RequestTimeoutException) throwable;
        } else if (throwable instanceof SocketTimeoutException || (throwable != null && throwable.getMessage() != null && (throwable.getMessage().contains("Inactivity timeout passed during read operation") || (throwable.getCause() instanceof SocketTimeoutException)))) {

            final RequestTimeoutException requestTimeoutException = new RequestTimeoutException("Error occurred while invoking the api: " + apiName, throwable.getCause());

            LOGGER.warn(requestTimeoutException.toString(), requestTimeoutException);

            throw requestTimeoutException;
        }
    }

    public void handleNullserverProxy(final String apiName, final Throwable lastCaugtException) {

//		final Serializable interfaceName = serviceMethod != null ? serviceMethod.getDeclaringClass() : "UNKNOWN";
//		final String methodName = serviceMethod != null ? serviceMethod.getName() : "UNKNOWN";
        final String causedBy = lastCaugtException != null ? ("\"Caused by: " + lastCaugtException) : "\"";
        final NoActiveServersException noActiveServersException = new NoActiveServersException("No active servers were found in the server proxies list. API: \"" + apiName + "\"." + causedBy, lastCaugtException);

        if (this instanceof FailOverStrategy) {
            FailOverStrategy failOverStrategy = (FailOverStrategy) this;
            failOverStrategy.lastActive = null;
        }

//		// drill down to see if the applicative interface declares throwing an
//		// IOException.
//		// if so, we want to throw an IOException and not a RuntimeException.
//		if (serviceMethod != null) {
//
//			final Class<?>[] exceptionTypes = serviceMethod.getExceptionTypes();
//			for (Class<?> exceptionClass : exceptionTypes) {
//				if (exceptionClass.isAssignableFrom(IOException.class)) {
//					// if lastCaugtException == null, this message will be used
//					// because it is most likely to be a configuration problem.
//					final String message = "Can not find a responding server, OR your config prefix may be wrong.";
//					final String errorMessage = noActiveServersException.getMessage() + "\nCaused by: " + (lastCaugtException == null ? message : lastCaugtException);
//					final NoActiveServersIOException noActiveSrvIOException = new NoActiveServersIOException(errorMessage);
//					throw noActiveSrvIOException;
//				}
//			}
//		}

        // if you are the first in chain and you don't have ana available server
        // throw this new exception so the loop of trying to find an active
        // server will end.
//		final boolean firstInChain = ConfigurationFactory.getConfiguration().getBoolean(LoadBalancerConstants.DEFAULT_FIRST_IN_CHAIN);
//		if (firstInChain) {
//			throw new NoActiveServersDeadEndException(noActiveServersException.getMessage(), lastCaugtException);
//		}

        throw noActiveServersException;

    }

    public List<InternalServerProxy> getServerProxies() {
        if ((serverProxies == null || serverProxies.isEmpty()) && isServiceDirectoryEnabled()) {
            if (lookupManager == null) {
                initSDLookupManager();
            }

            if (lookupManager != null) {

                try {
                    // Look up all ServiceInstances of the Service.
                    List<ServiceInstance> allServiceInstances = lookupManager.getAllInstances(serviceName);

                    List<InternalServerProxy> serversList = Lists.newCopyOnWriteArrayList();


                    for (ServiceInstance serviceInstance : allServiceInstances) {
                        String host = serviceInstance.getAddress();
                        int port = serviceInstance.getPort();
                        final InternalServerProxy internalServerProxy = createInternalServerProxy(host, port);
                        serversList.add(internalServerProxy);
                    }

                    AbstractLoadBalancerStrategy.readWriteLock.writeLock().lock();
                    try {

                        serverProxies = serversList;


                    } finally {
                        AbstractLoadBalancerStrategy.readWriteLock.writeLock().unlock();
                    }
                } catch (ServiceException e) {
                    throw new NoActiveServersException("can't get server instance from service directory server. error is: " + e, e);
                }


            } else {
                throw new NoActiveServersException("no Service Directory lookup manager - can't get active servers", null);
            }

        }
        return serverProxies;
    }

    @Override
    public void setServerProxies(final List<InternalServerProxy> serverProxies) {
        this.serverProxies = serverProxies;
    }

    private InternalServerProxy createInternalServerProxy(String host, int port) {
        final InternalServerProxy internalServerProxy = new InternalServerProxy(waitingTime, clientName);
        internalServerProxy.setRetryDelay(retryDelay);
        internalServerProxy.setMaxNumberOfAttempts(numberOfAttempts);
        internalServerProxy.setHost(host);
        internalServerProxy.setPort(port);
        return internalServerProxy;
    }


}
