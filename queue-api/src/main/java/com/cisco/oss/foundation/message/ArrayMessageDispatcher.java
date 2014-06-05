package com.cisco.oss.foundation.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ArrayMessageDispatcher extends AbstractMessageDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArrayMessageDispatcher.class);

	ArrayMessageDispatcher(MessageProcessor messageProcessor){
		super(messageProcessor);
	}

	protected boolean checkAndDispatchEvent(Object message, boolean alreadyOnWaitingList){

		boolean isDispatchable = false;
		// Try to dispatch the event
		synchronized (this){
			isDispatchable = getMessageProcessor().isDispatchable(message);
			if (isDispatchable){
				getMessageProcessor().onDispatchMessage(message);
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
		Object nextEvent = null;
		boolean dispatchedEvent = false;
		for (Object event : getWaitingList()){
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
