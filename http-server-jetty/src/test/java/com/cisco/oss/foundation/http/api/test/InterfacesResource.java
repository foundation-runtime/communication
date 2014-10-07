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

package com.cisco.oss.foundation.http.api.test;

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/ps/ifs")
public class InterfacesResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(InterfacesResource.class);
    private static final Logger AUDITOR = LoggerFactory.getLogger("audit." + InterfacesResource.class.getName());
    
    private Configuration config;

    @GET
	@Produces("text/plain")
//    @Transactional(rollbackFor=Exception.class)
    public String requestGetIfs(@Context UriInfo uriInfo,
			@HeaderParam("Session-ID") String sessionId,
			@HeaderParam("Source-ID") String sourceId,
			@HeaderParam("Source-Type") String sourceType) {

//        FlowContextFactory.createFlowContext();
        AUDITOR.info("starting InterfacesResource.requestGetIfs...");       

        LOGGER.debug("psSms InterfacesResource: Get interfaces");
        
//        Assert.isTrue(config != null);
        
        Assert.isTrue(FlowContextFactory.getFlowContext() != null);

        AUDITOR.info("exit InterfacesResource.requestGetIfs...");
        return "psSms adaptor v3.34.0; ICD version 2.10\n"
				+ "01. (see 7.1 in ICD)   SMS notifies PS\n"
                + "02. (see 7.3 in ICD)   Notify Pin Change\n"
                + "03. (see 10.5 in ICD)  Unpair Devices\n"
				+ "04. (see 10.8 in ICD)  Reset Master Pin\n"
				+ "05. (see 10.14 in ICD) Refresh CPE Household data\n";
	}


    @POST
    @Produces("text/plain")
//    @Transactional(rollbackFor=Exception.class)
    public String testPost(@Context UriInfo uriInfo,
                                @HeaderParam("Session-ID") String sessionId,
                                @HeaderParam("Source-ID") String sourceId,
                                @HeaderParam("Source-Type") String sourceType,
                                String body) {

//        FlowContextFactory.createFlowContext();
        AUDITOR.info("starting InterfacesResource.requestGetIfs...");

        LOGGER.debug("psSms InterfacesResource: Get interfaces");

//        Assert.isTrue(config != null);

        Assert.isTrue(FlowContextFactory.getFlowContext() != null);

        AUDITOR.info("exit InterfacesResource.requestGetIfs...");
        return "psSms adaptor v3.34.0; ICD version 2.10\n"
                + "01. (see 7.1 in ICD)   SMS notifies PS\n"
                + "02. (see 7.3 in ICD)   Notify Pin Change\n"
                + "03. (see 10.5 in ICD)  Unpair Devices\n"
                + "04. (see 10.8 in ICD)  Reset Master Pin\n"
                + "05. (see 10.14 in ICD) Refresh CPE Household data\n";
    }

    public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

}
