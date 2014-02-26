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
