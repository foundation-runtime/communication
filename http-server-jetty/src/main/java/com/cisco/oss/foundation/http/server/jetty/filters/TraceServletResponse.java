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

package com.cisco.oss.foundation.http.server.jetty.filters;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class TraceServletResponse extends HttpServletResponseWrapper
{
    private final TraceLogger tracer;
    private TraceServletOutputStream stream;
    private TraceServletWriter writer;

    public TraceServletResponse(HttpServletResponse response, TraceLogger tracer)
    {
        super(response);
        this.tracer = tracer;
        // You might be tempted to log the response headers here
        // but don't, as the response is not yet committed.
        // As strange as it sounds, it would be wiser to to
        // write out the response headers after the Response has started.
        this.tracer.setResponse(response);
    }

    @Override
    public void flushBuffer() throws IOException
    {
        tracer.log("Response.flushBuffer()");
        tracer.logResponseFlush();
        super.flushBuffer();
    }

    @Override
    public void reset()
    {
        tracer.log("Response.reset()");
        tracer.logResponseReset();
        super.reset();
    }

    @Override
    public void resetBuffer()
    {
        tracer.log("Response.resetBuffer()");
        tracer.logResponseContentReset();
        super.resetBuffer();
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        tracer.logResponseError(sc,null);
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        tracer.logResponseError(sc,msg);
        super.sendError(sc,msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        tracer.logResponseRedirect(location);
        super.sendRedirect(location);
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        tracer.log("Get OutputStream");
        if (stream != null)
        {
            return stream;
        }
        // make sure servlet spec is still followed
        if (writer != null)
        {
            throw new IllegalStateException("getWriter() previously called");
        }
        ServletOutputStream delegate = super.getOutputStream();
        this.stream = new TraceServletOutputStream(delegate,tracer);
        return this.stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        if (writer != null)
        {
            return writer;
        }
        // make sure servlet spec is still followed
        if (stream != null)
        {
            throw new IllegalStateException("getOutputStream() previously called");
        }

        PrintWriter delegate = super.getWriter();
        this.writer = new TraceServletWriter(delegate,tracer);
        return this.writer;
    }
}
