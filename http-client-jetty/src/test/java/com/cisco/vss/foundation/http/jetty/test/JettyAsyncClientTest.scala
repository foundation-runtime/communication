package com.cisco.vss.foundation.http.jetty.test

import com.cisco.vss.foundation.http.api.test.{AsyncTestUtil, BasicHttpTestUtil}
import com.cisco.vss.foundation.http.HttpRequest
import com.cisco.vss.foundation.http.jetty.{JettyHttpClientFactory, JettyHttpResponse}
import org.junit.Test
import com.cisco.vss.foundation.loadbalancer.NoActiveServersException
import com.cisco.vss.foundation.flowcontext.FlowContextFactory

/**
 * Created by Yair Ogen on 1/23/14.
 */
class JettyAsyncClientTest {

  val httpTestUtil = new AsyncTestUtil[HttpRequest, JettyHttpResponse]

  @Test(expected = classOf[NoActiveServersException])
  def realServerInvokeAndFail() {
//    FlowContextFactory.createFlowContext();
    val clientTest = JettyHttpClientFactory.createHttpClient("clientTest")
    httpTestUtil.realServerInvokeAndFail(clientTest)
    //fix test
  }

  @Test
  def realServerInvokePostRoudRobin() {

    val clientRoundRobinTest = JettyHttpClientFactory.createHttpClient("clientRoundRobinSyncTest")
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest)

  }


}
