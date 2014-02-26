package com.cisco.oss.foundation.http.jetty.test

import com.cisco.oss.foundation.http.api.test.{AsyncTestUtil, BasicHttpTestUtil}
import com.cisco.oss.foundation.http.HttpRequest
import com.cisco.oss.foundation.http.jetty.{JettyHttpClientFactory, JettyHttpResponse}
import org.junit.Test
import com.cisco.oss.foundation.loadbalancer.NoActiveServersException
import com.cisco.oss.foundation.flowcontext.FlowContextFactory

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
