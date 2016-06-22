/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.deployment.monitor.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.api.ServerGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A simple Task Implementation which prints a log
 */
public class SimpleTask implements DeploymentMonitorTask {
    private static final Logger logger = LoggerFactory.getLogger(SimpleTask.class);

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        logger.info(serverGroup.getName() + ".SimpleTask Executed at : " + simpleDateFormat.format(new Date()));
        RunStatus runStatus = new RunStatus();
        runStatus.setSuccess(true);
        runStatus.setMessage(serverGroup.getName() + ".SimpleTask");
        return runStatus;
    }

}
