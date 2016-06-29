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

package org.wso2.deployment.monitor.impl.task;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.utils.http.HttpRestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Pings the given servers
 */
public class PingTask implements DeploymentMonitorTask {

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {

        RunStatus status = new RunStatus();
        HttpRestClient restClient = new HttpRestClient();

        List<String> failedHosts = new ArrayList<>();
        List<String> successHosts = new ArrayList<>();

        //We use this map to send the details of each failed host
        Map<String, Object> resultMap = new HashMap<>();

        //Check for below values in the response
        String path;
        if (customParams.get("path") != null) {
            path = (String) customParams.get("path");
        } else {
            path = "/carbon/product/about.html";
        }

        int statusCode;
        if (customParams.get("statusCode") != null) {
            statusCode = (Integer) customParams.get("statusCode");
        } else {
            statusCode = HttpStatus.SC_OK;
        }

        String bodyValue;
        if (customParams.getProperty("responseContains") != null) {
            bodyValue = customParams.getProperty("responseContains");
        } else {
            bodyValue = "About WSO2 Carbon";
        }

        for (String host : serverGroup.getHosts()) {
            ClientResponse response;
            try {
                response = restClient
                        .get(host + path, new HashMap<String, String>(), new HashMap<String, String>(), null);
            } catch (Exception e) {
                failedHosts.add(host);
                resultMap.put(host, e.getMessage());
                continue;
            }

            if (response.getStatusCode() != statusCode) {
                failedHosts.add(host);
                resultMap.put(host, response.getStatusCode() + " " + response.getMessage());
                continue;
            }

            String responseAsString = response.getEntity(String.class);
            if (!responseAsString.contains(bodyValue)) {
                failedHosts.add(host);
                resultMap.put(host, "Response body does not contain the defined value");
            }
        }

        if (failedHosts.isEmpty()) {
            status.setSuccess(true);
            status.setSuccessHosts(successHosts);
            return status;
        } else {
            status.setSuccess(false);
            status.setFailedHosts(failedHosts);
            status.setCustomTaskDetails(resultMap);
            return status;
        }
    }
}
