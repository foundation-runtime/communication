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

import java.io.Serializable;
import java.util.List;

/**
 * The strategy is in charge of handling the way servers are being called. There are
 * different types of load balancing strategies including FailOver,RoundRobin and ActiveActive.
 * The Strategy is a member of the orchestrator. hence, there is a new instance for each client.
 *
 * @author Yair Ogen
 */
public interface LoadBalancerStrategy<S extends ClientRequest> extends Serializable {
	
	static final ThreadLocal<Throwable> lastKnownErrorThreadLocal = new ThreadLocal<Throwable>();

    /**
     * set a list of objects to be invoked by the orchestrator. each object
     * represents a server instance.
     *
     * @param serverProxies list of server objects to be invoked by the orchestrator.
     */
    void setServerProxies(List<InternalServerProxy> serverProxies);


//    /**
//     * invoke the actual method on the server object using reflection.
//     *
//     * @param invocation the MethodInvocation object that holds reflection data.
//     * @return the result of the invocation. This object will hold the actual result from the object being invoked.
//     * @throws Throwable any exception
//     */
//    Object invoke(MethodInvocation invocation) throws Throwable;

    InternalServerProxy getServerProxy(S request);

    void handleNullserverProxy(final String apiName, final Throwable lastCaugtException);
    
    List<InternalServerProxy> getServerProxies();

    Throwable handleException(final String apiName, final InternalServerProxy serverProxy, final Throwable throwable) ;

    public static enum STRATEGY_TYPE {

        /**
         * The fail over strategy.
         */
        FAIL_OVER("failOverStrategy"),

        /**
         * The round robin strategy.
         */
        ROUND_ROBIN("roundRobinStrategy"),

        /**
         * The sticky round robin strategy.
         */
        STICKY_ROUND_ROBIN("stickyRoundRobinStrategy");


        private String type;

        private STRATEGY_TYPE(final String type) {
            this.type = type;
        }

        private String getType() {
            return type;
        }


//        public static STRATEGY_TYPE realValueOf(final String logicalName) {
//            for (STRATEGY_TYPE type : STRATEGY_TYPE.values()) {
//                if (type.getType().equalsIgnoreCase(logicalName)) {
//                    return type;
//                }
//            }
//            return STRATEGY_TYPE.ROUND_ROBIN;
//        }
    }


}
