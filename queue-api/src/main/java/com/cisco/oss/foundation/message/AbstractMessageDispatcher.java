package com.cisco.oss.foundation.message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nds.cab.infra.configuration.ConfigurationFactory;

abstract class AbstractMessageDispatcher implements MessageDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);

	private List<Object> waitingList;
	private MessageProcessor messageProcessor;
	private ExecutorService executorService;
	private CapacityEnsurableLinkedBlockingQueue<Runnable> blockingWaitingQueue;

	public AbstractMessageDispatcher(MessageProcessor messageProcessor){
		this.messageProcessor = messageProcessor;

		Configuration configuration = ConfigurationFactory.getConfiguration();
		int maxThreadPoolSize = configuration.getInt(MessageConstants.QUEUE_SIZE_PROPERTY);
		int waitingQueueSize = configuration.getInt(MessageConstants.WAITING_QUEUE_SIZE_PROPERTY);

		waitingList = new CopyOnWriteArrayList<Object>();
		try {
			if (waitingQueueSize > 0){
				blockingWaitingQueue = new CapacityEnsurableLinkedBlockingQueue<Runnable>(waitingQueueSize);
			}
			else {
				blockingWaitingQueue = new CapacityEnsurableLinkedBlockingQueue<Runnable>();
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to create message dispatcher", ex);
		}
		executorService = new ThreadPoolExecutor(maxThreadPoolSize, maxThreadPoolSize,
				0L, TimeUnit.MILLISECONDS,blockingWaitingQueue); //Executors.newFixedThreadPool(maxThreadPoolSize);
	}

	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public List<Object> getWaitingList() {
		return waitingList;
	}

	protected abstract boolean checkAndDispatchEvent(Object message, boolean alreadyOnWaitingList);
	protected abstract void pushNextEvent();

	public void handleMessage(Object message) {
		// Do eventRecive method on lifecyclePhases
		getMessageProcessor().onRecieveMessage(message);
		// Try to dispatch the event
		checkAndDispatchEvent(message, false);
	}

	protected void dispatchMessage(Object message){
		MessageProcessorRunnable workerRunnable = new MessageProcessorRunnable(messageProcessor, message);

		try {
			LOGGER.trace("On ensure capacity");
			blockingWaitingQueue.ensureCapacity();
			LOGGER.trace("Finish ensure capacity, going to execute");
			executorService.execute(workerRunnable);
			return;
		} catch (Exception ex){
			LOGGER.error("Failed to dispatch message", ex);
		}
	}


	private void completeProcessing(Object message, Throwable throwable){
		try {
			if (null != throwable){
				getMessageProcessor().onException(message, throwable);
			}
			else {
				getMessageProcessor().onCompleteMessage(message);
			}
		} finally {
			pushNextEvent();
		}
	}

	class MessageProcessorRunnable implements Runnable {

		private Object message;
		private MessageProcessor messageProcessor;

		public MessageProcessorRunnable(MessageProcessor messageProcessor, Object message){
			this.message = message;
			this.messageProcessor = messageProcessor;
		}

		@Override
		public void run() {
			try {
				LOGGER.trace("Start running run method");
				messageProcessor.process(message);
				messageProcessor = null;
				completeProcessing(message, null);
				LOGGER.trace("Finish running run method with success");
			} catch (Throwable throwable) {
				LOGGER.error("Problem occurred when processing message '{}'. Error is: {}", new Object[]{message,throwable,throwable});
				completeProcessing(message, throwable);
				LOGGER.trace("Finish running run method with exception");
			}
		}
	}
}
