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


import com.cisco.oss.foundation.http.HttpRequest;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * this is the round robin strategy implementation. this strategy delegates the
 * requests to different servers each time. this way the load is balanced
 * between more than 1 server.
 * 
 * @author Yair Ogen
 */
public class RoundRobinStrategy<S extends HttpRequest> extends AbstractLoadBalancerStrategy<S> {

	private static final long serialVersionUID = 6806126762594591923L;
    private AtomicInteger nextIndex = new AtomicInteger(0);

	protected int nextNode(int serverProxisListSize, S request) {
        int index = nextIndex.incrementAndGet() % serverProxisListSize;
        return index;
	}

	@Override
	public InternalServerProxy getServerProxy(S request) {
		final int serverProxisListSize = getServerProxies().size();
		boolean hasMoreServers = true;

		if (serverProxisListSize > 0) {

			while (hasMoreServers) {

				// get a random number that is between 0 and
				// serverProxisListSize - 1 (indexes of the collection start
				// from 0).
				final int index = nextNode(serverProxisListSize, request);

				final InternalServerProxy serverProxy = getServerProxies().get(index);
				if (serverProxy.activate()) {
					return serverProxy;
				}

				hasMoreServers = hasMoreServers(getServerProxies());

			}

		}
		return null;

	}

	private boolean hasMoreServers(final List<InternalServerProxy> list) {
		boolean hasMoreServers = false;

		final Iterator<InternalServerProxy> internalServerListIter = list.iterator();
		while (internalServerListIter.hasNext() && !hasMoreServers) {

			final InternalServerProxy internalServerProxy = internalServerListIter.next();
			hasMoreServers = internalServerProxy.isActive();
		}

		// if found an active server, stop the loop
		return hasMoreServers;
	}

}
