package org.wso2.deployment.monitor.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This is for Service Config
 */
public class ServiceConfig {

    private boolean enabled = false;
    private int port = 8080;
    private List<String> serviceClasses = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public int getPort() {
        return port;
    }

    public List<String> getServiceClasses() {
        return serviceClasses;
    }
}
