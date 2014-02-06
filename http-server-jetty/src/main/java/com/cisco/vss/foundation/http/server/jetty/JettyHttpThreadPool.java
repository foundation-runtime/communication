package com.cisco.vss.foundation.http.server.jetty;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.server.HttpThreadPool;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
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

        int minThreads = configuration.getInt("service." + serviceName + ".http.minThreads", 100);
        int maxThreads = configuration.getInt("service." + serviceName + ".http.maxThreads", 1000);
        if (minThreads <= 0) {
            throw new IllegalArgumentException("http server min number of threads must be greater than 0");
        }


        return new QueuedThreadPool(maxThreads, minThreads);
    }

    @Override
    public void setMaxThreads(int maxThreads) {
        threadPool.setMaxThreads(maxThreads);
    }

    @Override
    public int getMaxThreads() {
        return threadPool.getMaxThreads();
    }

    @Override
    public void setMinThreads(int minThreads) {
        threadPool.setMinThreads(minThreads);
    }

    @Override
    public int getMinThreads() {
        return threadPool.getMinThreads();
    }

    @Override
    public boolean isLowOnThreads() {
        return threadPool.isLowOnThreads();
    }
}
