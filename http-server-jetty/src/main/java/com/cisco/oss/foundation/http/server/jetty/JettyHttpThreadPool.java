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

package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.HttpThreadPool;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * a jetty base implementation to the common HttpThreadPool interface
 * Created by Yair Ogen on 2/6/14.
 */
public class JettyHttpThreadPool implements HttpThreadPool{

    private String serviceName;


    private QueuedThreadPool threadPool;

    public JettyHttpThreadPool(String serviceName){
        this.serviceName = serviceName;
    }

    public JettyHttpThreadPool init() {
        threadPool = createThreadPool();
        return this;
    }

    private QueuedThreadPool createThreadPool() {
        Configuration configuration = ConfigurationFactory.getConfiguration();

        int minThreads = configuration.getInt(serviceName + ".http.minThreads", 100);
        int maxThreads = configuration.getInt(serviceName + ".http.maxThreads", 1000);
        if (minThreads <= 0) {
            throw new IllegalArgumentException("http server min number of threads must be greater than 0");
        }


        return new QueuedThreadPool(maxThreads, minThreads);
    }
    public QueuedThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * set the max number of threads on the pool
     * @param maxThreads
     */
    @Override
    public void setMaxThreads(int maxThreads) {
        threadPool.setMaxThreads(maxThreads);
    }

    /**
     * get the max number of threads on the pool
     * @return
     */
    @Override
    public int getMaxThreads() {
        return threadPool.getMaxThreads();
    }

    /**
     * set the min number of threads on the pool
     * @param minThreads
     */
    @Override
    public void setMinThreads(int minThreads) {
        threadPool.setMinThreads(minThreads);
    }

    /**
     * get the min number of threads on the pool
     * @return
     */
    @Override
    public int getMinThreads() {
        return threadPool.getMinThreads();
    }

    /**
     * return true if the server doesn't have enough threads to process a request
     * @return
     */
    @Override
    public boolean isLowOnThreads() {
        return threadPool.isLowOnThreads();
    }

    @Override
    public int getThreads() {
        return threadPool.getThreads();
    }
}
