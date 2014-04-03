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

import com.cisco.oss.foundation.http._
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import java.util.Properties
import org.junit.Assert._
import java.io.{File, FileOutputStream}
import com.cisco.oss.foundation.configuration.ConfigurationFactory
import java.util.concurrent.{CountDownLatch, ConcurrentHashMap, CopyOnWriteArrayList}
import java.lang.String
import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.configuration.Configuration

/**
 * Created by Yair Ogen on 1/23/14.
 */
class AsyncTestUtil[S <: HttpRequest, R <: HttpResponse] {


  def realServerInvokeAndFail(httpClient: HttpClient[S,R]) {

    class ResponseHandler extends ResponseCallback[R]{
      def completed(response: R): Unit = {

        if (response.isSuccess) {
          RounRobinStats.countdown.countDown
          val result = response.getResponseAsString
          println(s"success:\n$result")
        } else {
          RounRobinStats.countdown.countDown
          val reason = response.getStatus;
          println(s"failed: $reason")
        }

      }

      def failed(e: Throwable): Unit = {

        RounRobinStats.throwable = e
        RounRobinStats.countdown.countDown

      }

      def cancelled(): Unit = ???
    }

    FlowContextFactory.createFlowContext();

    val request = HttpRequest.newBuilder().httpMethod(HttpMethod.GET).uri("/wsm/session/test").build();

    RounRobinStats.countdown = new CountDownLatch(1)
    val response = httpClient.executeWithLoadBalancer(request.asInstanceOf[S], new ResponseHandler)
    RounRobinStats.countdown.await

    throw RounRobinStats.throwable


  }

  object RounRobinStats {

    var groupBy = Map[String, mutable.Buffer[String]]()

    var results = new CopyOnWriteArrayList[String]()

    var countdown:CountDownLatch = null

    var throwable:Throwable = null

  }

  def realServerInvokePostRoudRobin(clientRoundRobinTest: HttpClient[S, R]) {
    realServerInvokePostRoudRobin(clientRoundRobinTest, ConfigurationFactory.getConfiguration)
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



    val configURL = getClass.getResource("/config.properties")
    val props = new Properties
    try {

      RounRobinStats.countdown = new CountDownLatch(9)
      for (x <- 1 to 9) {
        runRequest(request)
      }
      RounRobinStats.countdown.await
      //println("***************")

      assertEquals(9, RounRobinStats.results.size)

      RounRobinStats.groupBy = RounRobinStats.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(3, RounRobinStats.groupBy.size)

      for ((key, value) <- RounRobinStats.groupBy) {
        assertEquals(3, value.size)
      }

      RounRobinStats.results = new CopyOnWriteArrayList[String]()

      server1.stop


      RounRobinStats.countdown = new CountDownLatch(9)
      for (x <- 1 to 9) {
        runRequest(request)
      }
      RounRobinStats.countdown.await
      //println("***************")

      assertEquals(9, RounRobinStats.results.size)

      RounRobinStats.groupBy = RounRobinStats.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(2, RounRobinStats.groupBy.size)

      for ((key, value) <- RounRobinStats.groupBy) {
        assertTrue(value.size >= 4)
      }

      RounRobinStats.results = new CopyOnWriteArrayList[String]()

      server1 = RunTestServer.runServer(port1)

      RounRobinStats.countdown = new CountDownLatch(9)
      for (x <- 1 to 9) {
        runRequest(request)
      }
      RounRobinStats.countdown.await
      //println("***************")

      assertEquals(9, RounRobinStats.results.size)

      RounRobinStats.groupBy = RounRobinStats.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(2, RounRobinStats.groupBy.size)

      for ((key, value) <- RounRobinStats.groupBy) {
        assertTrue(value.size >= 4)
      }

      Thread.sleep(5000)

      RounRobinStats.results = new CopyOnWriteArrayList[String]()

      RounRobinStats.countdown = new CountDownLatch(9)
      for (x <- 1 to 9) {
        runRequest(request)
      }
      RounRobinStats.countdown.await
      //println("***************")

      assertEquals(9, RounRobinStats.results.size)

      RounRobinStats.groupBy = RounRobinStats.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(3, RounRobinStats.groupBy.size)

      for ((key, value) <- RounRobinStats.groupBy) {
        assertEquals(3, value.size)
      }

      println("#################################")

      props.load(getClass.getResourceAsStream("/config.properties"))
      props.setProperty("clientRoundRobinSyncTest.4.host", "localhost")
      props.setProperty("clientRoundRobinSyncTest.4.port", "13348")
      props.store(new FileOutputStream(new File(configURL.getFile)), "")

      Thread.sleep(4000)
      configuration.getBoolean("configuration.dynamicConfigReload.enabled")


      RounRobinStats.results = new CopyOnWriteArrayList[String]()

      RounRobinStats.countdown = new CountDownLatch(12)
      for (x <- 1 to 12) {
        runRequest(request)
      }
      RounRobinStats.countdown.await
      //println("***************")

      assertEquals(12, RounRobinStats.results.size)

      RounRobinStats.groupBy = RounRobinStats.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
      assertEquals(4, RounRobinStats.groupBy.size)

      for ((key, value) <- RounRobinStats.groupBy) {
        assertEquals(3, value.size)
      }

    } finally {
      props.remove("clientRoundRobinSyncTest.4.host")
      props.remove("clientRoundRobinSyncTest.4.port")
      props.store(new FileOutputStream(new File(configURL.getFile)), "")

      server1.stop
      server2.stop
      server3.stop
      server4.stop
    }



    def runRequest(request: HttpRequest) {

      FlowContextFactory.createFlowContext()

      val response = clientRoundRobinTest.executeWithLoadBalancer(request.asInstanceOf[S], new ResponseCallback[R] {
        def cancelled(): Unit = ???

        def completed(response: R): Unit = {
          if (response.isSuccess) {
            val result = response.getResponseAsString
                    println(result)
            RounRobinStats.results.add(result)
            RounRobinStats.countdown.countDown()
            //      assertEquals("dummy response", result)
          } else {
            val reason = response.getStatus;
            fail(s"failed: $reason")
          }
        }

        def failed(e: Throwable): Unit = sys.error(e.toString)
      })


    }
  }

}
