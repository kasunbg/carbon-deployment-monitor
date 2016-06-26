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
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This runs the user provided tasks once.
 * This ignores the trigger properties specified in deployment-monitor.yaml.
 *
 */
public class RunCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);

    @Override
    public void execute(DeploymentMonitorConfiguration deploymentMonitorConfiguration) {
        List<ServerGroup> allServerGroups = deploymentMonitorConfiguration.getServerGroups();
        List<TaskConfig> allTasks = deploymentMonitorConfiguration.getTasks();

        List<String> taskNamesToRun = getTaskNames();

        List<TaskConfig> tasksToRun = new ArrayList<>(taskNamesToRun.size());
        if (taskNamesToRun.contains("*")) {
            tasksToRun = allTasks;
        } else {
            for (TaskConfig taskConfig : allTasks) {
                Iterator<String> it = taskNamesToRun.iterator();
                while (it.hasNext()) {
                    if (it.next().equals(taskConfig.getName())) {
                        tasksToRun.add(taskConfig);
                        it.remove();
                        break;
                    }
                }
            }
        }

        if (!taskNamesToRun.isEmpty()) {
            logger.warn("These task names were not found in {} - {}",
                    MonitoringConstants.DEPLOYMENT_MONITOR_CONFIG_FILE, taskNamesToRun);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Running {} tasks once...", tasksToRun.size());
        }

        //A Task is scheduled per each server group defined for a Task
        Map<String, ServerGroup> allServerGroupMap = new HashMap<>();
        for (ServerGroup serverGroup : allServerGroups) {
            allServerGroupMap.put(serverGroup.getName(), serverGroup);
        }

        for (TaskConfig taskConfig : tasksToRun) {
            for (String serverGroupName : taskConfig.getServers()) {
                ServerGroup serverGroup = allServerGroupMap.get(serverGroupName);
                if (serverGroup == null) {
                    logger.warn("Server Group '{}' is not declared in the serverGroups configuration.",
                            serverGroupName);
                    continue;
                }

                logger.info("--- Running task '{}' @ '{}' ---", taskConfig.getName(), serverGroupName);
                TaskUtils.callTask(taskConfig.getClassName(), taskConfig.getOnResult(), serverGroup,
                        taskConfig.getTaskParams());
            }
        }
    }
}
