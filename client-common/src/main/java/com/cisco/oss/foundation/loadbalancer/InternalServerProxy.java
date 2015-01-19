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

    private transient boolean active = true;

    // the time to wait while inactive before re activating. Default: do not
    // wait.
    private transient long waitingTime = -1;

    // the time this server proxy instance was passivated.
    private transient long passivationTimeStamp;

    private final AtomicBoolean reactivated = new AtomicBoolean(true);

    // private AtomicBoolean notify = new AtomicBoolean(true);

    private long failedAttemptTimeStamp;
    private int maxNumberOfAttempts = -1;
    private long retryDelay = -1;
    private int currentNumberOfAttempts;
    private String host;
    private Integer port;
    private String clientName;

    /**
     * InternalServer ctor.
     *
     * @param waitingTime         - the time to wait while inactive before re activating.
     * @param clientName
     */
    public InternalServerProxy(final long waitingTime, String clientName) {
        this.waitingTime = waitingTime;
        this.clientName = clientName;
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
            LOGGER.warn("Invocation of service '{}' at [{}:{}] has failed {} consecutive times. [{}:{}] will be blacklisted for {} milliseconds", clientName, getHost(), getPort(), getMaxNumberOfAttempts(), getHost(), getPort(), waitingTime);

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
                setCurrentNumberOfAttempts(0);

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
    public int getMaxNumberOfAttempts() {
        return maxNumberOfAttempts;
    }

    /**
     * set the maximum number of times to retry invoking a server before it is
     * passivated.
     *
     * @param numberOfRetries the number of times to retry invoking a server before it is
     *                           passivated.
     */
    public void setMaxNumberOfAttempts(final int numberOfRetries) {
        this.maxNumberOfAttempts = numberOfRetries;
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
    public synchronized int getCurrentNumberOfAttempts() {
        return currentNumberOfAttempts;
    }

    /**
     * set the number of times the server was invoked until now.
     *
     * @param currentNumberOfAttempts the number of times the server was invoked until now.
     */
    public synchronized void setCurrentNumberOfAttempts(final int currentNumberOfAttempts) {
        this.currentNumberOfAttempts = currentNumberOfAttempts;
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
        currentNumberOfAttempts++;
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

    @Override
    public String toString() {
        return "InternalServerProxy{" +
                "clientName='" + clientName + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalServerProxy)) return false;

        InternalServerProxy that = (InternalServerProxy) o;

        if (!clientName.equals(that.clientName)) return false;
        if (!host.equals(that.host)) return false;
        if (!port.equals(that.port)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port.hashCode();
        result = 31 * result + clientName.hashCode();
        return result;
    }
}
