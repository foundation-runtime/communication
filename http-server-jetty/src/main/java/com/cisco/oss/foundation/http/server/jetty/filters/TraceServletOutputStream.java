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

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class TraceServletOutputStream extends ServletOutputStream
{
    private final ServletOutputStream delegate;
    private final TraceLogger tracer;

    public TraceServletOutputStream(ServletOutputStream delegate, TraceLogger tracer)
    {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            tracer.logResponseContentClose();
            super.close();
            tracer.log("Closed: {}",delegate);
        }
        catch (IOException e)
        {
            tracer.log(e);
            throw e;
        }
    }

    @Override
    public void flush() throws IOException
    {
        try
        {
            super.flush();
            tracer.log("Flushed: {}",delegate);
        }
        catch (IOException e)
        {
            tracer.log(e);
            throw e;
        }
    }

    @Override
    public boolean isReady()
    {
        return delegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener)
    {
        this.delegate.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException
    {
        try
        {
            tracer.logResponseContentByte(b);
            delegate.write(b);
        }
        catch (IOException e)
        {
            tracer.log(e);
            throw e;
        }
    }
}