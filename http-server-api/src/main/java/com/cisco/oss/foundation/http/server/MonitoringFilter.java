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

package com.cisco.oss.foundation.http.server;

import com.cisco.oss.foundation.configuration.ConfigUtil;
import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.monitoring.CommunicationInfo;
import com.cisco.oss.foundation.monitoring.MonitoringAgentFactory;
import com.cisco.oss.foundation.monitoring.services.ServiceDetails;
import com.cisco.oss.foundation.string.utils.BoyerMoore;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Component
@Order(40)
public class MonitoringFilter extends AbstractInfraHttpFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(MonitoringFilter.class);

    private ServiceDetails serviceDetails;
    private HttpThreadPool threadPool = null;
    private String description;
    private int port;
    private boolean isMonitoringWithWrappers = false;
    private boolean uniqueUriMonitoringEnabled = false;
    private List<BoyerMoore> boyers = new ArrayList<BoyerMoore>();
    private boolean isRegistered = false;

    public MonitoringFilter() {
        super();

    }

    @PostConstruct
    public void initMonitoringFilter() {
        description = ConfigurationFactory.getConfiguration().getString(serviceName + ".http.serviceDescription", "DEFAULT_DESCRIPTION");
        port = ConfigurationFactory.getConfiguration().getInt(serviceName + ".http.port", 8080);
        serviceDetails = new ServiceDetails(description, serviceName, "HTTP", port);
        uniqueUriMonitoringEnabled = ConfigurationFactory.getConfiguration().getBoolean(serviceName + ".http.monitoringFilter.uniqueUriMonitoringEnabled", false);
        if (!uniqueUriMonitoringEnabled) {
            populateBoyersList();
        }
        regiterMonitoring();
    }

    private void regiterMonitoring() {
        if (!isRegistered && getConfigValue(enabledKey, isEnabledByDefault())){
            MonitoringAgentFactory.getInstance().register();
            isRegistered = true;
        }

    }

    public MonitoringFilter(String serviceName, HttpThreadPool threadPool) {
        super(serviceName);
        this.threadPool = threadPool;
        description = ConfigurationFactory.getConfiguration().getString(serviceName + ".http.serviceDescription", "DEFAULT_DESCRIPTION");
        port = ConfigurationFactory.getConfiguration().getInt(serviceName + ".http.port", 8080);
        serviceDetails = new ServiceDetails(description, serviceName, "HTTP", port);
        uniqueUriMonitoringEnabled = ConfigurationFactory.getConfiguration().getBoolean(serviceName + ".http.monitoringFilter.uniqueUriMonitoringEnabled", false);
        if (!uniqueUriMonitoringEnabled) {
            populateBoyersList();
        }
        regiterMonitoring();
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

        regiterMonitoring();

        final long startTime = System.currentTimeMillis();
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String tempMethodName = httpServletRequest.getMethod();
        if (uniqueUriMonitoringEnabled) {
            tempMethodName += ":" + httpServletRequest.getRequestURI();
        }

        boolean reportToMonitoring = true;

        try {
            LOGGER.trace("Monitoring filter: Request processing started at: {}", startTime);

//			methodName = httpServletRequest.getMethod() + ":" + httpServletRequest.getRequestURI().toString();
            tempMethodName = updateMethodName(httpServletRequest, tempMethodName);

            LOGGER.trace("transaction method name is: {}", tempMethodName);


            CommunicationInfo.getCommunicationInfo().transactionStarted(serviceDetails, tempMethodName, threadPool != null ? threadPool.getThreads() : -1);

        } catch (Exception e) {
            LOGGER.error("can't report monitoring data as it has failed on:" + e);
            reportToMonitoring = false;
        }
        final String methodName = tempMethodName;

        try {
            chain.doFilter(httpServletRequest, httpServletResponse);

            if (request.isAsyncStarted()) {

                AsyncContext async = request.getAsyncContext();
                async.addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) throws IOException {
                        final long endTime = System.currentTimeMillis();
                        final int processingTime = (int) (endTime - startTime);
                        LOGGER.debug("Processing time: {} milliseconds", processingTime);
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) throws IOException {

                    }

                    @Override
                    public void onError(AsyncEvent event) throws IOException {
                        Throwable throwable = event.getThrowable();
                        if (throwable != null) {
                            CommunicationInfo.getCommunicationInfo().transactionFinished(serviceDetails, methodName, true, throwable.toString());
                        }
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) throws IOException {

                    }
                });


            } else {
                final long endTime = System.currentTimeMillis();
                final int processingTime = (int) (endTime - startTime);
                LOGGER.debug("Processing time: {} milliseconds", processingTime);
            }


        } catch (Exception e) {
            CommunicationInfo.getCommunicationInfo().transactionFinished(serviceDetails, methodName, true, e.toString());
            throw e;
        }

        if (reportToMonitoring) {
            try {

                int status = httpServletResponse.getStatus();

                if (status >= 400) {
                    CommunicationInfo.getCommunicationInfo().transactionFinished(serviceDetails, methodName, true, httpServletResponse.getStatus() + "");

                } else {

                    CommunicationInfo.getCommunicationInfo().transactionFinished(serviceDetails, methodName, false, "");
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
        return false;
    }


}
