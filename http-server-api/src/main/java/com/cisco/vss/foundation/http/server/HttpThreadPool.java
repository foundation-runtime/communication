package com.cisco.vss.foundation.http.server;

/**
 * Created by Yair Ogen on 2/6/14.
 */
public interface HttpThreadPool {

    void setMaxThreads(int maxThreads);
    int getMaxThreads();
    void setMinThreads(int minThreads);
    int getMinThreads();
    boolean isLowOnThreads();

}
