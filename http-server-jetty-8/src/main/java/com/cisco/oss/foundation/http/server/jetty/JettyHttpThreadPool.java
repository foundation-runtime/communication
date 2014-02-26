package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.HttpThreadPool;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * a jetty base implementation to the common HttpThreadPool interface
 * Created by Yair Ogen on 2/6/14.
 */
public class JettyHttpThreadPool implements HttpThreadPool{

    private String serviceName;

    public JettyHttpThreadPool(String serviceName){
        this.serviceName = serviceName;
    }

    QueuedThreadPool threadPool = createThreadPool();

    private QueuedThreadPool createThreadPool() {
        Configuration configuration = ConfigurationFactory.getConfiguration();

        int minThreads = configuration.getInt(serviceName + ".http.minThreads", 100);
        int maxThreads = configuration.getInt(serviceName + ".http.maxThreads", 1000);
        if (minThreads <= 0) {
            throw new IllegalArgumentException("http server min number of threads must be greater than 0");
        }

        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMaxThreads(maxThreads);
        queuedThreadPool.setMinThreads(minThreads);
        return queuedThreadPool;
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
}
