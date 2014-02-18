package com.cisco.vss.foundation.loadbalancer;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.ClientException;
import com.cisco.vss.foundation.http.HttpRequest;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteInvocationFailureException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.rmi.ServerError;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This Class is the abstract implementation of the load balancing strategy.
 * 
 * @author Yair Ogen
 */
public abstract class AbstractHighAvailabilityStrategy<S extends HttpRequest> implements HighAvailabilityStrategy<S> {

	private static final long serialVersionUID = -4787963395573781601L;


	protected static final Logger LOGGER = Logger.getLogger(AbstractHighAvailabilityStrategy.class);

	protected List<InternalServerProxy> serverProxies;

	public static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	public Throwable handleException(final String apiName, final InternalServerProxy serverProxy, final Throwable throwable)  {

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

//			final boolean firstInChain = ConfigurationFactory.getConfiguration().getBoolean(HighAvailabilityConstants.DEFAULT_FIRST_IN_CHAIN);

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
                throw new ClientException(throwable.toString(),throwable);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			// LOGGER.warn(errorMessage + ". Exception is: " + throwable);
			LOGGER.warn(warnMessage + "(attempt " + serverProxy.getCurrentNumberOfRetries() + ", will retry in " + serverProxy.getRetryDelay() + " milliseconds)", throwable);
		} else {
			LOGGER.warn(warnMessage + "(attempt " + serverProxy.getCurrentNumberOfRetries() + ", will retry in " + serverProxy.getRetryDelay() + " milliseconds)");
		}

		serverProxy.processFailureAttempt();

		if (serverProxy.getCurrentNumberOfRetries() >= serverProxy.getMaxNumberOfRetries()) {
			serverProxy.passivate();
		}

		return throwable;
	}

	private void handleTimeout(final String apiName, final Throwable throwable) {
        if (throwable instanceof RequestTimeoutException){
            throw (RequestTimeoutException)throwable;
        } else if (throwable instanceof SocketTimeoutException || (throwable != null && throwable.getMessage() != null && (throwable.getMessage().contains("Inactivity timeout passed during read operation") || (throwable.getCause() instanceof SocketTimeoutException)))) {

			final RequestTimeoutException requestTimeoutException = new RequestTimeoutException("Error occurred while invoking the api: " + apiName, throwable.getCause());

			LOGGER.warn(requestTimeoutException);

			throw requestTimeoutException;
		}
	}

	public void handleNullserverProxy(final String apiName, final Throwable lastCaugtException)  {

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
//		final boolean firstInChain = ConfigurationFactory.getConfiguration().getBoolean(HighAvailabilityConstants.DEFAULT_FIRST_IN_CHAIN);
//		if (firstInChain) {
//			throw new NoActiveServersDeadEndException(noActiveServersException.getMessage(), lastCaugtException);
//		}

		throw noActiveServersException;

	}


	@Override
	public void setServerProxies(final List<InternalServerProxy> serverProxies) {
		this.serverProxies = serverProxies;
	}

	public List<InternalServerProxy> getServerProxies() {
		return serverProxies;
	}



}
