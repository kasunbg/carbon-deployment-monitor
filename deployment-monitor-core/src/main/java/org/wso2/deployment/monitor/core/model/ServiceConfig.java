package org.wso2.deployment.monitor.core.model;

/**
 * This is for Service Config
 */
public class ServiceConfig {

    private boolean enabled = false;
    private int port = 8080;
    private String serviceClass = "org.wso2.deployment.monitor.service.DeploymentMonitorService";

    public boolean isEnabled() {
        return enabled;
    }

    public int getPort() {
        return port;
    }

    public String getServiceClass() {
        return serviceClass;
    }
}
