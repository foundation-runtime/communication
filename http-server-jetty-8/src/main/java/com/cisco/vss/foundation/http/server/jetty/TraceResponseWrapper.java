/**
 * 
 */
package com.cisco.vss.foundation.http.server.jetty;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import com.cisco.vss.foundation.http.server.TraceWrapper;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * a response wrapper that enables reading output without exhausting the output stream
 * @author dmirkin
 * @author Yair Ogen
 */
public class TraceResponseWrapper extends HttpServletResponseWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceResponseWrapper.class);
//	private final static String[] CONTENT_TYPES = ConfigurationFactory.getConfiguration().getStringArray("auth.tracefilter.textContentTypes");
	private final String bodySuffix;
	private ByteArrayOutputStream localStream = null;
	private final int bodyLimit;
	private List<String> contentTypes = null;
	private String serviceName;
	
	public TraceResponseWrapper(final HttpServletResponse response, final int bodyLimit, final String serviceName) {
		super(response);
		this.bodyLimit = bodyLimit;
		bodySuffix = " ... (Body is larger than " + this.bodyLimit + " bytes)";
		contentTypes = TraceWrapper.getContentTypes(serviceName);
		this.serviceName = serviceName;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponseWrapper#getWriter()
	 */
	@Override
	public PrintWriter getWriter() throws IOException {
		if (localStream == null) {
			localStream = new ByteArrayOutputStream();			
			return new PrintWriter(new PrintStream(localStream, false, super.getCharacterEncoding()),false);
		}
		else {
			throw new IllegalStateException();
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponseWrapper#getOutputStream()
	 */
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (localStream == null) {
			localStream = new ByteArrayOutputStream();
			return new ServletOutputStream() {
				/* (non-Javadoc)
				 * @see java.io.OutputStream#write(byte[])
				 */
				@Override
				public void write(final byte[] b) throws IOException {
					localStream.write(b);
				}

				@Override
				public void write(final int b) throws IOException {
					localStream.write(b);					
				}

                @Override
                public void flush() throws IOException {
                    TraceResponseWrapper.this.getResponse().getOutputStream().flush();
                }

                @Override
                public void close() throws IOException {
                    if (localStream != null) {
                        TraceResponseWrapper.this.getResponse().getOutputStream().write(localStream.toByteArray());
                    }
                    TraceResponseWrapper.this.writeBody();
                    TraceResponseWrapper.this.getResponse().getOutputStream().close();
                }
			};
		} else {
			throw new IOException("Response has already been written.");
		}
	}
	
	public void writeBody() throws IOException {
		if (localStream != null && super.getOutputStream()!= null ) {
            try {
                super.getOutputStream().write(localStream.toByteArray());
            } catch (IOException e) {
                LOGGER.trace("can't write to outputstream:" + e);
            }
        }
	}

	@Override
	public String toString() {
		return super.getResponse().toString();
	}
	
	public String toStringWithBody() {
		final String resp = super.getResponse().toString();
		String ndsBodyStatusHeader = super.getHeader("Nds-Body-Status");
		boolean forceBinaryPrint = ConfigurationFactory.getConfiguration().getBoolean(serviceName +".http.traceFilter.printInHex", false);
		
		String contentType = getContentType();
		if(contentType == null){						
			ServletResponse response = getResponse();

			if (response instanceof Response){
				Response httpResponse = (Response)response;
				contentType = httpResponse.getHeader(HttpHeaders.CONTENT_TYPE);
			}
		}
		
		return TraceWrapper.toStringWithBody(resp, ndsBodyStatusHeader, localStream != null ? localStream.toByteArray() : new byte[]{}, bodyLimit, contentType, contentTypes, getCharacterEncoding(), bodySuffix, forceBinaryPrint);
	}
}
