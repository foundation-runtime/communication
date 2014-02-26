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

}
