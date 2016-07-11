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
import org.wso2.deployment.monitor.api.HostBean;
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

    //Default values for the check
    private static final String DEFAULT_PATH = "/carbon/product/about.html";
    private static final String DEFAULT_RESPONSE_CONTAIN = "About WSO2 Carbon";

    private static final String PATH = "path";
    private static final String STATUS_CODE = "statusCode";
    private static final String RESPONSE_CONTAINS = "responseContains";

    private List<HostBean> failedHosts = new ArrayList<>();
    private List<HostBean> successHosts = new ArrayList<>();

    //We use this map to send the details of each failed host
    private Map<String, Object> resultMap = new HashMap<>();

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {

        RunStatus status = new RunStatus();
        HttpRestClient restClient = new HttpRestClient();

        //Check for below values in the response
        String path;
        if (customParams.get(PATH) != null) {
            path = (String) customParams.get(PATH);
        } else {
            path = DEFAULT_PATH;
        }

        int statusCode;
        if (customParams.get(STATUS_CODE) != null) {
            statusCode = (Integer) customParams.get(STATUS_CODE);
        } else {
            statusCode = HttpStatus.SC_OK;
        }

        String bodyValue;
        if (customParams.get(RESPONSE_CONTAINS) != null) {
            bodyValue = (String) customParams.get(RESPONSE_CONTAINS);
        } else {
            bodyValue = DEFAULT_RESPONSE_CONTAIN;
        }

        for (String host : serverGroup.getHosts()) {
            HostBean hostBean = new HostBean();
            hostBean.setHostName(host);
            hostBean.setNodeIndex(serverGroup.getHosts().indexOf(host));
            ClientResponse response;
            try {
                response = restClient
                        .get(host + path, new HashMap<String, String>(), new HashMap<String, String>(), null);
            } catch (Exception e) {
                addErrorDetails(hostBean, e.getMessage());
                continue;
            }

            if (response.getStatusCode() != statusCode) {
                addErrorDetails(hostBean, response.getStatusCode() + " " + response.getMessage());
                continue;
            }

            String responseAsString = response.getEntity(String.class);
            if (!responseAsString.contains(bodyValue)) {
                addErrorDetails(hostBean, "Response body does not contain the defined value");
            }
            hostBean.setTaskSuccess(true);
            successHosts.add(hostBean);
        }

        if (failedHosts.isEmpty()) {
            status.setSuccess(true);
            status.setSuccessHosts(successHosts);
            return status;
        } else {
            status.setSuccess(false);
            status.setSuccessHosts(successHosts);
            status.setFailedHosts(failedHosts);
            status.setCustomTaskDetails(resultMap);
            return status;
        }
    }

    private void addErrorDetails(HostBean hostBean, String message) {
        hostBean.setTaskSuccess(false);
        hostBean.setErrorMsg(message);
        failedHosts.add(hostBean);
    }
}
