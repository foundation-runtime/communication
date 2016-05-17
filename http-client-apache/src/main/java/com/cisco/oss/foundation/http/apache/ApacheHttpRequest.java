package com.cisco.oss.foundation.http.apache;

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.cisco.oss.foundation.http.HttpRequest;
import org.apache.http.HttpEntity;

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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends HttpRequest.Builder{
        private ApacheHttpRequest request = new ApacheHttpRequest();

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
