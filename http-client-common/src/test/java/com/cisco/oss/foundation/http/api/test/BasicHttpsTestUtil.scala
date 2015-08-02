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

package com.cisco.oss.foundation.http.api.test

import java.io.{File, FileOutputStream}
import java.util.Properties

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http._
import org.apache.commons.configuration.Configuration
import org.junit.Assert._

/**
 * Created by Yair Ogen on 1/23/14.
 */
class BasicHttpsTestUtil[S <: HttpRequest, R <: HttpResponse] {



  def realHttpsServerInvokeGet(httpClient: HttpClient[S, R]) {
    val server = RunTestServer.runHTTPSServer(1579)
    Thread.sleep(1500)

    val request = HttpRequest.newBuilder().httpMethod(HttpMethod.GET).https().uri("/test").build();
    val response = httpClient.executeWithLoadBalancer(request.asInstanceOf[S])
    if (response.isSuccess) {
      val result = response.getResponseAsString
      assertEquals("dummy response", result)
    } else {
      val reason = response.getStatus;
      fail(s"failed: $reason")
    }

    server.stop
  }


}
