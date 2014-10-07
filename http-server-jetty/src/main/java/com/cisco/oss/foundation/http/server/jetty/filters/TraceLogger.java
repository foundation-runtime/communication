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

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Enumeration;
import java.util.Map;

public class TraceLogger implements Closeable, AsyncListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceLogger.class);
    private HttpServletResponse response;
    private CharContentLogFormatter requestContentCharFormatter;
    private ByteContentLogFormatter requestContentByteFormatter;
    private CharContentLogFormatter responseContentCharFormatter;
    private ByteContentLogFormatter responseContentByteFormatter;

    @Override
    public void close() {
        logRequestContentClose();
        logResponseContentClose();
        if (response != null) {
            logResponseHeaders();
        }
    }

    public void log(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    public void log(Throwable t) {
        LOGGER.error(t.toString(), t);
    }

    private synchronized ByteContentLogFormatter getRequestContentByteFormatter() {
        if (requestContentByteFormatter == null) {
            requestContentByteFormatter = new ByteContentLogFormatter("Request");
        }
        return requestContentByteFormatter;
    }

    private synchronized CharContentLogFormatter getRequestContentCharFormatter() {
        if (requestContentCharFormatter == null) {
            requestContentCharFormatter = new CharContentLogFormatter("Request");
        }
        return requestContentCharFormatter;
    }

    private synchronized ByteContentLogFormatter getResponseContentByteFormatter() {
        if (responseContentByteFormatter == null) {
            responseContentByteFormatter = new ByteContentLogFormatter("Response");
        }
        return responseContentByteFormatter;
    }

    private synchronized CharContentLogFormatter getResponseContentCharFormatter() {
        if (responseContentCharFormatter == null) {
            responseContentCharFormatter = new CharContentLogFormatter("Response");
        }
        return responseContentCharFormatter;
    }

    public void logRequestContentByte(byte b) {
        getRequestContentByteFormatter().dump(b);
    }

    public void logRequestContentChar(char ret) {
        getRequestContentCharFormatter().dump(ret);
    }

    public void logRequestContentClose() {
        if (requestContentByteFormatter != null) {
            requestContentByteFormatter.close();
        }
        if (requestContentCharFormatter != null) {
            requestContentCharFormatter.close();
        }
    }

    public void logRequestHeaders(HttpServletRequest httpReq) {

        StringBuilder builder = new StringBuilder("Request Headers:\n");

        Enumeration<String> enames = httpReq.getHeaderNames();
        while (enames.hasMoreElements()) {
            String name = enames.nextElement();
            builder.append("\t").append(name).append(": ").append(httpReq.getHeader(name)).append("\n");
        }
        LOGGER.debug(builder.toString());

        Map<String, String[]> params = httpReq.getParameterMap();
        if ((params != null) && (params.size() > 0)) {
            LOGGER.debug(" (request parameters)");
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String value = Joiner.on(",").join(entry.getValue());
                LOGGER.debug("{}: {}", entry.getKey(), value);
            }
        }
    }

    public void logResponseContentByte(int b) {
        getResponseContentByteFormatter().dump((byte) (b & 0xff));
    }

    public void logResponseContentChar(char c) {
        getResponseContentCharFormatter().dump(c);
    }

    public void logResponseContentChar(char[] cbuf, int off, int len) {
        CharContentLogFormatter formatter = getResponseContentCharFormatter();
        for (int i = 0; i < len; i++) {
            formatter.dump(cbuf[off + i]);
        }
    }

    public void logResponseContentChar(int c) {
        getResponseContentCharFormatter().dump((char) (c & 0xffff));
    }

    public void logResponseContentChar(String s, int off, int len) {
        CharContentLogFormatter formatter = getResponseContentCharFormatter();
        for (int i = 0; i < len; i++) {
            formatter.dump(s.charAt(off + i));
        }
    }

    public void logResponseContentClose() {
        if (responseContentByteFormatter != null) {
            responseContentByteFormatter.close();
        }
        if (responseContentCharFormatter != null) {
            responseContentCharFormatter.close();
        }
    }

    public void logResponseContentReset() {
        // TODO Auto-generated method stub
    }

    public void logResponseError(int sc, String msg) {
        // TODO Auto-generated method stub
    }

    public void logResponseFlush() {
        // TODO Auto-generated method stub
    }

    private void logResponseHeaders() {

        int status = response.getStatus();
        StringBuilder builder = new StringBuilder("Response (status: ").append(status).append(") Headers:\n");

        for (String name : response.getHeaderNames()) {
            builder.append("\t").append(name).append(": ").append(response.getHeader(name)).append("\n");
        }
        LOGGER.debug(builder.toString());
    }

    public void logResponseRedirect(String location) {
        // TODO Auto-generated method stub
    }

    public void logResponseReset() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        this.close();
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    private class ByteContentLogFormatter {
        private final static int MAX_BUF = 16;
        private final static int HEX_DISP_WIDTH = (MAX_BUF + (MAX_BUF * 2) + 1);
        private final String mode;
        private final ByteBuffer buf;
        private long length = 0;
        private boolean closed = false;
        private StringBuilder body = new StringBuilder();

        public ByteContentLogFormatter(String mode) {
            this.mode = mode;
            this.buf = ByteBuffer.allocate(MAX_BUF);
            LOGGER.debug("[" + mode + "] Byte Content");
        }

        public void close() {
            if (closed) {
                return;
            } else {
                processBuf(true);
                LOGGER.trace("Response Content: " + body.toString());
                LOGGER.trace("[{}] Closed :: Seen {} bytes", mode, length);
            }
            closed = true;
        }

        public void dump(byte b) {
            length += 1;
            processBuf(false);
            buf.put(b);
        }

        private synchronized void processBuf(boolean partialOk) {
            if (partialOk || (buf.remaining() <= 0)) {
                buf.flip();
                // time to dump the buffer contents to the log
//                StringBuilder hexed = new StringBuilder();
                StringBuilder asciid = new StringBuilder();
                int i = 0;
                while (buf.remaining() > 0) {
//                    if (i++ == (MAX_BUF / 2)) {
//                        hexed.append(" ");
//                    }
                    byte c = buf.get();
//                    hexed.append(String.format("%02X ", c));
                    // only show simple printable chars
                    if ((c >= 0x20) && (c <= 0x7E)) {
                        asciid.append((char) c);
                    } else {
//                        asciid.append(".");
                    }
                }
                buf.flip();
                body.append(asciid);
//                LOGGER.debug("[{}] Content:: {}", mode, asciid);
            }
        }
    }

    private class CharContentLogFormatter {
        private final static int MAX_BUF = 128;
        private final String mode;
        private CharBuffer buf;
        private long length = 0;
        private boolean closed = false;
        private StringBuilder body = new StringBuilder();

        public CharContentLogFormatter(String mode) {
            this.mode = mode;
            LOGGER.debug("[{}] Character Based", mode);
            buf = ByteBuffer.allocate(MAX_BUF).asCharBuffer();
        }

        public void close() {
            if (closed) {
                return;
            } else {
                processBuf(true);
                LOGGER.trace("Response Content: " + body.toString());
                LOGGER.debug("[{}] Closed :: Seen {} characters", mode, length);
            }
            closed = true;
        }

        public void dump(char c) {
            length += 1;
            processBuf(false);
            buf.append(c);
        }

        private synchronized void processBuf(boolean partialOk) {
            if (partialOk || (buf.remaining() <= 0)) {
                buf.flip();
                // time to dump the buffer contents to the log
                StringBuilder line = new StringBuilder();
                line.append('[').append(mode).append("] Content:: ");
                while (buf.remaining() > 0) {
                    char c = buf.get();
                    switch (c) {
                        case '\r':
                            line.append("\\r");
                            break;
                        case '\n':
                            line.append("\\n");
                            break;
                        case '\t':
                            line.append("\\t");
                            break;
                        default:
                            line.append(c);
                            break;
                    }
                }
                buf.flip();
                String msg = line.toString();
                body.append(msg);
                LOGGER.debug(msg);
            }
        }
    }
}