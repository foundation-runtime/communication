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
import org.junit.Test
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import org.slf4j.LoggerFactory
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.concurrent.CountDownLatch
import com.cisco.oss.foundation.http.server.HttpServerFactory

/**
 * Created by Yair Ogen on 2/9/14.
 */
class ServerTest {

  @Test
  def testSimpleServer() {

    RunTestServer.runServer()
    val numOfIter = 20
    val countDown = new CountDownLatch(numOfIter)

    for (i <- 1 to numOfIter) {

      future {
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

    RunTestServer.stopServer()

  }

  @Test
  def testTrace() {

    RunTestServer.runServer()
    val numOfIter = 5
    val countDown = new CountDownLatch(numOfIter)

    val client = ApacheHttpClientFactory.createHttpClient("clientTest")
    for (i <- 1 to numOfIter) {
      val index = i
      future {

        val JSON = "{\n  \"blueprint\": {\n    \"tiers\": [\n      {\n        \"generic\": {\n          \"name\": \"upm\",\n          \"order\": 1,\n          \"nodeCount\": 4,\n          \"reverseProxy\": false,\n          \"dns\": false,\n          \"node\": {\n            \"region\": \"us-es\",\n            \"swapFile\": false,\n            \"minDisk\": 10,\n            \"minCores\": 4,\n            \"minRam\": 1024,\n            \"osType\": \"Red Hat\",\n            \"osVersion\": 6.5,\n            \"networks\": [\n              {\n                \"name\": \"internal\",\n                \"ports\": [\n                  6060,\n                  6061\n                ]\n              }\n            ]\n          },\n          \"modules\": [\n            {\n              \"name\": \"cisco_upm\",\n              \"version\": \"5.0.5\",\n              \"configuration\": {\n                \"hiera\": {\n                  \"values\": [\n                    {\n                      \"name\": \"foo1\",\n                      \"value\": \"bar1\"\n                    },\n                    {\n                      \"name\": \"foo2\",\n                      \"value\": \"bar2\"\n                    }\n                  ]\n                },\n                \"ccp\": {\n                  \"baseConfig\": \"/docs/samples/config.properties\",\n                  \"sumpplement\": [\n                    {\n                      \"name\": \"name1\",\n                      \"value\": \"value1\"\n                    },\n                    {\n                      \"name\": \"name2\",\n                      \"value\": \"value2\"\n                    }\n                  ]\n                }\n              }\n            }\n          ]\n        }\n      },\n      {\n        \"generic\": {\n          \"name\": \"pps\",\n          \"order\": 1,\n          \"nodeCount\": 3,\n          \"reverseProxy\": false,\n          \"dns\": false,\n          \"node\": {\n            \"region\": \"us-es\",\n            \"swapFile\": false,\n            \"minDisk\": 10,\n            \"minCores\": 4,\n            \"minRam\": 1024,\n            \"osType\": \"Red Hat\",\n            \"osVersion\": 6.5,\n            \"networks\": [\n              {\n                \"name\": \"internal\",\n                \"ports\": [\n                  6060,\n                  6061\n                ]\n              }\n            ]\n          },\n          \"modules\": [\n            {\n              \"name\": \"cisco_pps\",\n              \"version\": \"5.0.5\",\n              \"configuration\": {\n                \"hiera\": {\n                  \"values\": [\n                    {\n                      \"name\": \"foo1\",\n                      \"value\": \"bar1\"\n                    },\n                    {\n                      \"name\": \"foo2\",\n                      \"value\": \"bar2\"\n                    }\n                  ]\n                },\n                \"ccp\": {\n                  \"baseConfig\": \"/docs/samples/config.properties\",\n                  \"sumpplement\": [\n                    {\n                      \"name\": \"name1\",\n                      \"value\": \"value1\"\n                    },\n                    {\n                      \"name\": \"name2\",\n                      \"value\": \"value2\"\n                    }\n                  ]\n                }\n              }\n            }\n          ]\n        }\n      },\n      {\n        \"mongodb\": {\n          \"name\": \"mongo-cluster\",\n          \"order\": 2,\n          \"shardCount\": 3,\n          \"nodesInReplica\": 3,\n          \"mongod-node\": {\n            \"region\": \"us-es\",\n            \"swapFile\": false,\n            \"minDisk\": 10,\n            \"minCores\": 4,\n            \"minRam\": 1024,\n            \"osType\": \"Red Hat\",\n            \"osVersion\": 6.5,\n            \"networks\": [\n              {\n                \"name\": \"internal\",\n                \"ports\": [\n                  6060,\n                  6061\n                ]\n              }\n            ]\n          },\n          \"mongoc-node\": {\n            \"region\": \"us-es\",\n            \"swapFile\": false,\n            \"minDisk\": 10,\n            \"minCores\": 4,\n            \"minRam\": 1024,\n            \"osType\": \"Red Hat\",\n            \"osVersion\": 6.5,\n            \"networks\": [\n              {\n                \"name\": \"internal\",\n                \"ports\": [\n                  6060,\n                  6061\n                ]\n              }\n            ]\n          }\n        }\n      },\n      {\n        \"hornetq\": {\n          \"order\": 3,\n          \"node\": {\n            \"region\": \"us-es\",\n            \"swapFile\": false,\n            \"minDisk\": 10,\n            \"minCores\": 4,\n            \"minRam\": 1024,\n            \"osType\": \"Red Hat\",\n            \"osVersion\": 6.5,\n            \"networks\": [\n              {\n                \"name\": \"internal\",\n                \"ports\": [\n                  6060,\n                  6061\n                ]\n              }\n            ]\n          }\n        }\n      }\n    ]\n  }\n}"
        //    println (JSON)
        FlowContextFactory.createFlowContext()


        if(i % 2 == 0){
          client.executeWithLoadBalancer(HttpRequest.newBuilder().uri("/test").httpMethod(HttpMethod.PUT).entity("testing new message").build())
        }else{
          client.executeWithLoadBalancer(HttpRequest.newBuilder().uri("/test").httpMethod(HttpMethod.PUT).entity(JSON).build())
        }
        countDown.countDown()
      }


    }

    countDown.await

    val resp = client.executeWithLoadBalancer(HttpRequest.newBuilder().uri("/test").httpMethod(HttpMethod.POST).entity("").build())
    println(resp.getHeaders.get(HttpServerFactory.FLOW_CONTEXT_HEADER))

    RunTestServer.stopServer

  }

}
