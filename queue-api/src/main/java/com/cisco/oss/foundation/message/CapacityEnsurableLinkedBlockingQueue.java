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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CapacityEnsurableLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2579901614995900922L;

	private ReentrantLock putLock;
	private AtomicInteger count;
	private int capacity;
	private Condition notFull;
	private ReentrantLock takeLock;
	private Condition notEmpty;


	public CapacityEnsurableLinkedBlockingQueue() throws Exception {
		super();
		initReflectedFields();
	}

	public CapacityEnsurableLinkedBlockingQueue(Collection<? extends E> c) throws Exception {
		super(c);
		initReflectedFields();
	}

	public CapacityEnsurableLinkedBlockingQueue(int capacity) throws Exception {
		super(capacity);
		initReflectedFields();
	}

	private void initReflectedFields() throws Exception {
		putLock = (ReentrantLock)getReflectedObject("putLock");
		count = (AtomicInteger)getReflectedObject("count");
		capacity = (Integer)getReflectedObject("capacity");
		notFull = (Condition)getReflectedObject("notFull");
		takeLock = (ReentrantLock)getReflectedObject("takeLock");
		notEmpty = (Condition)getReflectedObject("notEmpty");
	}

	public void ensureCapacity() throws InterruptedException {
		// Note: convention in all put/take/etc is to preset local var
		// holding count negative to indicate failure unless set.
		int c = -1;	
		putLock.lockInterruptibly();
		try {
			/*
			 * Note that count is used in wait guard even though it is
			 * not protected by lock. This works because count can
			 * only decrease at this point (all other puts are shut
			 * out by lock), and we (or some other waiting put) are
			 * signalled if it ever changes from
			 * capacity. Similarly for all other uses of count in
			 * other wait guards.
			 */
			while (count.get() == capacity) { 
				notFull.await();
			}

			c = count.get();
			if (c + 1 < capacity)
				notFull.signal();
		} finally {
			putLock.unlock();
		}
		if (c == 0)
			signalNotEmpty();
	}

	private Object getReflectedObject(String name) throws Exception {
		try {
			Field configField = LinkedBlockingQueue.class.getDeclaredField(name);
			configField.setAccessible(true);
			return configField.get(this);
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
     * Signals a waiting take. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }
}