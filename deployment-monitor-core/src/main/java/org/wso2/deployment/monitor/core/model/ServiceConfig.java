package org.wso2.deployment.monitor.core.model;

/**
 * This is for Service Config
 */
public class ServiceConfig {

    private boolean enabled = false;
    private int port = 8080;

    public boolean isEnabled() {
        return enabled;
    }

    public int getPort() {
        return port;
    }
}
