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

import java.io.PrintWriter;

public class TraceServletWriter extends PrintWriter {

    private final static String LINESEP = System.lineSeparator();
    private final static int LINESEPLEN = LINESEP.length();
    private final PrintWriter delegate;
    private final TraceLogger tracer;
    private final StringBuilder builder = new StringBuilder();

    public TraceServletWriter(PrintWriter delegate, TraceLogger tracer) {
        super(delegate);
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public void close() {
        tracer.logResponseContentClose();
        tracer.log("Response Content: " + builder.toString());
        tracer.log("Closed: {}", delegate);
        super.close();
    }

    @Override
    public void flush() {
        tracer.log("Flushed: {}", delegate);
        super.flush();
    }

    @Override
    public void println() {
        tracer.logResponseContentChar(LINESEP, 0, LINESEPLEN);
        super.println();
    }

    @Override
    public void write(char[] buf, int off, int len) {
        tracer.logResponseContentChar(buf, off, len);
        super.write(buf, off, len);
    }

    @Override
    public void write(int c) {
        tracer.logResponseContentChar(c);
        super.write(c);
    }

    @Override
    public void write(String s, int off, int len) {
        builder.append(s);
//        tracer.logResponseContentChar(s, off, len);
        super.write(s, off, len);
    }
}