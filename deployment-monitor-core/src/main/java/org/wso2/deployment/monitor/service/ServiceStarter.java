package org.wso2.deployment.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.model.ServiceConfig;
import org.wso2.msf4j.MicroservicesRunner;

/**
 * This will start a MSF4J based service to control the Deployment Monitor
 */
public class ServiceStarter {

    private static final Logger logger = LoggerFactory.getLogger(ServiceStarter.class);

    public static void startService(ServiceConfig serviceConfig) {
        if (serviceConfig.isEnabled()) {
            logger.info("Starting Deployment Monitor Service in port : " + serviceConfig.getPort());
            new MicroservicesRunner(serviceConfig.getPort()).deploy(new DeploymentMonitorService()).start();
        } else {
            logger.warn("Deployment Monitor service is disabled");
        }
    }

}
