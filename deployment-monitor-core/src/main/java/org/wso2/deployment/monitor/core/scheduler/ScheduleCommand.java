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
package org.wso2.deployment.monitor.core.scheduler;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.Command;
import org.wso2.deployment.monitor.core.DeploymentMonitorException;
import org.wso2.deployment.monitor.core.TaskUtils;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedule the tasks to run periodically as per the trigger config
 * in the deployment-monitor.yaml.
 *
 * todo take the enabled=false into account if * is specified.
 *
 */
public class ScheduleCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleCommand.class);

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
        List<TaskConfig> allTasks = deploymentMonitorConfiguration.getTasks();

        List<TaskConfig> tasksToRun;
        if (scheduleAll) {
            tasksToRun = allTasks;
        } else {
            if (taskNamesToRun.size() == 0) {
                throw new DeploymentMonitorException("Please specify either -all or a list of task names to schedule.");
            }
            tasksToRun = TaskUtils.filterTasksByName(allTasks, getTaskNamesToRun());
        }

        //call schedule manager
        ScheduleManager scheduleManager;
        try {
            scheduleManager = ScheduleManager.getInstance();
            for (TaskConfig task : tasksToRun) {
                if (task.isEnable()) {
                    logger.debug("Scheduling '{}'", task.getName());
                    scheduleManager.scheduleTask(task);
                }
            }
            scheduleManager.startScheduler();
        } catch (SchedulerException e) {
            logger.error("Error occurred while scheduling the tasks.", e);
        }
    }

    public List<String> getTaskNamesToRun() {
        return taskNamesToRun;
    }

    public void setScheduleAll(boolean scheduleAll) {
        this.scheduleAll = scheduleAll;
    }


}
