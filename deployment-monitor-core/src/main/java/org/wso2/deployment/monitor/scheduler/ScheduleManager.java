/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.deployment.monitor.scheduler;

import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.ServerGroup;
import org.wso2.deployment.monitor.core.QuartzJobProxy;
import org.wso2.deployment.monitor.core.model.TaskConfig;
import org.wso2.deployment.monitor.scheduler.utils.SchedulerConstants;
import org.wso2.deployment.monitor.scheduler.utils.TriggerUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

/**
 * Helper class to schedule service tests using TaskScheduler
 */
public class ScheduleManager {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleManager.class);

    private Scheduler scheduler;
    private Random generator;

    /**
     * Initializes ScheduleManager
     */
    public ScheduleManager() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            SchedulerListenerImpl schedulerListener = new SchedulerListenerImpl();
            scheduler.getListenerManager().addSchedulerListener(schedulerListener);
            generator = new Random();
        } catch (SchedulerException e) {
            logger.error("Error occurred while initializing the scheduler.", e);
        }
    }

    /**
     * Starts Scheduler
     *
     * @throws org.quartz.SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        scheduler.start();
    }

    /**
     * Shutdown Scheduler
     *
     * @throws org.quartz.SchedulerException
     */
    public void shutDownScheduler() throws SchedulerException {
        scheduler.shutdown();
    }

    public void scheduleTask(TaskConfig taskConfig, List<ServerGroup> serverGroups) throws SchedulerException {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(SchedulerConstants.TASK_CLASS, taskConfig.getClassName());
        dataMap.put(SchedulerConstants.CALLBACK_CLASS, taskConfig.getOnResult());
        //Custom parameters for a Task
        dataMap.put(SchedulerConstants.CUSTOM_PARAMS, taskConfig.getTaskParams());

        Map<String, ServerGroup> serverGroupMap = new HashMap<>();
        for (ServerGroup serverGroup : serverGroups) {
            serverGroupMap.put(serverGroup.getName(), serverGroup);
        }

        for (String serverName : taskConfig.getServers()) {
            dataMap.put(SchedulerConstants.SERVER_GROUP, serverGroupMap.get(serverName));

            String jobName = serverName + "." + taskConfig.getName();
            JobDetail job = newJob(QuartzJobProxy.class).withIdentity(jobName, taskConfig.getGroup())
                    .usingJobData(dataMap).build();

            Trigger trigger;
            if (SchedulerConstants.SIMPLE_TRIGGER.equalsIgnoreCase(taskConfig.getTriggerType())) {
                if (taskConfig.getTrigger().endsWith(TriggerUtilities.MINUTES)) {
                    trigger = simpleTriggerIntervalsInMinutes(jobName, taskConfig.getGroup(), taskConfig.getTrigger());
                } else if (taskConfig.getTrigger().endsWith(TriggerUtilities.SECONDS)) {
                    trigger = simpleTriggerIntervalsInSeconds(jobName, taskConfig.getGroup(), taskConfig.getTrigger());
                } else {
                    trigger = simpleTriggerIntervalsInHours(jobName, taskConfig.getGroup(), taskConfig.getTrigger());
                }
            } else {
                trigger = cronTrigger(jobName, taskConfig.getGroup(), taskConfig.getTrigger());
            }
            scheduler.scheduleJob(job, trigger);
        }
    }

    public void unScheduleTask(String jobName, String jobGroup) throws SchedulerException {
        scheduler.unscheduleJob(triggerKey(jobName, jobGroup));
    }

    /**
     * Returns cron trigger
     * Triggers will have Task's name as the name and group name as the group name
     *
     * @return Quartz Simple Trigger
     */
    private Trigger cronTrigger(String triggerName, String triggerGroup, String expression) {

        if (TriggerUtilities.isValidCronExpression(expression)) {
            CronTrigger trigger;
            trigger = newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(cronSchedule(expression))
                    .build();
            return trigger;
        } else {
            logger.error(triggerName + " Task will not be scheduled because the provided cron expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }

    }

    /**
     * Returns Simple trigger with interval in hours
     * Triggers will have Task's name as the name and group name as the group name
     *
     * @return Quartz Simple Trigger
     */
    private Trigger simpleTriggerIntervalsInHours(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.HOURS)[0]);

        return newTrigger().withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule().withIntervalInHours(interval).repeatForever())
                .startAt(futureDate(generator.nextInt(SchedulerConstants.INIT_DURATION), DateBuilder.IntervalUnit.HOUR))
                .build();
    }

    /**
     * Returns Simple trigger with interval in minutes
     * Triggers will have Task's name as the name and group name as the group name
     *
     * @return Quartz Simple Trigger
     */
    private Trigger simpleTriggerIntervalsInMinutes(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.MINUTES)[0]);

        return newTrigger().withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule().withIntervalInMinutes(interval).repeatForever()).startAt(
                        futureDate(generator.nextInt(SchedulerConstants.INIT_DURATION),
                                DateBuilder.IntervalUnit.MINUTE)).build();

    }

    /**
     * Returns Simple trigger with interval in seconds
     * Triggers will have Task's name as the name and group name as the group name
     *
     * @return Quartz Simple Trigger
     */
    private Trigger simpleTriggerIntervalsInSeconds(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.SECONDS)[0]);

        return newTrigger().withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule().withIntervalInSeconds(interval).repeatForever()).startAt(
                        futureDate(generator.nextInt(SchedulerConstants.INIT_DURATION),
                                DateBuilder.IntervalUnit.SECOND)).build();

    }

}