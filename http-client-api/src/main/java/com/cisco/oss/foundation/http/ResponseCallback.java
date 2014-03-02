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

package com.cisco.oss.foundation.http;

/**
 * Interface to be used by users of the async API. It defines the callback methods that are invoked when an asynchronous request is completed.
 * Created by Yair Ogen on 1/6/14.
 */
public interface ResponseCallback<R extends HttpResponse> {

    /**
     * Invoked when all communications are successful and content is consumed.
     */
    public void completed(R response);

    /**
     * Invoked when any error happened in the communication or content consumption.
     */
    public void failed(Throwable e);

    /**
     * Invoked if the I/O operation is cancelled after it is started.
     */
    public void cancelled();
}
