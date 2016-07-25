package com.cisco.oss.foundation.core.test;

import com.cisco.oss.foundation.message.RabbitMQAdmin;
import org.junit.Test;

/**
 * Created by Yair Ogen (yaogen) on 09/03/2016.
 */
public class AdminTest {

    @Test
    public void testAdmin(){
        RabbitMQAdmin.INSTANCE.initAdmin();
        RabbitMQAdmin.INSTANCE.getQueueLength("Yair-test-queue");
    }
}
