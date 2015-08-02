/*
 * Copyright 2015 Cisco Systems, Inc.
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

package com.cisco.oss.foundation.http.jetty.test

import org.junit.Test
import com.cisco.oss.foundation.http.api.test.{MultithreadTestUtil, BasicHttpTestUtil}
import com.cisco.oss.foundation.http.HttpRequest
import com.cisco.oss.foundation.http.jetty.{JettyHttpClientFactory, JettyHttpResponse}
import org.slf4j.LoggerFactory
import com.cisco.oss.foundation.loadbalancer.{LoadBalancerStrategy, RequestTimeoutException, FailOverStrategy, NoActiveServersException}
import com.cisco.oss.foundation.configuration.ConfigurationFactory

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
