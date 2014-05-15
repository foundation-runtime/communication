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

/**
 * Generic RuntimeException for possible queue related exceptions
 * Created by Yair Ogen
 */
public class QueueException extends RuntimeException {

	private static final long serialVersionUID = 3681660394330306229L;

	public QueueException() {
		super();
	}

	public QueueException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueueException(String message) {
		super(message);
	}

	public QueueException(Throwable cause) {
		super(cause);
	}

}
