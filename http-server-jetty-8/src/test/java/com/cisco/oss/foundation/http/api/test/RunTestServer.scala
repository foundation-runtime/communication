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

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import java.io.{InputStreamReader, BufferedReader}
import scala.Int
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory
import com.google.common.collect.{ArrayListMultimap}
import javax.servlet.Servlet
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * Created by Yair Ogen on 1/20/14.
 */
object RunTestServer {

  def runServer() = {
    val servlets:ArrayListMultimap[String,Servlet] = ArrayListMultimap.create()
    servlets.put("/*", new ServletTester)
    JettyHttpServerFactory.INSTANCE.startHttpServer("serverTest", servlets)
  }

}

class ServletTester extends HttpServlet{

  val LOGGER  = LoggerFactory.getLogger(getClass)


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
    val body = getBody(req)
    LOGGER.info("doPut: {}", body);
    resp.setDateHeader("Date", (new Date).getTime)
    resp.getWriter.write(body)
//    Thread.sleep(2500)
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
