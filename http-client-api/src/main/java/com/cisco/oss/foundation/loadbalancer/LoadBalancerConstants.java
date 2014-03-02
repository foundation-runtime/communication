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
    //    public static final String REP_EXST_BIND = "replaceExistingBinding";
    public static final String SERVICE_NAME = "serviceName";
    //    public static final String PORT_RANGE = "portRange";
    public static final String STRATEGY = "strategy";
    public static final String RETRY_ON_TIMEOUT = "retryOnTimeout";
    //    public static final String FIRST_IN_CHAIN = "firstInChain";
//    public static final String DEFAULT_UDP_KEY = "service.udp.";
//    public static final String DEFAULT_HTTP_KEY = "service.http.";
    public static final String RUN_SERVER_RECOVERY = "runServerRecoveryDaemon";
    public static final String RUN_SERVER_RECOVERY_TIME = "serverRecoveryDaemonTimeInterval";
    //    public static final String DEFAULT_UDP_TIME_OUT = DEFAULT_UDP_KEY + READ_TIME_OUT;
//    public static final String DEFAULT_HTTP_TIME_OUT = DEFAULT_HTTP_KEY + READ_TIME_OUT;
//    public static final String DEFAULT_HTTP_CONNECT_TIME_OUT = DEFAULT_HTTP_KEY + CONNECT_TIME_OUT;
//    public static final String DEFAULT_MAMA_TIME_OUT = DEFAULT_MAMA_KEY + READ_TIME_OUT;
//    public static final String DEFAULT_RMI_TIME_OUT = DEFAULT_KEY + READ_TIME_OUT;
//    public static final String DEFAULT_WRITE_TIME_OUT = DEFAULT_KEY + WRITE_TIME_OUT;
//    public static final String DEFAULT_CONNECT_TIME_OUT = DEFAULT_KEY + CONNECT_TIME_OUT;
//    public static final String DEFAULT_PORT = DEFAULT_KEY + PORT;
//    public static final String DEFAULT_HTTP_PORT = DEFAULT_HTTP_KEY + PORT;
//    public static final String DEFAULT_INNER_PORT = DEFAULT_KEY + INNER_PORT;
//    public static final String DEFAULT_SERVER = DEFAULT_KEY + SERVER;
//    public static final String DEFAULT_REP_EXST_BIND = DEFAULT_KEY + REP_EXST_BIND;
//    public static final String DEFAULT_PORT_RANGE = DEFAULT_KEY + PORT_RANGE;
//    public static final String DEFAULT_STRATEGY = DEFAULT_KEY + STRATEGY;
//    public static final String DEFAULT_RETRY_ON_TIMEOUT = DEFAULT_KEY + RETRY_ON_TIMEOUT;
//    public static final String DEFAULT_ENABLE_HEART_BEAT = DEFAULT_KEY + ENABLE_HEART_BEAT;
//    public static final String DEFAULT_FIRST_IN_CHAIN = DEFAULT_KEY + FIRST_IN_CHAIN;
//    public static final String IS_SNIFFER_ENABLED = DEFAULT_KEY + "rmiSnifferEnabled";
//    public static final String RMI_SNIFFER_FILTER = DEFAULT_KEY + "rmiSnifferFilter";
//    public static final String DEFAULT_RUN_SERVER_RECOVERY = DEFAULT_KEY + RUN_SERVER_RECOVERY;
//    public static final String DEFAULT_RUN_SERVER_RECOVERY_TIME = DEFAULT_KEY + RUN_SERVER_RECOVERY_TIME;
//    public static final String RMIREGSITRY_NUM_OF_RETRY = "rmiregistry.numberOfRetries";
//    public static final String RMIREGSITRY_RETRY_DELAY = "rmiregistry.retryDelay";
//    public static final String IS_MONITOR_ENABLED = DEFAULT_KEY + "exposeStatisticsToMonitor";
//    public static final String IS_UDP_MONITOR_ENABLED = DEFAULT_UDP_KEY + "exposeStatisticsToMonitor";
//	public static final String ENABLE_MULTI_HOST = "service.enableMultiHost";
//	public static final String JAVA_RMI_SERVER_NAME_PROPERTY = "java.rmi.server.hostname";

    public static final String X_FORWARD_FOR_HEADER = "x-forwarded-for";
    public static final int DEFAULT_CONNECT_TIMEOUT = 2500;
    public static final int DEFAULT_READ_TIMEOUT = 5000;
    public static final int DEFAULT_WAITING_TIME = 60000;
    public static final int DEFAULT_NUMBER_OF_ATTEMPTS = 3;
    public static final int DEFAULT_RETRY_DELAY = 1500;
//    public static final int LINGER = 0;
//    public static final int SOCKET_BUFFER_SIZE = 8192;
//    public static final boolean STALE_CHECKING_ENABLED = Boolean.FALSE;
//    public static final boolean TCP_NO_DELAY = Boolean.TRUE;
//    public static final boolean SO_REUSE_ADDR = Boolean.TRUE;

    public static final String FLOW_CONTEXT_HEADER = "FLOW_CONTEXT";
    public static final String SSL_CONTEXT_FACTORY = "sslContextFactory";
    public static final String INVOCATION_ENRICHER = "invocationEnricher";
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

    private LoadBalancerConstants() {
        // prevent instantiation
    }
}
