/*
 * Copyright 2014 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.http.apache.test

import org.slf4j.LoggerFactory
import org.junit.Test
import com.cisco.oss.foundation.http.HttpRequest
import com.cisco.oss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpResponse}
import com.cisco.oss.foundation.loadbalancer.{LoadBalancerStrategy, RequestTimeoutException, NoActiveServersException}
import com.cisco.oss.foundation.http.api.test.{MultithreadTestUtil, BasicHttpTestUtil}
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


