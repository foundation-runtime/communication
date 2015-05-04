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

import com.cisco.oss.foundation.http._
import org.junit.Assert._
import java.util.Properties
import java.io.{File, FileOutputStream}
import com.cisco.oss.foundation.configuration.ConfigurationFactory
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import org.apache.commons.configuration.Configuration

/**
 * Created by Yair Ogen on 1/23/14.
 */
class BasicHttpTestUtil[S <: HttpRequest, R <: HttpResponse] {

  def basicGoogleFetch(httpClient: HttpClient[S, R]) {
    val request: HttpRequest = HttpRequest.newBuilder().httpMethod(HttpMethod.GET).uri("http://www.google.com").build();
    val response: HttpResponse = httpClient.executeDirect(request.asInstanceOf[S])
    if (response.isSuccess) {
      val result = response.getResponseAsString
      println(s"success:\n$result")
    } else {
      val reason = response.getStatus;
      println(s"failed: $reason")
    }
  }

  def realServerInvokeAndFail(httpClient: HttpClient[S, R]) {
    val request = HttpRequest.newBuilder().httpMethod(HttpMethod.GET).uri("/wsm/session/test").build();
    println(request)
    val response = httpClient.executeWithLoadBalancer(request.asInstanceOf[S])
    if (response.isSuccess) {
      val result = response.getResponseAsString
      println(s"success:\n$result")
    } else {
      val reason = response.getStatus;
      println(s"failed: $reason")
    }
  }

  def realServerInvokeGet(httpClient: HttpClient[S, R]) {
    val server = RunTestServer.runServer(1234)
    Thread.sleep(1000)

    val request = HttpRequest.newBuilder().httpMethod(HttpMethod.GET).uri("/test").build();
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

  def realServerInvokePostRoudRobin(clientRoundRobinTest: HttpClient[S, R], configuration:Configuration) {

    val port1 = 13345
    val port2 = 13346
    val port3 = 13347
    val port4 = 13348
    var server1 = RunTestServer.runServer(port1)
    var server2 = RunTestServer.runServer(port2)
    var server3 = RunTestServer.runServer(port3)
    var server4 = RunTestServer.runServer(port4)
    Thread.sleep(1000)

    val request = HttpRequest.newBuilder()
      .httpMethod(HttpMethod.POST)
      .uri("/test")
      .entity(port1.toString)
      .contentType("text/html")
      .build();

    var groupBy = Map[String, List[String]]()

    var results = List[String]()

    val configURL = getClass.getResource("/config.properties")
    val props = new Properties
    try {

      for (x <- 1 to 9) {
        runRequest(request)
      }
      println("1: ***************")

      assertEquals(9, results.size)

      groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(3, groupBy.size)

      for ((key, value) <- groupBy) {
        assertEquals(3, value.size)
      }

      results = List[String]()

      server1.stop

      for (x <- 1 to 9) {
        runRequest(request)
      }
      println("2: ***************")

      assertEquals(9, results.size)

      groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(2, groupBy.size)

      for ((key, value) <- groupBy) {
        assertTrue(value.size >= 4)
      }

      results = List[String]()

      server1 = RunTestServer.runServer(port1)

      for (x <- 1 to 9) {
        runRequest(request)
      }
      println("3:***************")

      assertEquals(9, results.size)

      groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(2, groupBy.size)

      for ((key, value) <- groupBy) {
        assertTrue(value.size >= 4)
      }

      Thread.sleep(5000)

      results = List[String]()

      for (x <- 1 to 9) {
        runRequest(request)
      }
      println("4:***************")

      assertEquals(9, results.size)

      groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(3, groupBy.size)

      for ((key, value) <- groupBy) {
        assertEquals(3, value.size)
      }

      println("4.1:***************")


      props.load(configURL.openStream())
      props.setProperty("clientRoundRobinSyncTest.4.host", "localhost")
      props.setProperty("clientRoundRobinSyncTest.4.port", "13348")
      props.store(new FileOutputStream(new File(configURL.getFile)), "")

      println("4.2:***************")
      Thread.sleep(3000)
      configuration.getString("clientRoundRobinSyncTest.4.port")

      println("4.3:***************")
      results = List[String]()

      for (x <- 1 to 12) {
        runRequest(request)
      }
      println("5:***************")

      assertEquals(12, results.size)

      groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(4, groupBy.size)

      for ((key, value) <- groupBy) {
        assertEquals(3, value.size)
      }

    }catch {
      case e:Exception  => e.printStackTrace()
    } finally{

      server1.stop
      server2.stop
      server3.stop
      server4.stop

      props.remove("clientRoundRobinSyncTest.4.host")
      props.remove("clientRoundRobinSyncTest.4.port")
      props.store(new FileOutputStream(new File(configURL.getFile)), "")
    }



    def runRequest(request: HttpRequest) {

      FlowContextFactory.createFlowContext()

      val response = clientRoundRobinTest.executeWithLoadBalancer(request.asInstanceOf[S])
      if (response.isSuccess) {
        val result = response.getResponseAsString
        //        println(result)
        results = result :: results
        //      assertEquals("dummy response", result)
      } else {
        val reason = response.getStatus;
        fail(s"failed: $reason")
      }
    }
  }


  def realServerInvokePostFailOver(clientRoundRobinTest: HttpClient[S, R]) {

    val port1 = 23456
    val port2 = 23457
    var server1 = RunTestServer.runServer(port1)
    var server2 = RunTestServer.runServer(port2)

    Thread.sleep(1000)

    val request = HttpRequest.newBuilder()
      .httpMethod(HttpMethod.POST)
      .uri("/test")
      .entity(port1.toString)
      .build();

    var groupBy = Map[String, List[String]]()

    var results = List[String]()

    for (x <- 1 to 9) {
      runRequest(request)
    }
    //println("***************")

    assertEquals(9, results.size)

    groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
    assertEquals(1, groupBy.size)

    for ((key, value) <- groupBy) {
      assertEquals(9, value.size)
      for (s <- value) {
        assertTrue(s contains (port1 + ""))
      }
    }

    server1.stop

    results = List[String]()

    server1.stop

    for (x <- 1 to 9) {
      runRequest(request)
    }
    //println("***************")

    assertEquals(9, results.size)

    groupBy = results.groupBy((s) => s.substring(s.indexOf(":") + 1))
    assertEquals(1, groupBy.size)

    for ((key, value) <- groupBy) {
      assertEquals(9, value.size)
      for (s <- value) {
        assertTrue(s contains (port2 + ""))
      }
    }

    server1.stop
    server2.stop

    def runRequest(request: HttpRequest) {
      FlowContextFactory.createFlowContext()
      val response = clientRoundRobinTest.executeWithLoadBalancer(request.asInstanceOf[S])
      if (response.isSuccess) {
        val result = response.getResponseAsString
        //        println(result)
        results = result :: results
        //      assertEquals("dummy response", result)
      } else {
        val reason = response.getStatus;
        fail(s"failed: $reason")
      }
    }
  }

  def timeoutTest(clientTest: HttpClient[S, R]) {

    val port1 = 1234
    var server1 = RunTestServer.runServer(port1)
    try {

      val request = HttpRequest.newBuilder().httpMethod(HttpMethod.PUT).uri("/test").entity(port1.toString).build();

      clientTest.executeWithLoadBalancer(request.asInstanceOf[S])
    }
    finally {
      server1.stop()

    }


  }

}
