package com.cisco.vss.foundation.http.apache.test

import org.slf4j.LoggerFactory
import org.junit.Test
import com.cisco.vss.foundation.http.HttpRequest
import com.cisco.vss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpResponse}
import com.cisco.vss.foundation.loadbalancer.{HighAvailabilityStrategy, RequestTimeoutException, NoActiveServersException}
import com.cisco.vss.foundation.http.api.test.{MultithreadTestUtil, BasicHttpTestUtil}

/**
 * Created by Yair Ogen on 1/19/14.
 */
class TestApacheClient {
  val httpTestUtil = new BasicHttpTestUtil[HttpRequest,ApacheHttpResponse]
  val httpMultiThreadTestUtil = new MultithreadTestUtil[HttpRequest,ApacheHttpResponse]
  val clientTest = ApacheHttpClientFactory.createHttpClient("clientTest")
  val LOGGER = LoggerFactory.getLogger(classOf[TestApacheClient])

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

    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinSyncTest")
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest)

  }

  @Test
  def realServerInvokePostFailOver() {

    val clientFailOverTest = ApacheHttpClientFactory.createHttpClient("clientFailOverTest", HighAvailabilityStrategy.STRATEGY_TYPE.FAIL_OVER)
    httpTestUtil.realServerInvokePostFailOver(clientFailOverTest)

  }

  @Test (expected = classOf[RequestTimeoutException])
  def timeoutTest(){

    httpTestUtil.timeoutTest(clientTest)


  }


  @Test
  def testUsingActors() {
    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinTest")
    httpMultiThreadTestUtil.testUsingActors(clientRoundRobinTest)
  }

}


