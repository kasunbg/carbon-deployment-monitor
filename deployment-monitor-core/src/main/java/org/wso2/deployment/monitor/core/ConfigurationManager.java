/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.deployment.monitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class manages the Deployment Monitor Configurations
 */
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static volatile DeploymentMonitorConfiguration deploymentMonitorConfiguration = null;

    private ConfigurationManager() {
    }

    /**
     * Initializes the full configuration as {@link DeploymentMonitorConfiguration}
     */
    private static void initialize() {
        synchronized (ConfigurationManager.class) {
            if (deploymentMonitorConfiguration == null) {
                Path monitorConf = Paths.get(System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME), "conf",
                        MonitoringConstants.DEPLOYMENT_MONITOR_CONFIG_FILE);

                try (InputStream confInputStream = Files.newInputStream(monitorConf)) {
                    Representer representer = new Representer();
                    representer.getPropertyUtils().setSkipMissingProperties(true);

                    Yaml yaml = new Yaml(representer);
                    yaml.setBeanAccess(BeanAccess.FIELD);
                    deploymentMonitorConfiguration = yaml.loadAs(confInputStream, DeploymentMonitorConfiguration.class);

                    if (logger.isDebugEnabled()) {
                        logger.debug("configuration map = {}", deploymentMonitorConfiguration);
                    }

                } catch (IOException e) {
                    throw new DeploymentMonitorException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Returns the whole configuration as a {@link DeploymentMonitorConfiguration} bean
     *
     * @return DeploymentMonitorConfiguration bean
     */
    public static DeploymentMonitorConfiguration getConfiguration() {
        if (deploymentMonitorConfiguration == null) {
            initialize();
        }
        return deploymentMonitorConfiguration;
    }

}
