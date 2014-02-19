package com.cisco.vss.foundation.http.jetty.test

import org.junit.Test
import com.cisco.vss.foundation.http.api.test.{MultithreadTestUtil, BasicHttpTestUtil}
import com.cisco.vss.foundation.http.HttpRequest
import com.cisco.vss.foundation.http.jetty.{JettyHttpClientFactory, JettyHttpResponse}
import org.slf4j.LoggerFactory
import com.cisco.vss.foundation.loadbalancer.{LoadBalancerStrategy, RequestTimeoutException, FailOverStrategy, NoActiveServersException}
import com.cisco.vss.foundation.configuration.ConfigurationFactory

/**
 * Created by Yair Ogen on 1/23/14.
 */
class JettyClientTest {
  val httpTestUtil = new BasicHttpTestUtil[HttpRequest, JettyHttpResponse]
  val httpMultiThreadTestUtil = new MultithreadTestUtil[HttpRequest, JettyHttpResponse]
  val clientTest = JettyHttpClientFactory.createHttpClient("clientTest")
  val LOGGER = LoggerFactory.getLogger(classOf[JettyClientTest])

  @Test
  def basicGoogleFetch() {
    httpTestUtil.basicGoogleFetch(clientTest)
  }

  @Test(expected = classOf[NoActiveServersException])
  def realServerInvokeAndFail() {
    httpTestUtil.realServerInvokeAndFail(clientTest)
  }


  @Test
  def realServerInvokeGet() {
    httpTestUtil.realServerInvokeGet(clientTest)
  }


  @Test
  def realServerInvokePostRoudRobin() {

    val clientRoundRobinTest = JettyHttpClientFactory.createHttpClient("clientRoundRobinSyncTest")
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest, ConfigurationFactory.getConfiguration)

  }

  @Test
  def realServerInvokePostFailOver() {

    val clientFailOverTest = JettyHttpClientFactory.createHttpClient("clientFailOverTest", LoadBalancerStrategy.STRATEGY_TYPE.FAIL_OVER)
    httpTestUtil.realServerInvokePostFailOver(clientFailOverTest)

  }

  @Test(expected = classOf[RequestTimeoutException])
  def timeoutTest() {

    httpTestUtil.timeoutTest(clientTest)


  }


  @Test
  def testUsingActors() {
    val clientRoundRobinTest = JettyHttpClientFactory.createHttpClient("clientRoundRobinTest")
    httpMultiThreadTestUtil.testUsingActors(clientRoundRobinTest)
  }



}
