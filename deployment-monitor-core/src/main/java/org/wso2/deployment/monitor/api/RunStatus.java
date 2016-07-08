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
package org.wso2.deployment.monitor.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieve the job execution status.
 * This is returned from the {@link DeploymentMonitorTask} instance
 * and is is passed into the {@link OnResultCallback#callback(RunStatus)} method
 *
 */
public class RunStatus {

    /**
     * Designates success or failure of the test run
     */
    private boolean success = true;

    private String taskName;

    private String serverGroupName;

    private String message;

    private List<HostBean> successHosts;

    private List<HostBean> failedHosts;

    private Map<String, Object> customTaskDetails = new HashMap<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    public void setServerGroupName(String serverGroupName) {
        this.serverGroupName = serverGroupName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getCustomTaskDetails() {
        return customTaskDetails;
    }

    public void setCustomTaskDetails(Map<String, Object> customTaskDetails) {
        this.customTaskDetails = customTaskDetails;
    }

    public List<HostBean> getSuccessHosts() {
        return successHosts;
    }

    public void setSuccessHosts(List<HostBean> successHosts) {
        this.successHosts = successHosts;
    }

    public List<HostBean> getFailedHosts() {
        return failedHosts;
    }

    public void setFailedHosts(List<HostBean> failedHosts) {
        this.failedHosts = failedHosts;
    }
}
