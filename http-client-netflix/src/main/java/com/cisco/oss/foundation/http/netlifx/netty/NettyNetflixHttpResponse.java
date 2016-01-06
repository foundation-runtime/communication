package com.cisco.oss.foundation.http.netlifx.netty;

import com.cisco.oss.foundation.http.ClientException;
import com.cisco.oss.foundation.http.HttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.HttpResponseHeaders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Yair Ogen (yaogen) on 13/12/2015.
 */
public class NettyNetflixHttpResponse implements HttpResponse {
    private URI requestUri = null;
    private byte[] responseBody;
    private boolean isClosed = false;
    private HttpClientResponse<ByteBuf> httpResponse;


    public NettyNetflixHttpResponse(HttpClientResponse<ByteBuf> httpResponse) {
        super();
        this.httpResponse = httpResponse;
    }

    @Override
    public int getStatus() {
        return httpResponse.getStatus().code();
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
                Map<String, Collection<String>> headers = new HashMap<>();
        HttpResponseHeaders httpHeaders = httpResponse.getHeaders();
        List<Map.Entry<String, String>> allHeaders = httpHeaders.entries();
        for (Map.Entry<String, String> allHeader : allHeaders) {
            headers.put(allHeader.getKey(),httpHeaders.getAll(allHeader.getKey()));
        }
//        return httpResponse.getHttpHeaders();
        return headers;
    }

    @Override
    public boolean hasResponseBody() {
        return httpResponse.getContent() != null || responseBody != null;
    }

    @Override
    public byte[] getResponse() {
        if(responseBody != null){
            return responseBody;
        }else{

            ByteBuf buf = httpResponse.getContent().doOnNext(ByteBuf::retain).toBlocking().first();
            int length = buf.readableBytes();

            if (buf.hasArray()) {
                responseBody = buf.array();
            } else {
                responseBody = new byte[length];
                buf.readBytes(responseBody);
            }
            buf.release();
        }

        return responseBody;
    }

    @Override
    public String getResponseAsString() {
        return new String(getResponse());
    }

    @Override
    public String getResponseAsString(String charset) {
//        ByteBuf byteBuf = httpResponse.getContent().toBlocking().first();
//        return byteBuf.toString(Charset.forName(charset));
        try {
            return new String(getResponse(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new ClientException("can't create response: " + e, e);
        }
    }

    @Override
    public InputStream getInputStream() {
        byte[] response = getResponse();
        return new ByteArrayInputStream(response);
    }

    @Override
    public URI getRequestedURI() {
        return requestUri;
    }

    @Override
    public boolean isSuccess() {
        return httpResponse.getStatus().code() < 400 ;
    }

    @Override
    public void close() {
    }

    }
