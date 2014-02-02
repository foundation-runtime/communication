package com.cisco.vss.foundation.http;

import com.netflix.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
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
    private int numberOfRetries = 0;
    private long retryDelay = 0;
    private List<Pair<String, Integer>> hostAndPortPairs = new ArrayList<Pair<String, Integer>>(3);

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

    public long getWaitingTime() {
        return waitingTime;
    }

    public InternalServerProxyMetadata(int readTimeout, int connectTimeout, long idleTimeout, int maxConnectionsPerAddress, int maxConnectionsTotal, int maxQueueSizePerAddress, long waitingTime, int numberOfRetries, long retryDelay, List<Pair<String, Integer>> hostAndPortPairs) {
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.idleTimeout = idleTimeout;
        this.maxConnectionsPerAddress = maxConnectionsPerAddress;
        this.maxConnectionsTotal = maxConnectionsTotal;
        this.maxQueueSizePerAddress = maxQueueSizePerAddress;
        this.waitingTime = waitingTime;
        this.numberOfRetries = numberOfRetries;
        this.retryDelay = retryDelay;
        this.hostAndPortPairs = hostAndPortPairs;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public List<Pair<String, Integer>> getHostAndPortPairs() {
        return hostAndPortPairs;
    }

}