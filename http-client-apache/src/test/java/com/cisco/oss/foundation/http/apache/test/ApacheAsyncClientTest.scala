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

package com.cisco.oss.foundation.http.apache.test

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.cisco.oss.foundation.http.api.test.AsyncTestUtil
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest, ResponseCallback}
import org.junit.{Assert, Test}
import com.cisco.oss.foundation.loadbalancer.NoActiveServersException
import com.cisco.oss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpResponse}
import org.apache.commons.configuration.{Configuration, PropertiesConfiguration}
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import com.cisco.oss.foundation.configuration.FoundationConfigurationListenerRegistry
import org.slf4j.LoggerFactory

/**
  * Created by Yair Ogen on 1/23/14.
  */
class ApacheAsyncClientTest {

  val LOGGER = LoggerFactory.getLogger(classOf[ApacheAsyncClientTest])
  val httpTestUtil = new AsyncTestUtil[HttpRequest, ApacheHttpResponse]
  val propsConfiguration: PropertiesConfiguration = new PropertiesConfiguration(classOf[TestApacheClient].getResource("/config.properties"))

  @Test
  def testDirectAsync(): Unit = {

    val coundtdown = new CountDownLatch(1)

    val clientTest = ApacheHttpClientFactory.createHttpClient("directAsyncClient")
    val request = HttpRequest.newBuilder()
      .uri("http://google.com:80")
      .httpMethod(HttpMethod.GET)
      .build()

    clientTest.executeDirect(request, new ResponseCallback[ApacheHttpResponse] {

      override def cancelled(): Unit = ???

      override def completed(response: ApacheHttpResponse): Unit = {
        LOGGER.info(s"got response. code: {}. body: {}", response.getStatus, response.getResponseAsString)
        coundtdown.countDown()
      }

      override def failed(e: Throwable): Unit = {
        LOGGER.error(e.toString)
      }
    })

    try {
      val success = coundtdown.await(2, TimeUnit.SECONDS)
      Assert.assertTrue("Count down has timed-out",success)
    } catch {
      case e:Exception => Assert.fail(e.toString)
    }
  }

  @Test(expected = classOf[NoActiveServersException])
  def realServerInvokeAndFail() {
    //    FlowContextFactory.createFlowContext();
    val clientTest = ApacheHttpClientFactory.createHttpClient("clientTest", propsConfiguration)
    httpTestUtil.realServerInvokeAndFail(clientTest)
  }

  @Test
  def realServerInvokePostRoudRobin() {
    val strategy: FoundationFileChangedReloadingStrategy = new FoundationFileChangedReloadingStrategy
    strategy.setRefreshDelay(propsConfiguration.getInt("configuration.dynamicConfigReload.refreshDelay"))
    propsConfiguration.setReloadingStrategy(strategy)

    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinSyncTest", propsConfiguration)
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest, propsConfiguration)

  }

  /**
    * @author Yair Ogen
    *
    */
  class FoundationFileChangedReloadingStrategy extends FileChangedReloadingStrategy {
    /**
      * @see org.apache.commons.configuration.reloading.FileChangedReloadingStrategy#reloadingPerformed()
      */
    override def reloadingPerformed {
      super.reloadingPerformed
      FoundationConfigurationListenerRegistry.fireConfigurationChangedEvent
    }
  }

}
