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

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Scheduler listener extending SchedulerListenerSupport implemented in this class
 */
public class SchedulerListenerImpl implements SchedulerListener {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerListenerImpl.class);
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");

    @Override public void jobScheduled(Trigger trigger) {
        if (logger.isDebugEnabled()) {
            logger.debug("Job Scheduled : " + trigger);
        }
    }

    @Override public void jobUnscheduled(TriggerKey triggerKey) {
        Date date = new Date();
        logger.info("Job : " + triggerKey + " un-scheduled at : " + simpleDateFormat.format(date));
    }

    @Override public void triggerFinalized(Trigger trigger) {

    }

    @Override public void triggerPaused(TriggerKey triggerKey) {

    }

    @Override public void triggersPaused(String s) {

    }

    @Override public void triggerResumed(TriggerKey triggerKey) {

    }

    @Override public void triggersResumed(String s) {

    }

    @Override public void jobAdded(JobDetail jobDetail) {
        if (logger.isDebugEnabled()) {
            logger.debug("Added Job : " + jobDetail);
        }
    }

    @Override public void jobDeleted(JobKey jobKey) {
//        if (logger.isDebugEnabled()) {
            logger.info("Deleted Job : " + jobKey);
//        }
    }

    @Override public void jobPaused(JobKey jobKey) {

    }

    @Override public void jobsPaused(String s) {

    }

    @Override public void jobResumed(JobKey jobKey) {

    }

    @Override public void jobsResumed(String s) {

    }

    @Override public void schedulerError(String s, SchedulerException e) {
        Date date = new Date();
        logger.error("Scheduler error : " + s + " occurred at : " + simpleDateFormat.format(date), e);
    }

    @Override public void schedulerInStandbyMode() {

    }

    @Override public void schedulerStarted() {
        Date date = new Date();
        logger.info("Scheduler started at : " + simpleDateFormat.format(date));
    }

    @Override public void schedulerStarting() {

    }

    @Override public void schedulerShutdown() {
        Date date = new Date();
        logger.info("Scheduler shutdown at : " + simpleDateFormat.format(date));
    }

    @Override public void schedulerShuttingdown() {

    }

    @Override public void schedulingDataCleared() {

    }
}