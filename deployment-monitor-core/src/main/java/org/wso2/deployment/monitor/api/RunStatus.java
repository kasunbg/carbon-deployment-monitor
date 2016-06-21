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
    private boolean success;

    private String message;

    private List<ServerGroup> successServerGroups;

    private List<ServerGroup> failedServerGroups;

    private Map<String, Object> customTaskDetails;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public List<ServerGroup> getSuccessServerGroups() {
        return successServerGroups;
    }

    public void setSuccessServerGroups(List<ServerGroup> successServerGroups) {
        this.successServerGroups = successServerGroups;
    }

    public List<ServerGroup> getFailedServerGroups() {
        return failedServerGroups;
    }

    public void setFailedServerGroups(List<ServerGroup> failedServerGroups) {
        this.failedServerGroups = failedServerGroups;
    }
}
