/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.deployment.monitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.ServerGroup;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.TaskConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The entry point.
 */
public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        //argument parsing
        //config parsing
        //invoke quartz

        if (System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME) == null) {
            System.setProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME, ".");
        }
        logger.info("Deployment Monitor Home {}", System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME));

        Launcher launcher = new Launcher();
        DeploymentMonitorConfiguration config = launcher.getMonitorConfig();

        List<ServerGroup> serverGroups = config.getServerGroups();
//        GlobalConfig global = config.getGlobal();
        List<TaskConfig> tasks = config.getTasks();

        //call schedule manager
        DummyScheduleManager scheduleManager = new DummyScheduleManager(); //todo
        for (TaskConfig task : tasks) {
            scheduleManager.schedule(task, serverGroups);

        }
    }

    private DeploymentMonitorConfiguration getMonitorConfig() {
        Path monitorConf = Paths.get(System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME), "conf",
                LauncherConstants.DEPLOYMENT_MONITOR_CONFIG_FILE);

        try (InputStream confInputStream = Files.newInputStream(monitorConf)) {
            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);

            Yaml yaml = new Yaml(representer);
            yaml.setBeanAccess(BeanAccess.FIELD);
            DeploymentMonitorConfiguration config = yaml.loadAs(confInputStream, DeploymentMonitorConfiguration.class);

            if (logger.isDebugEnabled()) {
                logger.debug("configuration map = {}", config);
            }

            return config;
        } catch (IOException e) {
            throw new DeploymentMonitorException(e.getMessage(), e);
        }

    }
}
