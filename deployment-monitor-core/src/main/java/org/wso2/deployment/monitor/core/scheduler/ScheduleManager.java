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

package org.wso2.deployment.monitor.core.scheduler;

import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.QuartzJobProxy;
import org.wso2.deployment.monitor.core.TaskUtils;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;
import org.wso2.deployment.monitor.core.scheduler.utils.SchedulerConstants;
import org.wso2.deployment.monitor.core.scheduler.utils.TriggerUtilities;

import java.util.ArrayList;
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
 * This class will schedule tasks
 */
public class ScheduleManager {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleManager.class);

    private static volatile ScheduleManager scheduleManager = null;
    private Scheduler scheduler;
    //This is used to randomly schedule all the tests between a given time
    private Random generator;

    /**
     * Initializes ScheduleManager
     */
    private ScheduleManager() throws SchedulerException {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        SchedulerListenerImpl schedulerListener = new SchedulerListenerImpl();
        scheduler.getListenerManager().addSchedulerListener(schedulerListener);
        generator = new Random();
    }

    /**
     * Initializes the {@link ScheduleManager}
     */
    private static void initialize() throws SchedulerException {
        synchronized (ScheduleManager.class) {
            if (scheduleManager == null) {
                scheduleManager = new ScheduleManager();
            }
        }
    }

    /**
     * Returns a {@link ScheduleManager} instance
     *
     * @return {@link ScheduleManager}
     * @throws SchedulerException
     */
    public static ScheduleManager getInstance() throws SchedulerException {
        if(scheduleManager == null){
            initialize();
        }
        return scheduleManager;
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

    /**
     * Schedules jobs of the given Task for each server group defined for the task
     *
     * @param taskName Task Name
     */
    public void scheduleTask(String taskName) {
        TaskConfig taskConfig = TaskUtils.getTaskConfigByName(taskName);
        if (taskConfig == null) {
            logger.warn("Scheduling Task failed. Unable to find a task with the name : " + taskName);
            return;
        }
        scheduleTask(taskConfig);
    }

    /**
     * Schedules all the jobs of a given server
     *
     * @param serverName Task Name
     */
    public boolean scheduleTasksOfServer(String serverName) {
        return false;
        //todo
    }

    /**
     * Schedules a job of the given Task for the given server group
     *
     * @param taskName Task Name
     */
    public boolean scheduleTaskForServer(String taskName, String serverGroupName) {
        TaskConfig taskConfig = TaskUtils.getTaskConfigByName(taskName);
        if (taskConfig == null) {
            logger.warn("Scheduling Task failed. Unable to find a task with the name " + taskName);
            return false;
        }
        ServerGroup serverGroup = TaskUtils.getServerGroupsByTaskConfig(taskConfig).get(serverGroupName);
        return scheduleTaskForServer(taskConfig, serverGroup);
    }

    /**
     * Schedules jobs of the given Task for each server group defined for the task
     *
     * @param taskConfig {@link TaskConfig}
     */
    void scheduleTask(TaskConfig taskConfig) {
        Map<String, ServerGroup> serverGroupMap = TaskUtils.getServerGroupsByTaskConfig(taskConfig);
        for (Map.Entry<String, ServerGroup> entry : serverGroupMap.entrySet()) {
            scheduleTaskForServer(taskConfig, entry.getValue());
        }
    }

    /**
     * Schedules a job of the given Task for the given server group
     *
     * @param taskConfig  {@link TaskConfig}
     * @param serverGroup {@link ServerGroup}
     */
    private boolean scheduleTaskForServer(TaskConfig taskConfig, ServerGroup serverGroup) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(SchedulerConstants.TASK_CLASS, taskConfig.getClassName());
        dataMap.put(SchedulerConstants.TASK_NAME, taskConfig.getName());
        dataMap.put(SchedulerConstants.CALLBACK_CLASS, taskConfig.getOnResult());
        dataMap.put(SchedulerConstants.CUSTOM_PARAMS, taskConfig.getTaskParams());

        String serverName = serverGroup.getName();
        dataMap.put(SchedulerConstants.SERVER_GROUP, serverGroup);

        String jobName = taskConfig.getName();
        JobDetail job = newJob(QuartzJobProxy.class).withIdentity(jobName, serverName).usingJobData(dataMap).build();

        Trigger trigger;
        if (SchedulerConstants.SIMPLE_TRIGGER.equalsIgnoreCase(taskConfig.getTriggerType())) {
            if (taskConfig.getTrigger().endsWith(TriggerUtilities.SECONDS)) {
                trigger = getSimpleTriggerInSeconds(jobName, serverName, taskConfig.getTrigger());
            } else if (taskConfig.getTrigger().endsWith(TriggerUtilities.MINUTES)) {
                trigger = getSimpleTriggerInMinutes(jobName, serverName, taskConfig.getTrigger());
            } else {
                trigger = getSimpleTriggerInHours(jobName, serverName, taskConfig.getTrigger());
            }
        } else if (SchedulerConstants.ONE_TIME.equalsIgnoreCase(taskConfig.getTriggerType())) {
            trigger = getOneTimeTrigger(jobName, serverName);
        } else {
            trigger = getCronTrigger(jobName, serverName, taskConfig.getTrigger());
        }

        try {
            scheduler.scheduleJob(job, trigger);
            return true;
        } catch (SchedulerException e) {
            logger.error("Scheduling Task - " + taskConfig.getName() + " for Server - " + serverName + " failed", e);
        }
        return false;
    }

    /**
     * Un-Schedule all the jobs of the given task
     *
     * @param taskName Name of the Task
     */
    public void unScheduleTask(String taskName) {
        TaskConfig taskConfig = TaskUtils.getTaskConfigByName(taskName);
        if (taskConfig == null) {
            logger.warn("Un-scheduling Task failed. Unable to find a task with the name " + taskName);
            return;
        }
        for (String server : taskConfig.getServers()) {
            unScheduleTaskForServer(taskName, server);
        }
    }

    /**
     * Un-Schedule the job of the Task for the given Server
     *
     * @param taskName        Name of the Task
     * @param serverGroupName Group name of the server
     */
    public boolean unScheduleTaskForServer(String taskName, String serverGroupName) {
        try {
            return scheduler.unscheduleJob(triggerKey(taskName, serverGroupName));
        } catch (SchedulerException e) {
            logger.error("Un-Scheduling Task failed {}", e);
        }
        return false;
    }

    /**
     * Un-Schedule all the jobs of a given Server
     *
     * @param serverGroupName Group name of the server
     */
    public boolean unScheduleTasksOfServer(String serverGroupName) {
        try {
            List<TriggerKey> triggerList = new ArrayList<>();
            triggerList.addAll(scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(serverGroupName)));
            return scheduler.unscheduleJobs(triggerList);
        } catch (SchedulerException e) {
            logger.error("Un-Scheduling Task failed {}", e);
        }
        return false;
    }

    /**
     * Pause all the jobs of the given task
     *
     * @param taskName Name of the Task
     */
    public void pauseTask(String taskName) {
        TaskConfig taskConfig = TaskUtils.getTaskConfigByName(taskName);
        if (taskConfig == null) {
            logger.warn("Pausing Task failed. Unable to find a task with the name " + taskName);
            return;
        }
        for (String server : taskConfig.getServers()) {
            pauseTaskForServer(taskName, server);
        }
    }

    /**
     * Pause the job of the Task for the given Server
     *
     * @param taskName        Name of the Task
     * @param serverGroupName Group name of the server
     */
    public boolean pauseTaskForServer(String taskName, String serverGroupName) {
        try {
            scheduler.pauseTrigger(TriggerKey.triggerKey(taskName, serverGroupName));
            return true;
        } catch (SchedulerException e) {
            logger.error("Pausing Task failed {}", e);
        }
        return false;
    }

    /**
     * Pause all the jobs of the given Server
     *
     * @param serverGroupName Group name of the server
     */
    public boolean pauseTasksOfServer(String serverGroupName) {
        try {
            scheduler.pauseTriggers(GroupMatcher.<TriggerKey>groupEquals(serverGroupName));
            return true;
        } catch (SchedulerException e) {
            logger.error("Pausing Tasks failed for server" + serverGroupName + " {}", e);
        }
        return false;
    }

    /**
     * Resumes all the jobs of the given task
     *
     * @param taskName Name of the Task
     */
    public void resumeTask(String taskName) {
        TaskConfig taskConfig = TaskUtils.getTaskConfigByName(taskName);
        if (taskConfig == null) {
            logger.warn("Resuming Task failed. Unable to find a task with the name " + taskName);
            return;
        }
        for (String server : taskConfig.getServers()) {
            resumeTaskForServer(taskName, server);
        }
    }

    /**
     * Resumes the job of the task for the given server
     *
     * @param taskName        Name of the Task
     * @param serverGroupName Group name of the server
     */
    public boolean resumeTaskForServer(String taskName, String serverGroupName) {
        try {
            scheduler.resumeTrigger(TriggerKey.triggerKey(taskName, serverGroupName));
            return true;
        } catch (SchedulerException e) {
            logger.error("Resuming Task failed {}", e);
        }
        return false;
    }

    /**
     * Resumes all the jobs of a given server
     *
     * @param serverGroupName Group name of the server
     */
    public boolean resumeTasksOfServer(String serverGroupName) {
        try {
            scheduler.resumeTriggers(GroupMatcher.<TriggerKey>groupEquals(serverGroupName));
            return true;
        } catch (SchedulerException e) {
            logger.error("Resuming Tasks failed for server" + serverGroupName + " {}", e);
        }
        return false;
    }

    /**
     * Returns cron trigger
     * Triggers will have Task's name as the name and group name as the group name
     *
     * @return Quartz Cron Trigger
     */
    private Trigger getCronTrigger(String triggerName, String triggerGroup, String expression) {
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
    private Trigger getSimpleTriggerInHours(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.HOURS)[0].trim());

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
    private Trigger getSimpleTriggerInMinutes(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.MINUTES)[0].trim());

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
    private Trigger getSimpleTriggerInSeconds(String triggerName, String triggerGroup, String expression) {
        if (!TriggerUtilities.isValidSimpleTriggerExpression(expression)) {
            logger.error(triggerName + " Task will not be scheduled because the provided simple expression is invalid."
                    + " Task will be executed once.");
            return newTrigger().withIdentity(triggerName, triggerGroup).withSchedule(simpleSchedule()).startNow()
                    .build();
        }
        int interval = Integer.parseInt(expression.split(TriggerUtilities.SECONDS)[0].trim());

        return newTrigger().withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule().withIntervalInSeconds(interval).repeatForever()).startAt(
                        futureDate(generator.nextInt(SchedulerConstants.INIT_DURATION),
                                DateBuilder.IntervalUnit.SECOND)).build();

    }

    /**
     * Returns a trigger which will only fire one time
     *
     * @return Quartz Simple Trigger
     */
    private Trigger getOneTimeTrigger(String triggerName, String triggerGroup) {
        return newTrigger().withIdentity(triggerName, triggerGroup).startNow().build();
    }

}
