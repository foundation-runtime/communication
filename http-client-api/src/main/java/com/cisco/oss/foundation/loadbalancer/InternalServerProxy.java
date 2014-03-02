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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents an Internal Server. The server can be of different
 * types of implementations depending on the channel used. Example list of
 * possible channels: UDP, RMI, HTTP etc.
 *
 * @author Yair Ogen
 */
public class InternalServerProxy {

    private static final String THIS_INSTANCE_IS = ". This instance is: ";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(InternalServerProxy.class);

    private static final Logger AUDITOR = LoggerFactory.getLogger("audit." + InternalServerProxy.class.getName());

    private transient boolean active = true;

    // the time to wait while inactive before re activating. Default: do not
    // wait.
    private transient long waitingTime = -1;

    // the time this server proxy instance was passivated.
    private transient long passivationTimeStamp;

    private final AtomicBoolean reactivated = new AtomicBoolean(true);

    // private AtomicBoolean notify = new AtomicBoolean(true);

    private long failedAttemptTimeStamp;
    private int maxNumberOfRetries = -1;
    private long retryDelay = -1;
    private int currentNumberOfRetries;
    private String host;
    private Integer port;
    private String serviceName;

    /**
     * InternalServer ctor.
     *
     * @param waitingTime         - the time to wait while inactive before re activating.
     * @param serviceName 
     */
    public InternalServerProxy(final long waitingTime, String serviceName) {
        this.waitingTime = waitingTime;
        this.serviceName = serviceName;
    }

    /**
     * passivate the server causing it to be inactive.
     */

    public synchronized void passivate() {
        if (active) {
            active = false;
            getReactivated().set(true);
            passivationTimeStamp = System.currentTimeMillis();

//            LoggingHelper.warn(AUDITOR, "Lost connection to host: [%s], port [%s]", getHost(), getPort());
            AUDITOR.warn("Invocation of service '{}' at [{}:{}] has failed {} consecutive times. [{}:{}] will be blacklisted for {} milliseconds", serviceName, getHost(), getPort(),getMaxNumberOfRetries(), getHost(), getPort(), waitingTime);

            if (LOGGER.isDebugEnabled()) {
                final Date passTimeAndWaitTime = new Date(passivationTimeStamp);
                LOGGER.debug("Passivated time is: {}", (passTimeAndWaitTime + THIS_INSTANCE_IS + this));
            }
        }
    }

    /**
     * re activate the server if it is down. If the current time is bigger than
     * the time the server was passivated plus the waiting time the server will
     * be activated.
     *
     * @return true if activated.
     */
    public synchronized boolean activate() {

        final long currentTimeInMilis = System.currentTimeMillis();
        final long passTimeAndWaitTimeMilis = passivationTimeStamp + waitingTime;

        // if is currently passivated.
        if (!active) {

            if (passTimeAndWaitTimeMilis < currentTimeInMilis) {
                // reset counters before re activating.
                setFailedAttemptTimeStamp(0);
                setCurrentNumberOfRetries(0);

                LOGGER.info("Attempting to reconnect to [{}:{}]", getHost(), getPort());
                active = true;
            }

        } else if ((failedAttemptTimeStamp > 0 && retryDelay > 0) && (retryDelay > (currentTimeInMilis - failedAttemptTimeStamp))) {

            // if retry delay is bigger than the time passed between now and
            // the last failed attempt, then return false to go to the next
            // server.

            try {
                final long timeToWait = retryDelay - (currentTimeInMilis - failedAttemptTimeStamp);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("retry delay is bigger than currentTimeInMilis - failedAttemptTimeStamp. continuing to next server! Time to wait is: " + timeToWait + THIS_INSTANCE_IS + this);
                    LOGGER.debug("sleeping for: " + timeToWait + " on server instance. This instance is: " + this);
                }
                Thread.sleep(timeToWait);

            } catch (InterruptedException interruptedException) {
                LOGGER.error("therad was interrupted. exception is: " + interruptedException);
            }
        }

        return active;
    }

    /**
     * get the time stamp for the last time a failure occurred while activating
     * the server.
     *
     * @return the time stamp for the last time a failure occurred while
     *         activating the server.
     */
    public synchronized long getFailedAttemptTimeStamp() {
        return failedAttemptTimeStamp;
    }

    /**
     * set the time stamp for the last time a failure occurred while activating
     * the server.
     *
     * @param failedAttemptTimeStamp the time stamp for the last time a failure occurred while
     *                               activating the server.
     */
    public synchronized void setFailedAttemptTimeStamp(final long failedAttemptTimeStamp) {
        this.failedAttemptTimeStamp = failedAttemptTimeStamp;
    }

    /**
     * get the number of times to retry invoking a server before it is
     * passivated.
     *
     * @return the number of times to retry invoking a server before it is
     *         passivated.
     */
    public int getMaxNumberOfRetries() {
        return maxNumberOfRetries;
    }

    /**
     * set the maximum number of times to retry invoking a server before it is
     * passivated.
     *
     * @param numberOfRetries the number of times to retry invoking a server before it is
     *                           passivated.
     */
    public void setMaxNumberOfRetries(final int numberOfRetries) {
        this.maxNumberOfRetries = numberOfRetries;
    }

    /**
     * get the delay period in milliseconds between attempts of re-invoking a
     * failing server.
     *
     * @return the delay period in milliseconds between attempts of re-invoking
     *         a failing server.
     */
    public synchronized long getRetryDelay() {
        return retryDelay;
    }

    /**
     * set the delay period in milliseconds between attempts of re-invoking a
     * failing server.
     *
     * @param retryDelay the delay period in milliseconds between attempts of
     *                   re-invoking a failing server.
     */
    public synchronized void setRetryDelay(final long retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * get the number of times the server was invoked until now.
     *
     * @return the number of times the server was invoked until now.
     */
    public synchronized int getCurrentNumberOfRetries() {
        return currentNumberOfRetries;
    }

    /**
     * set the number of times the server was invoked until now.
     *
     * @param currentNumberOfRetries the number of times the server was invoked until now.
     */
    public synchronized void setCurrentNumberOfRetries(final int currentNumberOfRetries) {
        this.currentNumberOfRetries = currentNumberOfRetries;
    }

    /**
     * get the active state flag, with out any logic (opposite to activate())
     *
     * @return the active state flag, with out any logic (opposite to
     *         activate())
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * process actions when an attempt to access server has failed. increment
     * the current amount of retries and update the last failure time stamp.
     */
    public synchronized void processFailureAttempt() {
        setFailedAttemptTimeStamp(System.currentTimeMillis());
        currentNumberOfRetries++;
    }

    /**
     * get the host of this instance server.
     *
     * @return the host of this instance server.
     */
    public String getHost() {
        return host;
    }

    /**
     * set the host of this instance server.
     *
     * @param host the host of this instance server.
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * get the port of this instance server.
     *
     * @return the port of this instance server.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * set the port of this instance server.
     *
     * @param port the port of this instance server.
     */
    public void setPort(final Integer port) {
        this.port = port;
    }

    /**
     * @return the reactivated
     */
    public AtomicBoolean getReactivated() {
        return reactivated;
    }

}
