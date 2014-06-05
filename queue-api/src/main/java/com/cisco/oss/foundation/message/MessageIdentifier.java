package com.cisco.oss.foundation.message;

/**
 * Identify message
 * The implementation of MessageIdentifier must be thread-safe
 * @author yglass
 *
 */
public interface MessageIdentifier {

	/**
	 * 
	 * @param message
	 * @return the message identifier
	 */
	public String getIdentifier(Object message);
}
