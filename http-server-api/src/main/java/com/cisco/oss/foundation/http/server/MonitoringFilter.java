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

package com.cisco.oss.foundation.http.server;

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.monitoring.CommunicationInfo;
import com.cisco.oss.foundation.monitoring.RMIMonitoringAgent;
import com.cisco.oss.foundation.monitoring.services.ServiceDetails;
import com.cisco.oss.foundation.string.utils.BoyerMoore;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

public class MonitoringFilter extends AbstractInfraHttpFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(MonitoringFilter.class);

    private ServiceDetails serviceDetails;
    private HttpThreadPool threadPool = null;
    private String description;
    private int port;
    private boolean isMonitoringWithWrappers = false;
    private boolean uniqueUriMonitoringEnabled = false;
    private List<BoyerMoore> boyers = new ArrayList<BoyerMoore>();

    public MonitoringFilter(String serviceName, HttpThreadPool threadPool) {
        super(serviceName);
        description = ConfigurationFactory.getConfiguration().getString(serviceName + ".http.serviceDescription", "DEFAULT_DESCRIPTION");
        port = ConfigurationFactory.getConfiguration().getInt(serviceName + ".http.port", 8080);
        serviceDetails = new ServiceDetails(description, serviceName, "HTTP", port);
        this.threadPool = threadPool;
        uniqueUriMonitoringEnabled = ConfigurationFactory.getConfiguration().getBoolean(serviceName + ".http.monitoringFilter.uniqueUriMonitoringEnabled", false);
        if (!uniqueUriMonitoringEnabled) {
            populateBoyersList();
        }
    }

    private void populateBoyersList() {
        Map<String, String> parseSimpleArrayAsMap = ConfigUtil.parseSimpleArrayAsMap(serviceName + ".http.monitoringFilter.baseUri");
        List<String> keys = new ArrayList<String>(parseSimpleArrayAsMap.keySet());
//		Collections.sort(keys);
        Collections.sort(keys, new Comparator<String>() {
            // Overriding the compare method to sort the age
            public int compare(String str1, String str2) {
                return Integer.parseInt(str1) - Integer.parseInt(str2);
            }
        });


        for (String key : keys) {
            String baseUri = parseSimpleArrayAsMap.get(key);
            boyers.add(new BoyerMoore(baseUri));
        }

    }

    @Override
    public void doFilterImpl(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        long startTime = System.currentTimeMillis();
        ServiceDetails reqServiceDetails = null;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String methodName = httpServletRequest.getMethod();
        if (uniqueUriMonitoringEnabled) {
            methodName += ":" + httpServletRequest.getRequestURI();
        }

        boolean reportToMonitoring = true;

        try {
            LOGGER.trace("Monitoring filter: Request processing started at: {}", startTime);

//			methodName = httpServletRequest.getMethod() + ":" + httpServletRequest.getRequestURI().toString();
            methodName = updateMethodName(httpServletRequest, methodName);
            LOGGER.trace("transaction method name is: {}", methodName);

            RMIMonitoringAgent.getInstance().register();


            reqServiceDetails = serviceDetails;


            reqServiceDetails = new ServiceDetails(description, serviceName, "HTTP", port);


            CommunicationInfo.getCommunicationInfo().transactionStarted(reqServiceDetails, methodName, threadPool.getThreads());

        } catch (Exception e) {
            LOGGER.error("can't report monitoring data as it has failed on:" + e);
            reportToMonitoring = false;
        }

        try {
            chain.doFilter(httpServletRequest, httpServletResponse);
        } catch (IOException e) {
            CommunicationInfo.getCommunicationInfo().transactionFinished(reqServiceDetails, methodName, true, e.toString());
            throw e;
        } catch (ServletException e) {
            CommunicationInfo.getCommunicationInfo().transactionFinished(reqServiceDetails, methodName, true, e.toString());
            throw e;
        } catch (RuntimeException e) {
            CommunicationInfo.getCommunicationInfo().transactionFinished(reqServiceDetails, methodName, true, e.toString());
            throw e;
        } finally {
            final long endTime = System.currentTimeMillis();
            final int processingTime = (int) (endTime - startTime);

            LOGGER.debug("Processing time: {} milliseconds", processingTime);
        }

        if (reportToMonitoring) {
            try {

                int status = httpServletResponse.getStatus();

                if (status >= 400) {
                    CommunicationInfo.getCommunicationInfo().transactionFinished(reqServiceDetails, methodName, true, httpServletResponse.getStatus() + "");

                } else {

                    CommunicationInfo.getCommunicationInfo().transactionFinished(reqServiceDetails, methodName, false, "");
                }
            } catch (Exception e) {
                LOGGER.error("can't report monitoring data as it has failed on:" + e);
            }
        }

    }

    protected String updateMethodName(final HttpServletRequest httpServletRequest, String defaultMethodName) {
        if (!uniqueUriMonitoringEnabled && !boyers.isEmpty()) {
            String uri = httpServletRequest.getRequestURI();
            for (BoyerMoore boyerMoore : boyers) {
                int match = boyerMoore.search(uri);
                if (match >= 0) {
                    return defaultMethodName + ":" + boyerMoore.getPattern();
                }
            }

        }
        return defaultMethodName;
    }

    protected Pair<Boolean, String> chcekResponseStatus(final HttpServletResponse httpServletResponse) {
        return Pair.of(false, "");
    }

    @Override
    protected String getKillSwitchFlag() {
        return "http.monitoringFilter.isEnabled";
    }

    @Override
    protected boolean isEnabledByDefault() {
        return true;
    }


}
