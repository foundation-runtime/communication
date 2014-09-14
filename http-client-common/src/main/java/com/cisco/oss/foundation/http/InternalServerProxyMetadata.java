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

package com.cisco.oss.foundation.http;


import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * This class holds all the metadata for a given client configuration. see wiki for more info.
 * Created by Yair Ogen on 1/20/14.
 */
public class InternalServerProxyMetadata {

    private int readTimeout = 0;
    private int connectTimeout = 0;

    private long idleTimeout = 0;
    private int maxConnectionsPerAddress = 1000;
    private int maxConnectionsTotal = 3000;
    private int maxQueueSizePerAddress = 1000;
    private long waitingTime = 0;
    private int numberOfAttempts = 0;
    private long retryDelay = 0;
    private String keyStorePath = "";
    private String keyStorePassword = "";
    private String trustStorePath = "";
    private String trustStorePassword = "";
    private boolean staleConnectionCheckEnabled = false;
    private boolean autoCloseable = true;

    private List<Pair<String, Integer>> hostAndPortPairs = null;

    public boolean isAutoCloseable() {
        return autoCloseable;
    }

    public boolean isStaleConnectionCheckEnabled() {
        return staleConnectionCheckEnabled;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    private boolean followRedirects = false;

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public int getMaxConnectionsPerAddress() {
        return maxConnectionsPerAddress;
    }

    public int getMaxConnectionsTotal() {
        return maxConnectionsTotal;
    }

    public int getMaxQueueSizePerAddress() {
        return maxQueueSizePerAddress;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public InternalServerProxyMetadata(int readTimeout, int connectTimeout, long idleTimeout, int maxConnectionsPerAddress, int maxConnectionsTotal, int maxQueueSizePerAddress, long waitingTime, int numberOfAttempts, long retryDelay, List<Pair<String, Integer>> hostAndPortPairs, String keyStorePath, String keyStorePassword,String trustStorePath,String trustStorePassword, boolean followRedirects, boolean autoCloseable, boolean staleConnectionCheckEnabled) {
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.idleTimeout = idleTimeout;
        this.maxConnectionsPerAddress = maxConnectionsPerAddress;
        this.maxConnectionsTotal = maxConnectionsTotal;
        this.maxQueueSizePerAddress = maxQueueSizePerAddress;
        this.waitingTime = waitingTime;
        this.numberOfAttempts = numberOfAttempts;
        this.retryDelay = retryDelay;
        this.hostAndPortPairs = hostAndPortPairs;
        this.followRedirects = followRedirects;
        this.autoCloseable = autoCloseable;
        this.staleConnectionCheckEnabled = staleConnectionCheckEnabled;
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public List<Pair<String, Integer>> getHostAndPortPairs() {
        return hostAndPortPairs;
    }

}