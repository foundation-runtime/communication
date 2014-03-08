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

package com.cisco.oss.foundation.http.server.jetty.filters;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class TraceServletRequest extends HttpServletRequestWrapper
{
    private final TraceLogger tracer;
    private TraceServletInputStream stream;
    private TraceServletReader reader;

    public TraceServletRequest(HttpServletRequest request, TraceLogger tracer)
    {
        super(request);
        this.tracer = tracer;
        this.tracer.logRequestHeaders(request);
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        tracer.log("Get Reader");
        if (reader != null)
        {
            return reader;
        }
        // make sure servlet spec is still followed
        if (stream != null)
        {
            throw new IllegalStateException("getInputStream() previously called");
        }
        BufferedReader delegate = super.getReader();
        this.reader = new TraceServletReader(delegate,tracer);
        return this.reader;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        if (stream != null)
        {
            return stream;
        }
        // make sure servlet spec is still followed
        if (reader != null)
        {
            throw new IllegalStateException("getReader() previously called");
        }
        ServletInputStream delegate = super.getInputStream();
        this.stream = new TraceServletInputStream(delegate,tracer);
        return this.stream;
    }
}
