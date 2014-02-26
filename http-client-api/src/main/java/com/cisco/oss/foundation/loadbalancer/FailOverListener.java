/**
 *
 */
package com.cisco.oss.foundation.loadbalancer;

/**
 * @author Yair Ogen
 */
public interface FailOverListener {

    void failOverOccured(String host, int port);

}
