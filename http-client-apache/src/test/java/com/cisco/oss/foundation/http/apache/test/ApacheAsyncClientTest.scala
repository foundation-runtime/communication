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

import com.cisco.oss.foundation.http.api.test.AsyncTestUtil
import com.cisco.oss.foundation.http.HttpRequest
import org.junit.Test
import com.cisco.oss.foundation.loadbalancer.NoActiveServersException
import com.cisco.oss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpResponse}
import org.apache.commons.configuration.{PropertiesConfiguration, Configuration}
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import com.cisco.oss.foundation.configuration.FoundationConfigurationListenerRegistry

/**
 * Created by Yair Ogen on 1/23/14.
 */
class ApacheAsyncClientTest {

  val httpTestUtil = new AsyncTestUtil[HttpRequest, ApacheHttpResponse]
  val propsConfiguration: PropertiesConfiguration = new PropertiesConfiguration(classOf[TestApacheClient].getResource("/config.properties"))

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
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest,propsConfiguration)

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
