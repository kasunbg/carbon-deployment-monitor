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

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.Command;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

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

    @Override
    public void execute(DeploymentMonitorConfiguration deploymentMonitorConfiguration) {
        List<ServerGroup> serverGroups = deploymentMonitorConfiguration.getServerGroups();
        List<TaskConfig> tasks = deploymentMonitorConfiguration.getTasks();

        //call schedule manager
        ScheduleManager scheduleManager;
        try {
            scheduleManager = new ScheduleManager();
            for (TaskConfig task : tasks) {
                try {
                    if (task.isEnable()) {
                        scheduleManager.scheduleTask(task, serverGroups);
                    }
                } catch (SchedulerException e) {
                    logger.error("Error occurred while scheduling the task - " + task.getName(), e);

                }
            }
            scheduleManager.startScheduler();
        } catch (SchedulerException e) {
            logger.error("Error occurred while scheduling the tasks.", e);
        }

    }
}
