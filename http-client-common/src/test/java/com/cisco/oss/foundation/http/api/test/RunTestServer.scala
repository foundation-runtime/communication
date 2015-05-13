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

import java.io.{BufferedReader, InputStreamReader}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import org.eclipse.jetty.server._
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory

/**
 * Created by Yair Ogen on 1/20/14.
 */
object RunTestServer {

  def runServer(port:Int):Server = {
    val server = new Server(port)

    val handler = new ServletHandler();
    server.setHandler(handler);
    handler.addServletWithMapping(classOf[ServletTester], "/test");
    server.start();

    server
  }

  def runHTTPSServer(port:Int):Server = {
    val server = new Server(port)

    val sslContextFactory: SslContextFactory = new SslContextFactory
    sslContextFactory.setKeyStorePath("server.jks")
    sslContextFactory.setKeyStorePassword("foundation-agent")
//    sslContextFactory.setpas

    val sslSelectChannelConnector = new SslSelectChannelConnector(sslContextFactory)
    sslSelectChannelConnector.setHost("localhost")
    sslSelectChannelConnector.setPort(port)


    val handler = new ServletHandler();
    server.setHandler(handler);
    handler.addServletWithMapping(classOf[ServletTester], "/test");
    server.setConnectors(Array[Connector](sslSelectChannelConnector))
    server.start();

    server
  }

}

class ServletTester extends HttpServlet{

  val LOGGER  = LoggerFactory.getLogger(classOf[ServletTester])


  override def service(req: HttpServletRequest, resp: HttpServletResponse) = {
    val fc = req.getHeader("FLOW_CONTEXT")
    FlowContextFactory.deserializeNativeFlowContext(fc)
    super.service(req,resp)
  }

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    LOGGER.info("doGet");
    resp.getWriter.write("dummy response")
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
//    LOGGER.info("doPost");
    LOGGER.info("request: {}; content-type: {}; content: {}", req.getRequestURL, req.getContentType, getBody(req));
    resp.getWriter.write(req.getHeader("Host"))
  }

  override def doPut(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    LOGGER.info("doPut");
    Thread.sleep(2500)
  }
  def getBody(request: HttpServletRequest):String = {

    var body:String = null;
    val stringBuilder = new StringBuilder();
    var bufferedReader:BufferedReader = null;

//    try {
      val inputStream = request.getInputStream();
      if (inputStream != null) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        return bufferedReader.readLine()
      }
    return "";

  }
}
