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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the FailOverStrategy implementation. This implementation relies on an
 * active passive deployment mode. If one server is down all the requests will
 * be done with the other server. If they are both down an exception will be
 * thrown.
 *
 * @author Yair Ogen
 */
public class FailOverStrategy<S extends ClientRequest> extends AbstractLoadBalancerStrategy<S> {

    private static final long serialVersionUID = 6806126762594591923L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FailOverStrategy.class);

    private static final Logger AUDITOR = LoggerFactory.getLogger("audit." + FailOverStrategy.class.getName());

    private static final List<FailOverListener> failOverListeners = new ArrayList<FailOverListener>();

    // support last active model. If primary server is up but secondary still
    // responds continue to work with secondary.
    InternalServerProxy lastActive = null;

    private final AtomicBoolean firstTime = new AtomicBoolean(true);

    @Override
    public InternalServerProxy getServerProxy(S request) {

        // if we don't have a last active server set, or the one set is no
        // longer active
        // search for a new free server starting from the beginning
        if (lastActive == null || !lastActive.activate()) {

            for (InternalServerProxy serverProxy : getServerProxies()) {

                if (serverProxy.activate()) {

                    if (serverProxy.getReactivated().get()) {
                        LOGGER.info("Client will connect using the server at [{}:{}]",serverProxy.getHost(), serverProxy.getPort());
                        serverProxy.getReactivated().set(false);
                        notifyListeners(serverProxy);
                    }
                    lastActive = serverProxy;
                    return serverProxy;
                }

            }
        } else { // last active is set and appears active, return it directly.
            return lastActive;
        }

        return null;

    }

    /**
     * notify listeners only when fail over really occurred. not on component
     * startup.
     *
     * @param serverProxy
     */
    private void notifyListeners(InternalServerProxy serverProxy) {

        if (firstTime.get()) {
            firstTime.set(false);
        } else {
            for (FailOverListener listener : failOverListeners) {
                listener.failOverOccured(serverProxy.getHost(), serverProxy.getPort());
            }
        }
    }

    public static void registerFailOverListener(FailOverListener failOverListener) {
        failOverListeners.add(failOverListener);
    }

    public static void removeFailOverListener(FailOverListener failOverListener) {
        failOverListeners.remove(failOverListener);
    }

}
