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

import org.apache.axis2.AxisFault;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.MonitoringConstants;
import org.wso2.deployment.monitor.core.model.GlobalConfig;
import org.wso2.deployment.monitor.core.model.ServerGroup;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tenant login test scenario for set of hosts in a cluster implemented in this class
 */
public class ServerTenantLoginTask implements DeploymentMonitorTask {

    private static final String TASK_NAME = "ServerTenantLoginTask";

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        GlobalConfig.TenantConfig tenantConfig = (GlobalConfig.TenantConfig) customParams
                .get(MonitoringConstants.DEFAULT_TENANT_KEY);
        //Timeout value is defined in seconds, hence converting to milli seconds
        int timeout = ((int) customParams.get(MonitoringConstants.TIMEOUT)) * 1000;

        AuthenticationAdminStub authenticationAdminStub;
        RunStatus status = new RunStatus();

        List<String> failedHosts = new ArrayList<>();
        List<String> successHosts = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();

        for (String host : serverGroup.getHosts()) {
            String endPoint = host + "/services/AuthenticationAdmin";
            try {
                authenticationAdminStub = new AuthenticationAdminStub(endPoint);
            } catch (AxisFault axisFault) {
                failedHosts.add(host);
                resultMap.put(host, axisFault.getMessage());
                continue;
            }

            try {
                authenticationAdminStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
                boolean loginSuccess = authenticationAdminStub
                        .login(tenantConfig.getUsername(), tenantConfig.getPassword(), "localhost");
                if (loginSuccess) {
                    successHosts.add(host);
                } else {
                    failedHosts.add(host);
                    resultMap.put(host, "Invalid Credentials");
                }
            } catch (RemoteException | LoginAuthenticationExceptionException e) {
                failedHosts.add(host);
                resultMap.put(host, e.getMessage());
            }

        }
        if (failedHosts.isEmpty()) {
            status.setSuccess(true);
            status.setMessage(serverGroup.getName() + " : " + TASK_NAME + " SUCCESS");
            status.setSuccessHosts(successHosts);
            status.setCustomTaskDetails(resultMap);
            return status;
        } else {
            status.setSuccess(false);
            status.setMessage(serverGroup.getName() + " - " + TASK_NAME + " Failed. ");
            status.setFailedHosts(failedHosts);
            status.setCustomTaskDetails(resultMap);
            return status;
        }

    }
}
