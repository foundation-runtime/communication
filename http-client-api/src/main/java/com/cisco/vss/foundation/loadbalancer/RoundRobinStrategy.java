package com.cisco.vss.foundation.loadbalancer;


import com.cisco.vss.foundation.http.HttpRequest;

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
