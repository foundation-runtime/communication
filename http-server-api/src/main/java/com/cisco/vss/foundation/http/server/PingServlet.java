package com.cisco.vss.foundation.http.server;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: Yair Ogen
 * Date: 10/16/13
 * Time: 3:26 PM
 */
public class PingServlet extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(PingServlet.class);

    private boolean enableLogging = false;

    public PingServlet(String serviceName) {
        enableLogging = ConfigurationFactory.getConfiguration().getBoolean("service." + serviceName + "http.pingFilter.enableLogging", false);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ping(req, resp);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ping(req, resp);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void ping(HttpServletRequest req, HttpServletResponse resp) {
        if (enableLogging) {
            LOGGER.debug("HTTP Ping received from " + HttpServerUtil.getOriginalClient(req));
        }
        ((HttpServletResponse) resp).setDateHeader("Date", System.currentTimeMillis());
        ((HttpServletResponse) resp).setStatus(HttpServletResponse.SC_OK);
    }
}
