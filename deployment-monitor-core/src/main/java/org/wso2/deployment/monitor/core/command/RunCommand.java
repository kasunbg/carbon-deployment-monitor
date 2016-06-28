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
package org.wso2.deployment.monitor.core.command;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.Command;
import org.wso2.deployment.monitor.core.DeploymentMonitorException;
import org.wso2.deployment.monitor.core.TaskUtils;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This runs the user provided tasks once.
 * This ignores the trigger properties specified in deployment-monitor.yaml.
 *
 */
public class RunCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);

    @Option(name = "-all", usage = "Schedule all the tasks", aliases = {"--all"})
    private boolean scheduleAll = false;

    /**
     * The list of task names to run/schedule.
     */
    @Argument(index = 0, metaVar = "<task-name ...>", usage = "list of task names. Specify <todo> for all.", //todo
              required = false, multiValued = true)
    private List<String> taskNamesToRun = new ArrayList<>();

    @Override
    public void execute(DeploymentMonitorConfiguration deploymentMonitorConfiguration) {
        List<ServerGroup> allServerGroups = deploymentMonitorConfiguration.getServerGroups();
        List<TaskConfig> allTasks = deploymentMonitorConfiguration.getTasks();

        List<TaskConfig> tasksToRun;
        if (scheduleAll) {
            tasksToRun = allTasks;
        } else {
            if (taskNamesToRun.size() == 0) {
                throw new DeploymentMonitorException("Please specify either -all or a list of task names to run.");
            }
            tasksToRun = TaskUtils.filterTasksByName(allTasks, taskNamesToRun);
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
            if (!taskConfig.isEnable()) {
                continue;
            }

            for (String serverGroupName : taskConfig.getServers()) {
                ServerGroup serverGroup = allServerGroupMap.get(serverGroupName);
                if (serverGroup == null) {
                    logger.warn("Server Group '{}' is not declared in the serverGroups configuration.",
                            serverGroupName);
                    continue;
                }

                logger.info("\n--- Running task '{}' @ '{}' ---", taskConfig.getName(), serverGroupName);
                TaskUtils.callTask(taskConfig.getName(), taskConfig.getClassName(), taskConfig.getOnResult(),
                        serverGroup, taskConfig.getTaskParams());
            }
        }
    }
}
