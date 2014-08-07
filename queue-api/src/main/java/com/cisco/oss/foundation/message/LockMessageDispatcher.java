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

package com.cisco.oss.foundation.message;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MessageDispatcher contains thread pool to process messages in parallel; 
 * For each received message it check whether it dispatchable, if so it dispatch it to the thread-pool; 
 * otherwise it put it on waiting list and will try to dispatch it when a thread from the thread-pool became free. 
 * MessageDispatcher is thread-safe.
 * @author yglass
 *
 */
public class LockMessageDispatcher extends AbstractMessageDispatcher {

	//private static final Logger LOGGER = LoggerFactory.getLogger(LockMessageDispatcher.class);

	private Lock waitingReadLock;
	private Lock waitingWriteLock;

	public LockMessageDispatcher(ConcurrentMessageHandler concurrentMessageHandler){
		super(concurrentMessageHandler);

		ReadWriteLock waitingReadWriteLock = new ReentrantReadWriteLock();
		waitingReadLock = waitingReadWriteLock.readLock();
		waitingWriteLock = waitingReadWriteLock.writeLock();
	}

	/**
	 * A RejectedExecutionException could be thrown in case there too many requests.
	 */
	protected boolean checkAndDispatchEvent(Message message, boolean alreadyOnWaitingList){

		boolean isDispatchable = false;
		// Try to dispatch the event
		synchronized (this){
			isDispatchable = getConcurrentMessageHandler().isDispatchable(message);
			if (isDispatchable){
				getConcurrentMessageHandler().onDispatchMessage(message);
			}
		}

		if (isDispatchable){
			dispatchMessage(message);
			return true;
		}
		
		if (alreadyOnWaitingList){
			return false;
		}		

		// In case the event can't be dispatch now, add it to the waiting list
		waitingWriteLock.lock();
		try {
			// If the current key is on work OR the working threads are full - put the message on waiting list (FIFO)
			getWaitingList().add(message);
		}
		finally {
			waitingWriteLock.unlock();
		}
		return false;
	}

	protected void pushNextEvent() {

		// Push next message from waiting list
		Message nextEvent = null;
		boolean dispatchedEvent = false;
		waitingReadLock.lock();
		try {
			
			for (Message event : getWaitingList()){
				dispatchedEvent = checkAndDispatchEvent(event, true);
				if (dispatchedEvent){
					nextEvent = event;
					break;
				}
			}
		}
		finally {
			waitingReadLock.unlock();
		}

		if (dispatchedEvent){
			waitingWriteLock.lock();
			try {
				getWaitingList().remove(nextEvent);
			}
			finally {
				waitingWriteLock.unlock();
			}
		}
	}	
}
