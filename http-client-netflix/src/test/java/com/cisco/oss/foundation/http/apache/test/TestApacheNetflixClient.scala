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

import java.security.cert.X509Certificate
import javax.net.ssl.{SSLSession, SSLSocket}

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http.netlifx.apache.{ApacheNetflixHttpResponse, ApacheNetflixHttpClientFactory}
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.http.api.test.{BasicHttpTestUtil, BasicHttpsTestUtil, MultithreadTestUtil}
import com.cisco.oss.foundation.loadbalancer.{LoadBalancerStrategy, NoActiveServersException, RequestTimeoutException}
import org.apache.commons.configuration.{Configuration, PropertiesConfiguration}
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.junit.{Assert, Test}
import org.slf4j.LoggerFactory

/**
 * Created by Yair Ogen on 14/12/2015.
 */
class TestApacheNetflixClient {
  val httpTestUtil = new BasicHttpTestUtil[HttpRequest,ApacheNetflixHttpResponse]
  val httpsTestUtil = new BasicHttpsTestUtil[HttpRequest,ApacheNetflixHttpResponse]
  val httpMultiThreadTestUtil = new MultithreadTestUtil[HttpRequest,ApacheNetflixHttpResponse]
  val propsConfiguration: Configuration = new PropertiesConfiguration(classOf[TestApacheNetflixClient].getResource("/config.properties"))
  val clientTest = ApacheNetflixHttpClientFactory.createHttpClient("clientTest", propsConfiguration)
//  val clientTest = ApacheNetflixHttpClientFactory.createHttpClient("clientTest", propsConfiguration)
  private val verifier: X509HostnameVerifier with Object {def verify(s: String, sslSession: SSLSession): Boolean; def verify(host: String, cert: X509Certificate): Unit; def verify(host: String, ssl: SSLSocket): Unit; def verify(host: String, cns: Array[String], subjectAlts: Array[String]): Unit} = new X509HostnameVerifier {
    override def verify(host: String, ssl: SSLSocket): Unit = {}

    override def verify(host: String, cert: X509Certificate): Unit = {}

    override def verify(host: String, cns: Array[String], subjectAlts: Array[String]): Unit = {}

    override def verify(s: String, sslSession: SSLSession): Boolean = true
  }
//  val clientHttpsTest = ApacheNetflixHttpClientFactory.createHttpClient("clientHttpsTest", propsConfiguration, verifier)

  val LOGGER = LoggerFactory.getLogger(classOf[TestApacheNetflixClient])


  @Test
  def realServerInvokePostRoudRobin() {

    val clientRoundRobinTest = ApacheNetflixHttpClientFactory.createHttpClient("clientRoundRobinSyncTest", propsConfiguration)
//    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest, propsConfiguration)
val request = HttpRequest.newBuilder()
  .httpMethod(HttpMethod.POST)
  .uri("/test")
  .entity("hello")
  .contentType("text/plain")
  .build();

    FlowContextFactory.createFlowContext()

    var response = clientRoundRobinTest.executeWithLoadBalancer(request)
    val str = response.getResponseAsString
    Assert.assertEquals(true,response.isSuccess)
    Assert.assertEquals("hello",str)

    response = clientRoundRobinTest.executeWithLoadBalancer(request)
    val bytes = response.getResponse
    Assert.assertEquals(true,response.isSuccess)
    response = clientRoundRobinTest.executeWithLoadBalancer(request)
    Assert.assertEquals(true,response.isSuccess)
    response = clientRoundRobinTest.executeWithLoadBalancer(request)
    Assert.assertEquals(true,response.isSuccess)
    response = clientRoundRobinTest.executeWithLoadBalancer(request)
    Assert.assertEquals(true,response.isSuccess)

  }


}


