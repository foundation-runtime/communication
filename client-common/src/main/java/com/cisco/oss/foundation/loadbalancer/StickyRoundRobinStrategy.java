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

package com.cisco.oss.foundation.loadbalancer;

import com.cisco.oss.foundation.flowcontext.FlowContext;
import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is the sticky round robin strategy implementation. this strategy delegates the
 * requests to different servers each time. this way the load is balanced
 * between more than 1 server.
 * if multiple requstes are initialized from the same client using the same flowcontext it will be directed to the same server node.
 *
 * @author Yair Ogen
 */
public class StickyRoundRobinStrategy<S extends ClientRequest> extends RoundRobinStrategy<S> {

    private static final String FLOW_CONTEXT = "FLOW_CONTEXT";
    private static final long serialVersionUID = -4176790456291346233L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StickyRoundRobinStrategy.class);
    private String hashField = FLOW_CONTEXT;

    public StickyRoundRobinStrategy(boolean serviceDirectoryEnabled, String serviceName) {
        super(serviceDirectoryEnabled, serviceName);
    }

    @Override
    protected int nextNode(int serverProxisListSize, S request) {
        int hash = getHash(request);
        if (hash == -1) {
            return super.nextNode(serverProxisListSize, request);
        } else {
            int serverIndex = determineNextNode(hash, serverProxisListSize);
            if (serverIndex == -1) {
                return super.nextNode(serverProxisListSize, request);
            } else {
                return serverIndex;
            }
        }

    }

    private int getHash(S request) {

        String hashField = request.getLbKey();

        if (StringUtils.isNotBlank(hashField)) {
            if (FLOW_CONTEXT.equals(hashField)) {
                FlowContext flowContext = FlowContextFactory.getFlowContext();
                if (flowContext != null) {
                    return flowContext.hashCode();
                }
            } else {
                return hashField.hashCode();
            }
        }
        return -1;
    }

    private int determineNextNode(int hash, int serverProxisListSize) {

        int serverIndex = -1;

        //run from the end to the begining
        for (int i = serverProxisListSize; i > 0; i--) {
            if (hash % i == 0) {
                LOGGER.trace("node {} chosen for hash {}", i, hash);
                serverIndex = i - 1;
                break;
            }
        }

        final InternalServerProxy serverProxy = getServerProxies().get(serverIndex);
        if (!serverProxy.activate()) {
            serverIndex = -1;
        }

        LOGGER.debug("defaulting to regular round robin mode");
        return serverIndex;
    }

    public String getHashField() {
        return hashField;
    }

    public void setHashField(String hashField) {
        this.hashField = hashField;
    }


}
