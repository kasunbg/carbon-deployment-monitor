package org.wso2.deployment.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.model.ServiceConfig;
import org.wso2.msf4j.MicroservicesRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This will start a MSF4J based service to control the Deployment Monitor
 */
public class ServiceStarter {

    private static final Logger logger = LoggerFactory.getLogger(ServiceStarter.class);

    public static void startService(ServiceConfig serviceConfig) {

        if (serviceConfig.isEnabled()) {
            logger.info("Starting Deployment Monitor Service in port : " + serviceConfig.getPort());
            try {
                Class<?> taskClass = Class.forName(serviceConfig.getServiceClass());
                Constructor[] constructors = taskClass.getDeclaredConstructors();
                Constructor constructor = null;
                for (Constructor tempConstructor : constructors) {
                    constructor = tempConstructor;
                    if (constructor.getGenericParameterTypes().length == 0) {
                        break;
                    }
                }
                if (constructor != null) {
                    new MicroservicesRunner(serviceConfig.getPort()).deploy(constructor.newInstance()).start();
                } else {
                    logger.error("Error occurred while starting the service : " + serviceConfig.getServiceClass()
                            + " Constructor was not found.");
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                logger.error("Error occurred while starting the service", e);
            }

        } else {
            logger.warn("Deployment Monitor service is disabled");
        }
    }

}
