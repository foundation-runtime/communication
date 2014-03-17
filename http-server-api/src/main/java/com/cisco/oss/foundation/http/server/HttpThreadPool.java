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

package com.cisco.oss.foundation.http.server;

/**
 * Created by Yair Ogen on 2/6/14.
 */
public interface HttpThreadPool {

    /**
     * set the max number of threads on the pool
     * @param maxThreads
     */
    void setMaxThreads(int maxThreads);

    /**
     * get the max number of threads on the pool
     * @return
     */
    int getMaxThreads();

    /**
     * set the min number of threads on the pool
     * @param minThreads
     */
   void setMinThreads(int minThreads);

    /**
     * get the min number of threads on the pool
     * @return
     */
    int getMinThreads();

    /**
     * return true if the server doesn't have enough threads to process a request
     * @return
     */
    boolean isLowOnThreads();

    /**
     * Get the current number of threads used
     */
    int getThreads();

}
