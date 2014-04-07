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

package com.cisco.oss.foundation.http.api.test

import org.junit.Assert._
import org.junit.{Assert, Test}
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import org.slf4j.LoggerFactory
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.concurrent.CountDownLatch
import scala.collection.JavaConversions._

/**
 * Created by Yair Ogen on 2/9/14.
 */
class ServerTest {

  @Test
  def testSimpleServer(){

    RunTestServer.runServer()
    val numOfIter = 20
    val countDown = new CountDownLatch(numOfIter)

    for (i <- 1 to numOfIter) {

      future{
        FlowContextFactory.createFlowContext()

        LoggerFactory.getLogger(getClass).info("sending request: {}", i)

        val client = ApacheHttpClientFactory.createHttpClient("clientTest")

        val request = HttpRequest.newBuilder().uri("/test").httpMethod(HttpMethod.PUT).entity(i.toString).build()

        val response = client.executeWithLoadBalancer(request)

        Assert.assertNotNull(response.getHeaders.get("Date"))
        println(s"***${(response.getHeaders.get("Date")).mkString(",")}***")


        LoggerFactory.getLogger(getClass).info("got response: {}", i)

        assertEquals(200, response.getStatus)

        assertEquals(i.toString, response.getResponseAsString)

        countDown.countDown()
      }


    }

    countDown.await

  }

}
