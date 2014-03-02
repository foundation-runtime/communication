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
