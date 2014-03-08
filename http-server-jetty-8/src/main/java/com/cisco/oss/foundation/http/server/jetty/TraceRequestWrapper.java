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

/**
 *
 */
package com.cisco.oss.foundation.http.server.jetty;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.TraceWrapper;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.List;

/**
 * A wrapper for a request can be read without consuming it's inputstream
 * @author Dima Mirkin
 * @author Yair Ogen
 * 
 */
public class TraceRequestWrapper extends HttpServletRequestWrapper {
	// private final static String[] CONTENT_TYPES =
	// ConfigurationFactory.getConfiguration().getStringArray("auth.tracefilter.textContentTypes");
	private final int bodyLimit;
	private final String bodySuffix;
	public static final int DEFAULT_BUF_SIZE = 1024 * 4;
	// private final static Logger LOGGER =
	// LoggerFactroy.getLogger(TraceRequestWrapper.class);
	private final byte[] body;
	private List<String> contentTypes = null;
	private String serviceName;

	public TraceRequestWrapper(final HttpServletRequest request, final int logLimit, final String serviceName) throws IOException {
		super(request);
		this.body = getRequestBody(request);
		this.bodyLimit = logLimit;
		bodySuffix = " ... (Body is larger than " + this.bodyLimit + " bytes)";
		contentTypes = TraceWrapper.getContentTypes(serviceName);
		this.serviceName = serviceName;
	}

	public byte[] getOriginalBody() {
		return body;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequestWrapper#getInputStream()
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStreamImpl(new ByteArrayInputStream(body));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequestWrapper#getReader()
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		String enc = getCharacterEncoding();
		if (enc == null) {
			enc = "UTF-8";
		}
		return new BufferedReader(new InputStreamReader(getInputStream(), enc));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();

		HttpServletRequest httpServletRequest = (HttpServletRequest) super.getRequest();
		builder.append(httpServletRequest.getMethod() + " ").append(httpServletRequest.getProtocol() + " ").append(httpServletRequest.getContextPath() + " ").append(httpServletRequest.getRequestURI());

		if (httpServletRequest.getQueryString() != null && !httpServletRequest.getQueryString().isEmpty()) {
			builder.append("?").append(httpServletRequest.getQueryString());
		}

		if (httpServletRequest.getAttribute("host") != null) {
			builder.append("host: ").append((String) httpServletRequest.getAttribute("host"));
		}

		builder.append("\n");

		final Enumeration<String> names = httpServletRequest.getHeaderNames();
		while (names.hasMoreElements()) {
			final String name = (String) names.nextElement();
			final String header = httpServletRequest.getHeader(name);
			builder.append(name).append(": ").append(header).append("\n");
		}// while
		return builder.toString();
	}

	public String toStringWithBody() {
		final String req = this.toString();
		String ndsBodyStatusHeader = super.getHeader("Nds-Body-Status");
		boolean forceBinaryPrint = ConfigurationFactory.getConfiguration().getBoolean(serviceName + ".http.traceFilter.printInHex", false);
		String contentType = getContentType();
		if (contentType == null) {
			if (getRequest() instanceof Request) {
				Request request = (Request) getRequest();
				contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
			}
		}
		
		return TraceWrapper.toStringWithBody(req, ndsBodyStatusHeader, body, bodyLimit, contentType, contentTypes, getCharacterEncoding(), bodySuffix, forceBinaryPrint);		
	}

	private class ServletInputStreamImpl extends ServletInputStream {
		private final ByteArrayInputStream iStream;

		public ServletInputStreamImpl(final ByteArrayInputStream istr) {
			super();
			this.iStream = istr;
		}

		public int read() throws IOException {
			return iStream.read();
		}

		public boolean markSupported() {
			return false;
		}

		public void reset() throws IOException {
			throw new IOException("mark/reset not supported");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		@Override
		public int read(final byte[] buf, final int off, final int len) throws IOException {
			return iStream.read(buf, off, len);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.InputStream#available()
		 */
		@Override
		public int available() throws IOException {
			return iStream.available();
		}
	}

	public static byte[] getRequestBody(final HttpServletRequest request) throws IOException {
		byte[] retValue = new byte[0];
		final int contentLength = request.getContentLength();
		if (contentLength < 0) {
			if (request.getHeader("Transfer-Encoding") != null && request.getHeader("Transfer-Encoding").compareToIgnoreCase("chunked") == 0) {
				final ServletInputStream iStream = request.getInputStream();
				int actualRead = -1;
				byte[] tmp = new byte[DEFAULT_BUF_SIZE];
				int currentLen = 0;
				ByteBuffer bodyAccumulator = null;
				do {
					actualRead = iStream.read(tmp);
					if (actualRead > 0) {
						bodyAccumulator = addBits(bodyAccumulator, currentLen, tmp, actualRead);
						currentLen += actualRead;
					}
				} while (actualRead != -1);

				if (currentLen == 0) {
					retValue = new byte[0];
				} else {
					retValue = bodyAccumulator.array();
				}
			}
		} else {
			final ByteBuffer bodyAccumulator = ByteBuffer.allocate(contentLength);
			final ServletInputStream iStream = request.getInputStream();
			final byte[] tmp = new byte[DEFAULT_BUF_SIZE];
			int actualRead = 0;
			do {
				actualRead = iStream.read(tmp);
				if (actualRead != -1) {
					bodyAccumulator.put(tmp, 0, actualRead);
				}
			} while (actualRead != -1);
			if (bodyAccumulator.remaining() > 0) {
				throw new IOException("Failed to read request body. Available: " + bodyAccumulator.position() + ", expected: " + contentLength);
			}
			retValue = bodyAccumulator.array();
		}
		return retValue;
	}

	private static ByteBuffer addBits(ByteBuffer bodyAccumulator, int currentLen, byte[] tmp, int actualRead) {
		if (actualRead <= 0) {
			return null;
		}
		if (bodyAccumulator == null) {
			bodyAccumulator = ByteBuffer.allocate(actualRead);
			bodyAccumulator.put(tmp, 0, actualRead);
			return bodyAccumulator;
		} else {
			ByteBuffer bodyAccumulatorTmp = ByteBuffer.allocate(currentLen + actualRead);
			bodyAccumulator.position(0);
			bodyAccumulatorTmp.put(bodyAccumulator);

			bodyAccumulatorTmp.position(currentLen);
			bodyAccumulatorTmp.put(tmp, 0, actualRead);
			bodyAccumulator = bodyAccumulatorTmp.duplicate();
			return bodyAccumulator;
		}
	}

}
