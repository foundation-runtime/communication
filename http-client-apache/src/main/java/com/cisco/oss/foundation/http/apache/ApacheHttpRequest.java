package com.cisco.oss.foundation.http.apache;

import com.cisco.oss.foundation.flowcontext.FlowContext;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.http.HttpMethod;
import com.cisco.oss.foundation.http.HttpRequest;
import com.google.common.collect.Multimap;
import org.apache.http.HttpEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * Created by Yair Ogen (yaogen) on 17/05/2016.
 */
public class ApacheHttpRequest extends HttpRequest {

    private HttpEntity apacheHttpEntity;

    public HttpEntity getApacheHttpEntity() {
        return apacheHttpEntity;
    }

    public void setApacheHttpEntity(HttpEntity apacheHttpEntity) {
        this.apacheHttpEntity = apacheHttpEntity;
    }

    public ApacheHttpRequest() {
    }


    HttpRequest replaceUri(URI newURI) {
        Builder builder = new Builder();
        if(this.silentLogging){
            builder.silentLogging();
        }
        return builder
                .apacheEntity(this.getApacheHttpEntity())
                .uri(newURI)
                .entity(this.getEntity())
                .headers(this.headers)
                .lbKey(this.lbKey)
                .contentType(this.contentType)
                .queryParams(this.queryParams)
                .FlowContext(this.flowContext)
                .httpMethod(this.getHttpMethod())
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends HttpRequest.Builder{
        private ApacheHttpRequest request = new ApacheHttpRequest();

        public Builder uri(URI uri) {
            request.setUri(uri);
            return this;
        }

        public Builder uri(String uri) {
            try {
                request.setUri(new URI(uri));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder lbKey(String lbKey) {
            request.setLbKey(lbKey);
            return this;
        }

        public Builder contentType(String contentType) {
            request.setContentType(contentType);
            return this;
        }

        public Builder header(String name, String value) {
            request.headers.put(name, value);
            return this;
        }

        public Builder headers(Multimap<String, String> headers) {
            return headers(headers, true);
        }

        public Builder headers(Multimap<String, String> headers, boolean overrideExisting) {
            if (overrideExisting) {
                request.headers = headers;
            }else{
                request.headers.putAll(headers);
            }
            return this;
        }

        public Builder queryParams(Multimap<String, String> queryParams) {
            request.queryParams = queryParams;
            return this;
        }

        public Builder queryParams(String name, String value) {
            request.queryParams.put(name, value);
            return this;
        }

        public Builder entity(byte[] entity) {
            request.entity = entity;
            return this;
        }

        public Builder entity(String entity) {
            request.entity = entity.getBytes(Charset.forName("UTF-8"));
            return this;
        }

        public Builder entity(String entity, String charset) {
            request.entity = entity.getBytes(Charset.forName(charset));
            return this;
        }

        public Builder FlowContext(FlowContext flowContext) {
            request.flowContext = flowContext;
            return this;
        }

        public Builder httpMethod(HttpMethod HttpMethod) {
            request.httpMethod = HttpMethod;
            return this;
        }

        public Builder https() {
            request.httpsEnabled = true;
            return this;
        }

        public Builder retryOnServerBusy() {
            request.retryOnServerBusy = true;
            return this;
        }

        public Builder queryParamsParseAsMultiValue() {
            request.queryParamsParseAsMultiValue = true;
            return this;
        }

        public Builder silentLogging() {
            request.silentLogging = true;
            return this;
        }

        public Builder apacheEntity(HttpEntity httpEntity) {
            request.setApacheHttpEntity(httpEntity);
            return this;
        }

        public HttpRequest build() {
            request.flowContext = FlowContextFactory.getFlowContext();
            request.headers.removeAll("FLOW_CONTEXT");
            request.headers.put("FLOW_CONTEXT", FlowContextFactory.serializeNativeFlowContext());
            return request;
        }
    }


}
