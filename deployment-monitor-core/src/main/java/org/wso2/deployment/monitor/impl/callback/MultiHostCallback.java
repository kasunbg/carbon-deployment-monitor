/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.deployment.monitor.impl.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.OnResultCallback;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.utils.notification.email.EmailSender;
import org.wso2.deployment.monitor.utils.notification.sms.SMSSender;

/**
 * Simple Implementation for callback
 */
public class MultiHostCallback implements OnResultCallback {
    private static final Logger logger = LoggerFactory.getLogger(MultiHostCallback.class);

    @Override public void callback(RunStatus runStatus) {
        if (runStatus.isSuccess()) {
            logger.info(" [Task Successful] " +  runStatus.getServerGroupName() + " : " + runStatus.getTaskName());
        } else {
            String msg = " [Task Failed] " + runStatus.getServerGroupName() + " : " + runStatus.getTaskName();
            StringBuilder failedHosts = new StringBuilder();
            String sep = "";
            for(String host : runStatus.getFailedHosts()) {
                failedHosts.append(sep).append(host).append(" - ").append(runStatus.getCustomTaskDetails().get(host));
                sep = ", ";
            }
            logger.error(msg + ", Failed Hosts : [ " + failedHosts.toString() + " ]");
            EmailSender.getInstance()
                    .send(msg, "Failed Hosts [ " + failedHosts.toString() + " ]");
            SMSSender.getInstance().send(msg);
        }
    }
}
