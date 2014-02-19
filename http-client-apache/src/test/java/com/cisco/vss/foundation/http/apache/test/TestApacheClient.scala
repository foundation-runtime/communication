package com.cisco.vss.foundation.http.apache.test

import org.slf4j.LoggerFactory
import org.junit.Test
import com.cisco.vss.foundation.http.HttpRequest
import com.cisco.vss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpResponse}
import com.cisco.vss.foundation.loadbalancer.{LoadBalancerStrategy, RequestTimeoutException, NoActiveServersException}
import com.cisco.vss.foundation.http.api.test.{MultithreadTestUtil, BasicHttpTestUtil}
import org.apache.commons.configuration.{Configuration, PropertiesConfiguration}

/**
 * Created by Yair Ogen on 1/19/14.
 */
class TestApacheClient {
  val httpTestUtil = new BasicHttpTestUtil[HttpRequest,ApacheHttpResponse]
  val httpMultiThreadTestUtil = new MultithreadTestUtil[HttpRequest,ApacheHttpResponse]
  val propsConfiguration: Configuration = new PropertiesConfiguration(classOf[TestApacheClient].getResource("/config.properties"))
  val clientTest = {
    ApacheHttpClientFactory.createHttpClient("clientTest", propsConfiguration)
  }
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
    val strategy: CABFileChangedReloadingStrategy = new CABFileChangedReloadingStrategy
    strategy.setRefreshDelay(3000)
    propsConfiguration.asInstanceOf[PropertiesConfiguration].setReloadingStrategy(strategy)

    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinSyncTest", propsConfiguration)
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest, propsConfiguration)

  }

  @Test
  def realServerInvokePostFailOver() {

    val clientFailOverTest = ApacheHttpClientFactory.createHttpClient("clientFailOverTest", LoadBalancerStrategy.STRATEGY_TYPE.FAIL_OVER, propsConfiguration)
    httpTestUtil.realServerInvokePostFailOver(clientFailOverTest)

  }

  @Test (expected = classOf[RequestTimeoutException])
  def timeoutTest(){

    httpTestUtil.timeoutTest(clientTest)


  }


  @Test
  def testUsingActors() {
    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinTest", propsConfiguration)
    httpMultiThreadTestUtil.testUsingActors(clientRoundRobinTest)
  }

}


