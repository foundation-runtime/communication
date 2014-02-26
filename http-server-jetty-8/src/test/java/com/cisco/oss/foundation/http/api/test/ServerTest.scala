package com.cisco.oss.foundation.http.api.test

import org.junit.Assert._
import org.junit.Test
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import org.slf4j.LoggerFactory
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.concurrent.CountDownLatch

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

        LoggerFactory.getLogger(getClass).info("got response: {}", i)

        assertEquals(200, response.getStatus)

        assertEquals(i.toString, response.getResponseAsString)

        countDown.countDown()
      }


    }

    countDown.await

  }

}
