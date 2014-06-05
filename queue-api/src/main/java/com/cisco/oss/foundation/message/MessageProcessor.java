package com.cisco.oss.foundation.message;

/**
 * MessageProcessor process messages; it also defines the lifecycle of a message.
 * The implementation of MessageProcessor must be thread-safe
 * @author yglass
 *
 */
public interface MessageProcessor {

	/**
	 * Check whether the message could be process now or not 
	 * @param message
	 * @return true if the message could be process now, otherwise return false
	 */
	public boolean isDispatchable(Object message);
	
	/**
	 * Message has been received; it could be that the message is not dispatchable yet. 
	 * @param message
	 */
	public void onRecieveMessage(Object message);
	
	/**
	 * The message is dispatchable now and will be added to the worker thread-pool
	 * @param message
	 */
	public void onDispatchMessage(Object message);
	
	/**
	 * Do the business logic
	 * @param message
	 */
	public void process(Object message);
	
	/**
	 * The 'process'  of the method has been completed successfully
	 * @param message
	 */
	public void onCompleteMessage(Object message);
	
	/**
	 * The 'process' method has been completed with exception
	 * @param message
	 * @param throwable
	 */
	public void onException(Object message, Throwable throwable);
}
