package com.cisco.oss.foundation.http.api.test

import scala.collection.JavaConversions._
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import org.scalatest.GivenWhenThen
import org.scalatest.FeatureSpec
import scala.collection.immutable.Stack
import org.eclipse.jetty.server.Server
import javax.servlet.Servlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.nds.cab.infra.configuration.ConfigurationFactory
import org.eclipse.jetty.client.ContentExchange
import org.eclipse.jetty.io.ByteArrayBuffer
import scala.collection.mutable.ListBuffer
import com.nds.cab.infra.flowcontext.FlowContextFactory
import com.nds.cab.infra.inet.utils.IpUtils
import com.google.common.collect.ArrayListMultimap
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import java.util.EventListener
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.ConfigurableWebApplicationContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.XmlWebApplicationContext
import com.nds.cab.infra.highavailability.server.HttpServerUtil
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.junit.Ignore
import org.glassfish.jersey.servlet.ServletContainer
import org.glassfish.jersey.server.ResourceConfig
import com.nds.cab.infra.highavailability.HighAvailabilityClientFactory
import com.nds.cab.infra.highavailability.server.HighAvailabilityServerFactory

@RunWith(classOf[JUnitRunner])
//@Ignore
class HttpServerRestTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter {

  val port = "3322"
  val name = "john"
  var sslfactory:SslContextFactory = null
  //  val host = localhost

  before {

    val config = ConfigurationFactory getConfiguration ()
    config setProperty ("service.serverTest1.http.port", port)
    config setProperty ("service.serverTest1.https.port", port+1)
    config setProperty ("service.serverTest1.http.requestValidityFilter.isEnabled", "true")
    config setProperty ("service.serverTest1.http.flowContextFilter.isEnabled", "true")
    config setProperty ("service.serverTest1.http.traceFilter.isEnabled", "true")
    config setProperty ("service.serverTest1.http.traceFilter.textContentTypes.1", "text/plain")
    config setProperty ("service.serverTest1.http.monitoringFilter.isEnabled", "false");
    
    
    config setProperty ("service.serverTest2.http.port", port+3)
    config setProperty ("service.serverTest2.https.port", port+4)
    config setProperty ("service.serverTest2.http.requestValidityFilter.isEnabled", "true")
    config setProperty ("service.serverTest2.http.flowContextFilter.isEnabled", "true")
    config setProperty ("service.serverTest2.http.traceFilter.isEnabled", "true")
    config setProperty ("service.serverTest2.http.traceFilter.textContentTypes.1", "text/plain")
    config setProperty ("service.serverTest2.http.monitoringFilter.isEnabled", "false");

    
    
    config setProperty ("service.serverTest1-client.http.readTimeout", "30000")
    config setProperty ("service.serverTest1-client.1.port", port)
    config setProperty ("service.serverTest1-client.1.host", IpUtils.getLocalHost())
    config setProperty ("service.serverTest1-clientssl.1.port", port)
    config setProperty ("service.serverTest1-clientssl.1.host", IpUtils.getLocalHost())
    
    val map:ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
//    val sh = new ServletContextHandler
    val resourceConfig = new ResourceConfig()
    resourceConfig packages("com.nds")

//    sh.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*")

//    sh.setInitParameter("com.sun.jersey.config.property.packages", "com.nds")
//    val webConfig = new XmlWebApplicationContext()
//    webConfig.setConfigLocation("classpath*:/META-INF/restContext.xml")
//    sh.setEventListeners(Array[EventListener](new ContextLoaderListener(webConfig)))
    
    val servlets:ArrayListMultimap[String,Servlet] = ArrayListMultimap.create()
    servlets.put("/*",new ServletContainer(resourceConfig) )
    
    val sh2 = new ServletContextHandler
    sh2.addServlet(new ServletHolder(new ServletContainer(new ResourceConfig(resourceConfig))), "/*")
//    sh2.setInitParameter("com.sun.jersey.config.property.packages", "com.nds")
//    val webConfig2 = new XmlWebApplicationContext()
//    webConfig2.setConfigLocation("classpath*:/META-INF/restContext.xml")
//    sh2.setEventListeners(Array[EventListener](new ContextLoaderListener(webConfig2)))
    
    
    val context = new ClassPathXmlApplicationContext("classpath*:/META-INF/httpTestContext.xml")
    sslfactory = context.getBean("sslContextFactory", classOf[SslContextFactory])
    val sslSocketConnector = new SslSelectChannelConnector(sslfactory)

//    HighAvailabilityServerFactory.startHttpServer("serverTest1", sh, sslSocketConnector)
    HighAvailabilityServerFactory.startHttpServer("serverTest1", servlets)
    HighAvailabilityServerFactory.startHttpServer("serverTest2", sh2)
    Thread.sleep(1000)
  }

  after {
    HighAvailabilityServerFactory.stopHttpServer("serverTest1")
    HighAvailabilityServerFactory.stopHttpServer("serverTest2")
  }

  feature("test InterfacesResource") {

    info("starting serverTest1 server")
    info("using MyServlet servlet")

    scenario("calling get method on uri /ps/ifs should get to my resource") {

      given("server is running")

      when("client sends a request")
      
      FlowContextFactory.createFlowContext()

      val client = HighAvailabilityClientFactory createHttpClient ("service.serverTest1-client", HighAvailabilityClientFactory.STRATEGY_TYPE.FAIL_OVER)
      val contentEx = new ContentExchange();
      contentEx setRequestURI ("/ps/ifs")
//      contentEx setRequestContent (new ByteArrayBuffer(name.getBytes()))
      contentEx setMethod ("GET")
      client send (contentEx)

      then("the server should return a message")

      contentEx waitForDone

      val result = contentEx getResponseContent ()
      assert(result != null)
      assert(result.contains("psSms"))


      val contentEx2 = new ContentExchange();
      contentEx2 setRequestURI ("/ps/ifs")
      //      contentEx setRequestContent (new ByteArrayBuffer(name.getBytes()))
      contentEx2 setMethod ("POST")
      contentEx2 setRequestContent (new ByteArrayBuffer("Hello Hello".getBytes()))
      contentEx2.setRequestContentType("text/plain")
      client send (contentEx2)
      contentEx2 waitForDone

    }
    
//    scenario("calling get method on uri /ps/ifs should get to my resource - using SSL") {
//
//      given("server is running")
//
//      when("client sends a request")
//      
//      FlowContextFactory.createFlowContext()
//
//      val client = HighAvailabilityClientFactory createHttpClient ("service.serverTest1-clientssl", FAIL_OVER, sslfactory)
//      val contentEx = new ContentExchange();
//      contentEx setRequestURI ("/ps/ifs")
////      contentEx setRequestContent (new ByteArrayBuffer(name.getBytes()))
//      contentEx setMethod ("GET")
//      client send (contentEx)
//
//      then("the server should return a message")
//
//      contentEx waitForDone
//
//      val result = contentEx getResponseContent ()
//      assert(result != null)
//      assert(result.contains("psSms"))
//
//
//
//    }

        
  }
}

