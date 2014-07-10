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

/**
 *
 */
package com.cisco.oss.foundation.loadbalancer;

/**
 * @author Yair Ogen
 */
public final class LoadBalancerConstants {

    // configuration parameters constants.
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String INNER_PORT = "innerPort";
    public static final String SERVER = "serverName";
    public static final String READ_TIME_OUT = "readTimeout";
    public static final String WRITE_TIME_OUT = "writeTimeout";
    public static final String CONNECT_TIME_OUT = "connectTimeout";
    public static final String WAITING_TIME = "waitingTime";
    public static final String NUMBER_OF_ATTEMPTS= "numberOfAttempts";
    public static final String RETRY_DELAY = "retryDelay";
    public static final String FOLLOW_REDIRECTS = "followRedirects";
    public static final String SERVICE_NAME = "serviceName";
    public static final String STRATEGY = "strategy";
    public static final String RETRY_ON_TIMEOUT = "retryOnTimeout";
    public static final String RUN_SERVER_RECOVERY = "runServerRecoveryDaemon";
    public static final String RUN_SERVER_RECOVERY_TIME = "serverRecoveryDaemonTimeInterval";
    public static final String X_FORWARD_FOR_HEADER = "x-forwarded-for";
    public static final int DEFAULT_CONNECT_TIMEOUT = 2500;
    public static final int DEFAULT_READ_TIMEOUT = 5000;
    public static final int DEFAULT_WAITING_TIME = 60000;
    public static final int DEFAULT_NUMBER_OF_ATTEMPTS = 3;
    public static final int DEFAULT_RETRY_DELAY = 1500;

    public static final String FLOW_CONTEXT_HEADER = "FLOW_CONTEXT";
    public static final String IDLE_TIME_OUT = "idleTimeout";
    public static final int DEFAULT_IDLE_TIMEOUT = 180000;

    public static final String KEYSTORE_PATH = "keyStorePath";
    public static final String KEYSTORE_PASSWORD = "keyStorePassword";
    public static final String TRUSTSTORE_PATH = "trustStorePath";
    public static final String TRUSTSTORE_PASSWORD = "trustStorePassword";

    public static final int DEFAULT_MAX_CONNECTIONS_PER_ADDRESS = 1000;
    public static final String MAX_CONNECTIONS_PER_ADDRESS = "http.maxConnectionsPerAddress";
    public static final String MAX_CONNECTIONS_TOTAL = "http.maxConnectionsTotal";
    public static final int DEFAULT_MAX_CONNECTIONS_TOTAL = 3000;
    public static final String MAX_QUEUE_PER_ADDRESS = "http.maxQueueSizePerAddress";
    public static final int DEFAULT_MAX_QUEUE_PER_ADDRESS = 1000;
    public static final String AUTO_CLOSEABLE = "autoCloseable";

    private LoadBalancerConstants() {
        // prevent instantiation
    }
}
