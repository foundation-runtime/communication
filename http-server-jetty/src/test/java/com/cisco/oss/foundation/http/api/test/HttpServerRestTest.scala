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

import org.junit.runner.RunWith
import javax.servlet.Servlet
import com.google.common.collect.ArrayListMultimap
import org.eclipse.jetty.util.ssl.SslContextFactory
import com.cisco.oss.foundation.configuration.ConfigurationFactory
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.cisco.oss.foundation.ip.utils.IpUtils
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory
import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}

//class MyLogFormatter extends Formatter {
//
//  val messageFormat = new MessageFormat("{3} [{0}] [{2}]: {1}: {5} {4} \n");
//
//  val dateFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy/MM/dd HH:mm:ss.SSS").toFormatter
//
//  @Override
//  def format(record: LogRecord): String = {
//    val arguments = Array(
//      truncateLoggerName(record.getLoggerName(),1),
//      record.getLevel(),
//      Thread.currentThread().getName(),
//      (new DateTime(record.getMillis(), DateTimeZone.UTC)).toString(dateFormatter),
//      record.getMessage(),
//      Option(FlowContextFactory.getFlowContext).getOrElse("").toString)
//    return messageFormat.format(arguments);
//  }
//
//  def truncateLoggerName(n: String, precision: Int): String = {
//    val parts = n.split('.')
//    return parts.reverse.head
//  }
//
//}

//object HttpServerRestTest{
//  LogManager.getLogManager().reset();
//
//
//  val fh = new FileHandler("./log/http-server-jetty.log");
//  fh.setLevel(Level.FINE);
//  fh.setFormatter(new MyLogFormatter)
//
//  val globalLogger = Logger.getLogger("")
//  globalLogger.addHandler(fh)
//  globalLogger.setLevel(Level.SEVERE)
//
//
//  Logger.getLogger("org.glassfish").setLevel(Level.FINEST)
//
//}

@RunWith(classOf[JUnitRunner])
//@Ignore
class HttpServerRestTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter {

  val port = "3322"
  val name = "john"
  var sslfactory: SslContextFactory = null
  //  val host = localhost

  before {

//    Logger.getLogger("org.glassfish").setLevel(Level.FINEST)

    val config = ConfigurationFactory getConfiguration()
    config setProperty("serverTest1.http.port", port)
    config setProperty("serverTest1.https.port", port + 1)
    config setProperty("serverTest1.http.requestValidityFilter.isEnabled", "true")
    config setProperty("serverTest1.http.flowContextFilter.isEnabled", "true")
    config setProperty("serverTest1.http.traceFilter.isEnabled", "true")
    config setProperty("serverTest1.http.traceFilter.textContentTypes.1", "text/plain")
    config setProperty("serverTest1.http.monitoringFilter.isEnabled", "true");
    config setProperty("serverTest1.http.serviceDirectory.isEnabled", "true");


    config setProperty("service.serverTest1-client.http.serviceDirectory.isEnabled", "true");
    config setProperty("service.serverTest1-client.http.readTimeout", "30000")
    config setProperty("service.serverTest1-client.http.serviceDirectory.serviceName", "serverTest1");
    //    config setProperty("service.serverTest1-client.1.port", port)
//    config setProperty("service.serverTest1-client.1.host", IpUtils.getIpAddress)

    val map: ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
    //    val scripts.sh = new ServletContextHandler
    val resourceConfig = new ResourceConfig()
    resourceConfig packages ("com.cisco")

    val servlets: ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
    servlets.put("/*", new ServletContainer(resourceConfig))


    JettyHttpServerFactory.INSTANCE.startHttpServer("serverTest1", servlets)
    Thread.sleep(1000)
  }

  after {
    JettyHttpServerFactory.INSTANCE.stopHttpServer("serverTest1")
  }

  feature("test InterfacesResource") {

    info("starting serverTest1 server")
    info("using MyServlet servlet")

    scenario("calling get method on uri /ps/ifs should get to my resource") {

      given("server is running")

      when("client sends a request")

      FlowContextFactory.createFlowContext()

      val client = ApacheHttpClientFactory.createHttpClient("service.serverTest1-client")
      val request = HttpRequest.newBuilder()
        .uri("/ps/ifs")
        .httpMethod(HttpMethod.POST)
        .entity("hello *************")
        .build()

      val result = client.execute(request)

      then("the server should return a message")


      assert(result != null)
      assert(result.getResponseAsString.contains("psSms"))


    }


  }

  feature("test InterfacesResource with SD support") {

    info("starting serverTest1 server")
    info("using MyServlet servlet")

    scenario("calling get return resource result") {

      given("server is running")

      when("client sends a request")

      FlowContextFactory.createFlowContext()

      val client = ApacheHttpClientFactory.createHttpClient("service.serverTest1-client")
      val request = HttpRequest.newBuilder()
        .uri("/ps/ifs")
        .httpMethod(HttpMethod.POST)
        .entity("hello *************")
        .build()

      println("first invoke")
      val result = client.execute(request)
      println("post first invoke")

      then("the server should return a message")


      assert(result != null)
      assert(result.getResponseAsString.contains("psSms"))

      JettyHttpServerFactory.INSTANCE.stopHttpServer("serverTest1")

      Thread.sleep(12500)

      println("second invoke")
      client.execute(request)
      println("post second invoke")
    }


  }
}

