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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ArrayMessageDispatcher extends AbstractMessageDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArrayMessageDispatcher.class);

	ArrayMessageDispatcher(ConcurrentMessageHandler concurrentMessageHandler){
		super(concurrentMessageHandler);
	}

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
		getWaitingList().add(message);

		return false;
	}

	protected void pushNextEvent() {

		LOGGER.trace("Trying to push next event");
		// Push next message from waiting list
        Message nextEvent = null;
		boolean dispatchedEvent = false;
		for (Message event : getWaitingList()){
			dispatchedEvent = checkAndDispatchEvent(event, true);
			if (dispatchedEvent){
				nextEvent = event;
				break;
			}
		}	

		if (dispatchedEvent){
			getWaitingList().remove(nextEvent);
		}
		LOGGER.trace("Finish to push next event");
	}
}
