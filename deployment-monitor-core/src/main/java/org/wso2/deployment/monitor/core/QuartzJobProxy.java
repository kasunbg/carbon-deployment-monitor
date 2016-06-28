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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.model.ServerGroup;

import java.util.Properties;

/**
 * Acts as a proxy between quartz and the actual task.
 * This helps to separate the API and Quartz.
 *
 */
public class QuartzJobProxy implements Job {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobProxy.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String taskName = dataMap.getString("taskName");
        String taskClassName = dataMap.getString("taskClass");
        String callbackClassName = dataMap.getString("callbackClass");
        Object serverGroup = dataMap.get("serverGroup");
        Object customParams = dataMap.get("customParams");

        TaskUtils.callTask(taskName, taskClassName, callbackClassName, (ServerGroup) serverGroup,
                (Properties) customParams);
    }
}
