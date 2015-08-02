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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

public class TraceServletInputStream extends ServletInputStream {
    private final ServletInputStream delegate;
    private final TraceLogger tracer;
    private final StringBuilder builder = new StringBuilder();
    private boolean isClosed = false;

    public TraceServletInputStream(ServletInputStream stream, TraceLogger tracer) {
        this.delegate = stream;
        this.tracer = tracer;
    }

    @Override
    public boolean isFinished() {
        return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        delegate.setReadListener(readListener);
    }

    @Override
    public int read() throws IOException {
        try {
            int ret = delegate.read();
            if (ret != (-1)) {
                builder.append((char)ret);
//                tracer.logRequestContentByte((byte) ret);
            } else {
//                tracer.log("Content: {}", builder.toString());
//                tracer.log("EOF reached on {}", delegate);
            }
            return ret;
        } catch (IOException e) {
            tracer.log(e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            tracer.logRequestContentClose();
            try {
                delegate.close();
                tracer.log("Request Content: {}", builder.toString());
                tracer.log("Closed: {}", delegate);
                isClosed = true;
            } catch (IOException e) {
                tracer.log(e);
                throw e;
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            int ret = delegate.read(b, off, len);
            if (ret != (-1)) {
                for (int i = off; i < ret; i++) {
                    char ch = (char) ((byte) b[i]);
                    builder.append(ch);
                }

//                tracer.logRequestContentByte((byte) ret);
            } else {
//                tracer.log("Content: {}", builder.toString());
//                tracer.log("EOF reached on {}", delegate);
            }
            return ret;
        } catch (IOException e) {
            tracer.log(e);
            throw e;
        }
    }
}
