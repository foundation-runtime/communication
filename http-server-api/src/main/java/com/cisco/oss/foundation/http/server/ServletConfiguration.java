package com.cisco.oss.foundation.http.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Yair Ogen (yaogen) on 08/06/2016.
 */

@Configuration
public class ServletConfiguration {

    @ConditionalOnExpression("#{'${${spring.application.name}.http.pingFilter.isEnabled}' != null && '${${spring.application.name}.http.pingFilter.isEnabled}'.toLowerCase().equals('false')}")
    @Bean (name="probe")
    public PingServlet probe(){
        return new PingServlet();
    }

    @ConditionalOnExpression("#{'${${spring.application.name}.http.pingFilter.isEnabled}' != null && '${${spring.application.name}.http.pingFilter.isEnabled}'.toLowerCase().equals('false')}")
    @Bean
    public ServletRegistrationBean dispatcherRegistration(PingServlet pingServlet) {
        ServletRegistrationBean registration = new ServletRegistrationBean(
                pingServlet);
        registration.addUrlMappings("/probe/*");
        return registration;
    }
}
