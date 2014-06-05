package com.cisco.oss.foundation.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractMessageProcessor process messages with or without identifier
 * In case there is no implementation for MessageIdentifier it lets all messages to be process in parallel
 * If there is a MessageIdentifier implementation, each message must have identifier and no more than one message with the same identifier will be process in parallel. 
 * If a message has 'null' identifier a NullPointerException will be thrown.
 * @author yglass
 *
 */
abstract public class AbstractMessageProcessor implements MessageProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageProcessor.class);
	
	private MessageIdentifier messageIdentifier;
	private Map<String, Object> onWorkIdentifierMap;
	
	public AbstractMessageProcessor(){
		this(null);
	}
	
	public AbstractMessageProcessor(MessageIdentifier messageIdentifier){
		this.messageIdentifier = messageIdentifier;
		onWorkIdentifierMap = new ConcurrentHashMap<String, Object>();
	}

	/**
	 * A message is dispatchable in case there is no other message with the same identifier that is in 'process' now.
	 * If a message has 'null' identifier a true value will be return.
	 */
	@Override
	public boolean isDispatchable(Object message) {
		if(messageIdentifier != null) {
			String identifier = messageIdentifier.getIdentifier(message);
			LOGGER.trace("Message identifier is: {}", identifier);
			if (StringUtils.isNotEmpty(identifier)){
				return !onWorkIdentifierMap.containsKey(identifier);
			}
		}
		return true;
	}

	@Override
	public void onRecieveMessage(Object message) {
		LOGGER.trace("Recived message '{}'", message);
	}

	@Override
	public void onDispatchMessage(Object message) {
		LOGGER.trace("Dispatch message '{}'", message);
		if(messageIdentifier != null){
			String identifier = messageIdentifier.getIdentifier(message);
			if (StringUtils.isNotEmpty(identifier)){
				onWorkIdentifierMap.put(identifier, 0);
			}
		}
	}

	@Override
	public void onCompleteMessage(Object message) {
		LOGGER.trace("Complete message '{}'", message);
		if(messageIdentifier != null){
			String identifier = messageIdentifier.getIdentifier(message);
			if (StringUtils.isNotEmpty(identifier)){
				onWorkIdentifierMap.remove(identifier);
			}
		}
	}

	@Override
	public void onException(Object message, Throwable throwable) {
		if(messageIdentifier != null){
			String identifier = messageIdentifier.getIdentifier(message);
			if (StringUtils.isNotEmpty(identifier)){
				onWorkIdentifierMap.remove(identifier);
			}
		}
	}	
}