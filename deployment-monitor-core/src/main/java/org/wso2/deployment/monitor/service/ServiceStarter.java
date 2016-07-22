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
        logger.info("Starting MSF4J Server in port : " + serviceConfig.getPort());
        MicroservicesRunner runner = new MicroservicesRunner(serviceConfig.getPort());

        if (serviceConfig.isEnabled()) {
            for (String serviceClass : serviceConfig.getServiceClasses()) {
                logger.info("Starting Service : " + serviceClass);
                try {
                    Class<?> taskClass = Class.forName(serviceClass);
                    Constructor[] constructors = taskClass.getDeclaredConstructors();
                    Constructor constructor = null;
                    for (Constructor tempConstructor : constructors) {
                        constructor = tempConstructor;
                        if (constructor.getGenericParameterTypes().length == 0) {
                            break;
                        }
                    }
                    if (constructor != null) {
                        runner.deploy(constructor.newInstance());
                    } else {
                        logger.error("Error occurred while starting the service : " + serviceClass
                                + " Constructor was not found.");
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    logger.error("Error occurred while starting the service", e);
                }
            }
            runner.start();
        } else {
            logger.warn("Deployment Monitor service is disabled");
        }
    }

}
