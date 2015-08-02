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

import java.io.BufferedReader;
import java.io.IOException;

public class TraceServletReader extends BufferedReader {
    private final BufferedReader delegate;
    private final TraceLogger tracer;
    private final StringBuilder builder = new StringBuilder();

    public TraceServletReader(BufferedReader delegate, TraceLogger tracer) {
        super(delegate);
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public void close() throws IOException {
        try {
            tracer.logRequestContentClose();
            super.close();
            tracer.log("Request Content: {}", builder.toString());
//            tracer.log("Closed: {}", delegate);
        } catch (IOException e) {
            tracer.log(e);
            throw e;
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        builder.append(cbuf);
        return read;
    }

    @Override
    public int read() throws IOException {
        try {
            int ret = super.read();

            if (ret != (-1)) {
                builder.append((char) ret);
//                tracer.logRequestContentChar((char)ret);
            } else {
//                tracer.log("Content: {}", builder.toString());
//                tracer.log("EOF reached on {}",delegate);
            }

            return ret;
        } catch (IOException e) {
            tracer.log(e);
            throw e;
        }
    }
}