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

import org.apache.log4j.PropertyConfigurator;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.ServerGroup;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.GlobalConfig;
import org.wso2.deployment.monitor.core.model.TaskConfig;
import org.wso2.deployment.monitor.scheduler.ScheduleManager;
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

    public static void main(String[] args) {
        
        if (System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME) == null) {
            System.setProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME, ".");
        }
        PropertyConfigurator.configure(MonitoringConstants.LOG4J_PROPERTIES_CONFIG_PATH);

        logger.info("Deployment Monitor Home {}", System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME));

        Launcher launcher = new Launcher();
        DeploymentMonitorConfiguration config = launcher.getMonitorConfig();

        List<ServerGroup> serverGroups = config.getServerGroups();
        GlobalConfig global = config.getGlobal();
        List<TaskConfig> tasks = config.getTasks();

        launcher.mergeGlobalConfigToTaskConfig(tasks, global);
        launcher.mergeGlobalConfigToServerGroups(serverGroups, global);

        //call schedule manager
        try {
            ScheduleManager scheduleManager = new ScheduleManager();
            for (TaskConfig task : tasks) {
                if (task.isEnable()) {
                    scheduleManager.scheduleTask(task, serverGroups);
                }
            }
            scheduleManager.startScheduler();
        } catch (SchedulerException e) {
            logger.error("Scheduler error. ", e);
        }
    }

    /**
     * Merge following global parameters into TaskConfig
     *  - onResult
     *  - tenantConfig
     *
     */
    private void mergeGlobalConfigToTaskConfig(List<TaskConfig> tasks, GlobalConfig global) {
        for (TaskConfig taskConfig : tasks) {
            if (taskConfig.getOnResult().isEmpty()) {
                taskConfig.setOnResult(global.getOnResult());
            }
            taskConfig.getTaskParams().put(MonitoringConstants.DEFAULT_TENANT_KEY, global.getTenant());
        }
    }

    /**
     * Merge following global parameters into ServerGroups
     *  - trustStore
     *  - trustStorePassword
     *
     */
    private void mergeGlobalConfigToServerGroups(List<ServerGroup> serverGroups, GlobalConfig global) {
        for (ServerGroup serverGroup : serverGroups) {
            if (serverGroup.getTrustStore().isEmpty()) {
                serverGroup.setTrustStore(global.getTrustStore());
            }
            if (serverGroup.getTrustStorePassword().isEmpty()) {
                serverGroup.setTrustStorePassword(global.getTrustStorePassword());
            }
        }
    }

    /**
     * parse deployment-monitor.yaml
     *
     * @return DeploymentMonitorConfiguration bean
     */
    private DeploymentMonitorConfiguration getMonitorConfig() {
        Path monitorConf = Paths.get(System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME), "conf",
                MonitoringConstants.DEPLOYMENT_MONITOR_CONFIG_FILE);

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
