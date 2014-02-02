/**
 *
 */
package com.cisco.vss.foundation.loadbalancer;

/**
 * @author Yair Ogen
 */
public interface FailOverListener {

    void failOverOccured(String host, int port);

}
