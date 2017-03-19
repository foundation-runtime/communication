/*
 * Copyright 2015 Cisco Systems, Inc.
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

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.flowcontext.FlowContext;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

abstract class AbstractMessageDispatcher implements MessageDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);

	private List<Message> waitingList;
	private ConcurrentMessageHandler concurrentMessageHandler;
	private ExecutorService executorService;
	private CapacityEnsurableLinkedBlockingQueue<Runnable> blockingWaitingQueue;

	public AbstractMessageDispatcher(ConcurrentMessageHandler concurrentMessageHandler){
		this.concurrentMessageHandler = concurrentMessageHandler;

		Configuration configuration = ConfigurationFactory.getConfiguration();
		int maxThreadPoolSize = configuration.getInt(MessageConstants.QUEUE_SIZE_PROPERTY);
		int waitingQueueSize = configuration.getInt(MessageConstants.WAITING_QUEUE_SIZE_PROPERTY);

		waitingList = new CopyOnWriteArrayList<Message>();
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

	public ConcurrentMessageHandler getConcurrentMessageHandler() {
		return concurrentMessageHandler;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public List<Message> getWaitingList() {
		return waitingList;
	}

	protected abstract boolean checkAndDispatchEvent(Message message, boolean alreadyOnWaitingList);
	protected abstract void pushNextEvent();

	public void handleMessage(Message message) {
		// Do eventRecive method on lifecyclePhases
		getConcurrentMessageHandler().onRecieveMessage(message);
		// Try to dispatch the event
		checkAndDispatchEvent(message, false);
	}

	protected void dispatchMessage(Message message){
		MessageProcessorRunnable workerRunnable = new MessageProcessorRunnable(concurrentMessageHandler, message, FlowContextFactory.getFlowContext());

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


	private void completeProcessing(Message message, Throwable throwable){
		try {
			if (null != throwable){
				getConcurrentMessageHandler().onException(message, throwable);
			}
			else {
				getConcurrentMessageHandler().onCompleteMessage(message);
			}
		} finally {
			pushNextEvent();
		}
	}

	class MessageProcessorRunnable implements Runnable {

		private Message message;
		private ConcurrentMessageHandler concurrentMessageHandler;
		private FlowContext flowContext;

		public MessageProcessorRunnable(ConcurrentMessageHandler concurrentMessageHandler, Message message, FlowContext flowContext){
			this.message = message;
			this.concurrentMessageHandler = concurrentMessageHandler;
			this.flowContext = flowContext;
		}

		@Override
		public void run() {
			try {
				FlowContextFactory.addFlowContext(flowContext);
				LOGGER.trace("Start running run method");
                concurrentMessageHandler.process(message);
                concurrentMessageHandler = null;
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
