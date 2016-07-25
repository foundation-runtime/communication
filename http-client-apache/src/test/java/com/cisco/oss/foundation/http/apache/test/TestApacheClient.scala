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

import org.apache.http.conn.ssl.X509HostnameVerifier
import org.slf4j.LoggerFactory
import org.junit.{Ignore, Test}
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.http.apache.{ApacheHttpClientFactory, ApacheHttpRequest, ApacheHttpResponse}
import com.cisco.oss.foundation.loadbalancer.{LoadBalancerStrategy, NoActiveServersException, RequestTimeoutException}
import com.cisco.oss.foundation.http.api.test.{BasicHttpTestUtil, BasicHttpsTestUtil, MultithreadTestUtil}
import org.apache.commons.configuration.{Configuration, PropertiesConfiguration}
import org.apache.http.HttpEntity
import org.apache.http.entity.mime.MultipartEntityBuilder

/**
 * Created by Yair Ogen on 1/19/14.
 */
class TestApacheClient {
  val httpTestUtil = new BasicHttpTestUtil[HttpRequest,ApacheHttpResponse]
  val httpsTestUtil = new BasicHttpsTestUtil[HttpRequest,ApacheHttpResponse]
  val httpMultiThreadTestUtil = new MultithreadTestUtil[HttpRequest,ApacheHttpResponse]
  val propsConfiguration: Configuration = new PropertiesConfiguration(classOf[TestApacheClient].getResource("/config.properties"))
  val clientTest = ApacheHttpClientFactory.createHttpClient("clientTest", propsConfiguration)
  private val verifier: X509HostnameVerifier with Object {def verify(s: String, sslSession: SSLSession): Boolean; def verify(host: String, cert: X509Certificate): Unit; def verify(host: String, ssl: SSLSocket): Unit; def verify(host: String, cns: Array[String], subjectAlts: Array[String]): Unit} = new X509HostnameVerifier {
    override def verify(host: String, ssl: SSLSocket): Unit = {}

    override def verify(host: String, cert: X509Certificate): Unit = {}

    override def verify(host: String, cns: Array[String], subjectAlts: Array[String]): Unit = {}

    override def verify(s: String, sslSession: SSLSession): Boolean = true
  }
  val clientHttpsTest = ApacheHttpClientFactory.createHttpClient("clientHttpsTest", propsConfiguration, verifier)

  val LOGGER = LoggerFactory.getLogger(classOf[TestApacheClient])

  @Test
  @Ignore
  def testMultiPart(): Unit ={
    val requestBuilder = ApacheHttpRequest.newBuilder();
    val multipartEntityBuilder = MultipartEntityBuilder.create()
    val httpEntity = multipartEntityBuilder.build()

    val httpRequest: ApacheHttpRequest = requestBuilder
      .uri("")
        .httpMethod(HttpMethod.POST)
      .contentType("mime")
      .apacheEntity(httpEntity)
      .build()

    ApacheHttpClientFactory.createHttpClient("clientTest", propsConfiguration)

    clientTest.execute(httpRequest);
  }

  @Test
  def basicGoogleFetch() {
    httpTestUtil.basicGoogleFetch(clientTest)
  }

  @Test(expected = classOf[NoActiveServersException])
  def realServerInvokeAndFail() {
    httpTestUtil.realServerInvokeAndFail(clientTest)
  }


  @Test
  def realServerInvokeGet() {
    httpTestUtil.realServerInvokeGet(clientTest)
  }

  @Test
  def realHttpsServerInvokeGet() {
    httpsTestUtil.realHttpsServerInvokeGet(clientHttpsTest)
  }


  @Test
  def realServerInvokePostRoudRobin() {
    val strategy: CABFileChangedReloadingStrategy = new CABFileChangedReloadingStrategy
    strategy.setRefreshDelay(3000)
    propsConfiguration.asInstanceOf[PropertiesConfiguration].setReloadingStrategy(strategy)

    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinSyncTest", propsConfiguration)
    httpTestUtil.realServerInvokePostRoudRobin(clientRoundRobinTest, propsConfiguration)

  }

  @Test
  def realServerInvokePostFailOver() {

    val clientFailOverTest = ApacheHttpClientFactory.createHttpClient("clientFailOverTest", LoadBalancerStrategy.STRATEGY_TYPE.FAIL_OVER, propsConfiguration)
    httpTestUtil.realServerInvokePostFailOver(clientFailOverTest)

  }

  @Test (expected = classOf[RequestTimeoutException])
  def timeoutTest(){

    httpTestUtil.timeoutTest(clientTest)


  }


  @Test
  def testUsingActors() {
    val clientRoundRobinTest = ApacheHttpClientFactory.createHttpClient("clientRoundRobinTest", propsConfiguration)
    httpMultiThreadTestUtil.testUsingActors(clientRoundRobinTest)
  }

}


