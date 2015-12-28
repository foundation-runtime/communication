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

import java.io.IOException
import java.nio.charset.Charset
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLSession, SSLSocket}

import com.cisco.oss.foundation.configuration.ConfigurationFactory
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http.api.test.{BasicHttpTestUtil, BasicHttpsTestUtil, MultithreadTestUtil}
import com.cisco.oss.foundation.http.netlifx.apache.ApacheNetflixHttpResponse
import com.cisco.oss.foundation.http.netlifx.netty.NettyNetflixHttpClientFactory
import com.cisco.oss.foundation.http.{HttpRequest}
import io.netty.handler.codec.http.HttpMethod
import com.netflix.appinfo.{MyDataCenterInstanceConfig, EurekaInstanceConfig}
import com.netflix.client.{RetryHandler, IClientConfigAware}
import com.netflix.client.config.{IClientConfig, DefaultClientConfigImpl}
import com.netflix.config.ConfigurationManager
import com.netflix.discovery.{DiscoveryManager, DefaultEurekaClientConfig, EurekaClientConfig}
import com.netflix.loadbalancer.{DynamicServerListLoadBalancer, ILoadBalancer}
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer
import com.netflix.ribbon.transport.netty.RibbonTransport
import com.netflix.ribbon.transport.netty.http.LoadBalancingHttpClient
import io.netty.buffer.ByteBuf
import io.reactivex.netty.protocol.http.client.{HttpClientResponse, HttpClientRequest}
import org.apache.commons.configuration.{AbstractConfiguration, Configuration, PropertiesConfiguration}
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.junit.{Ignore, Assert, Test}
import org.slf4j.LoggerFactory
import rx.exceptions.OnErrorNotImplementedException
import rx.schedulers.Schedulers
import rx.{Subscriber, Subscription, Observable}
import rx.functions.Action1

/**
 * Created by Yair Ogen on 14/12/2015.
 */
class TestNetflixNettyClient {

  val LOGGER = LoggerFactory.getLogger(classOf[TestNetflixNettyClient])

  val clientRoundRobinTest = NettyNetflixHttpClientFactory.createHttpClient("clientRoundRobinSyncTest")

  private val body: String = "hello1"

  @Ignore
  @Test
  def testWithFactory() ={


    FlowContextFactory.createFlowContext()

    val request = HttpRequest.newBuilder()
      .httpMethod(com.cisco.oss.foundation.http.HttpMethod.POST)
      .uri("/test")
      .entity(body)
      .contentType("text/plain")
      .header("Accept","text/plain")
      .build();


    var response = clientRoundRobinTest.executeWithLoadBalancer(request)
    val str = response.getResponseAsString
    Assert.assertEquals(true,response.isSuccess)
    Assert.assertEquals(body,str)

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

  @Ignore
  @Test
  def testGet(): Unit ={
    FlowContextFactory.createFlowContext()

    val request = HttpRequest.newBuilder()
      .httpMethod(com.cisco.oss.foundation.http.HttpMethod.GET)
      .uri("/test")
      .queryParams("param1", body)
      .header("Accept","text/plain")
      .build();


    var response = clientRoundRobinTest.executeWithLoadBalancer(request)
    val str = response.getResponseAsString
    Assert.assertEquals(true,response.isSuccess)
    Assert.assertEquals(body,str)
  }


  @Ignore
  @Test
  def realServerInvokePostRoudRobin() {
//    val strategy: CABFileChangedReloadingStrategy = new CABFileChangedReloadingStrategy
//    strategy.setRefreshDelay(3000)
//    propsConfiguration.asInstanceOf[PropertiesConfiguration].setReloadingStrategy(strategy)
    if (!ConfigurationManager.isConfigurationInstalled) {
      ConfigurationManager.install(ConfigurationFactory.getConfiguration.asInstanceOf[AbstractConfiguration])
    }

    val clientConfig = new DefaultClientConfigImpl()
    val apiName = "clientRoundRobinSyncTest"
    clientConfig.loadProperties(apiName)

    val eurekaInstanceConfig: EurekaInstanceConfig = new MyDataCenterInstanceConfig(apiName)
    val eurekaClientConfig: EurekaClientConfig = new DefaultEurekaClientConfig(apiName + ".")
    DiscoveryManager.getInstance.initComponent(eurekaInstanceConfig, eurekaClientConfig)


    val loadBalancer: ILoadBalancer = new DynamicServerListLoadBalancer[DiscoveryEnabledServer]
    (loadBalancer.asInstanceOf[IClientConfigAware]).initWithNiwsConfig(clientConfig)

    val httpClient: LoadBalancingHttpClient[ByteBuf, ByteBuf] = RibbonTransport.newHttpClient(loadBalancer,clientConfig)

    FlowContextFactory.createFlowContext()
    val flowContext1: String = FlowContextFactory.serializeNativeFlowContext()
    println(flowContext1)

    val clientRequest: HttpClientRequest[ByteBuf] = HttpClientRequest.create(HttpMethod.POST,"/test")
    clientRequest.withContent(body.getBytes(Charset.defaultCharset)).withHeader("Content-Type","text/plain").withHeader("FLOW_CONTEXT",flowContext1)

    val retryHandler: RetryHandler with Object {
      def getMaxRetriesOnSameServer: Int;
      def isCircuitTrippingException(e: Throwable): Boolean;
      def isRetriableException(e: Throwable, sameServer: Boolean): Boolean;
      def getMaxRetriesOnNextServer: Int} = new RetryHandler {
      override def getMaxRetriesOnSameServer: Int = 2

      override def getMaxRetriesOnNextServer: Int = 2

      override def isCircuitTrippingException(e: Throwable): Boolean =  if (e.isInstanceOf[IOException]) true  else false

      override def isRetriableException(e: Throwable, sameServer: Boolean): Boolean = if (e.isInstanceOf[IOException]) true else false
    }

    import java.util.function.{ Function ⇒ JFunction, Predicate ⇒ JPredicate, BiPredicate }

    //usage example: `i: Int ⇒ 42`
    implicit def toJavaFunction[A, B](f: Function[A, B]) = new JFunction[A, B] {
      override def apply(a: A): B = f(a)
    }

    val responseObservableSync: Observable[HttpClientResponse[ByteBuf]] = httpClient.submit(clientRequest, retryHandler, clientConfig)
    val response =  responseObservableSync.toBlocking.first()
    LOGGER.info("status: {}", response.getStatus)
    response.getContent.toBlocking.first();

//    responseObservable.doOnError(new Action1[Throwable] {
//
//      override def call(t: Throwable): Unit = {
//        t.printStackTrace()
//      }
//
//    })

//    sendRequestAsync()

    def sendRequestAsync(): Unit = {

      FlowContextFactory.createFlowContext()
      val flowContext2: String = FlowContextFactory.serializeNativeFlowContext()
      println(flowContext2)

      val clientRequestAsync: HttpClientRequest[ByteBuf] = HttpClientRequest.create(HttpMethod.POST,"/test")
      clientRequestAsync.withContent("hello".getBytes(Charset.defaultCharset)).withHeader("Content-Type","text/plain").withHeader("FLOW_CONTEXT",flowContext2)
      val responseObservable: Observable[HttpClientResponse[ByteBuf]] = httpClient.submit(clientRequestAsync, retryHandler, clientConfig)
      responseObservable.subscribe(new Subscriber[HttpClientResponse[ByteBuf]]() {
        final def onCompleted {
        }

        final def onError(e: Throwable) {
          LOGGER.error("error serving request: {}", e, e)
        }

        final def onNext(response: HttpClientResponse[ByteBuf]) {
          val status = response.getStatus
          LOGGER.info("status: {}", response.getStatus)
        }
      });
      Thread.sleep(2000)
    }

    //    val subscribription: Subscription = responseObservable.subscribe(new Action1[HttpClientResponse[ByteBuf]] {
    //      override def call(t: HttpClientResponse[ByteBuf]): Unit = {
    //        val status = t.getStatus
    //        println(status)
    //      }
    //    })


//    response.map((resp:HttpClientResponse[ByteBuf]) => println(""))



//    var response = clientRoundRobinTest.executeWithLoadBalancer(request)
//    val str = response.getResponseAsString
//    Assert.assertEquals(true,response.isSuccess)
//    Assert.assertEquals("hello",str)
//
//    response = clientRoundRobinTest.executeWithLoadBalancer(request)
//    val bytes = response.getResponse
//    Assert.assertEquals(true,response.isSuccess)
//    response = clientRoundRobinTest.executeWithLoadBalancer(request)
//    Assert.assertEquals(true,response.isSuccess)
//    response = clientRoundRobinTest.executeWithLoadBalancer(request)
//    Assert.assertEquals(true,response.isSuccess)
//    response = clientRoundRobinTest.executeWithLoadBalancer(request)
//    Assert.assertEquals(true,response.isSuccess)

  }

  //    val next: Observable[HttpClientResponse[ByteBuf]] = response.doOnNext(new Action1[HttpClientResponse[ByteBuf]] {
  //      override def call(t: HttpClientResponse[ByteBuf]): Unit = {
  //        val status = t.getStatus
  //        println(status)
  //      }
  //    })

  //    val on: Observable[HttpClientResponse[ByteBuf]] = response.subscribeOn(Schedulers.immediate()).doOnNext(new Action1[HttpClientResponse[ByteBuf]] {
  //      override def call(t: HttpClientResponse[ByteBuf]): Unit = {
  //        val status = t.getStatus
  //        println(status)
  //      }
  //    })



}


