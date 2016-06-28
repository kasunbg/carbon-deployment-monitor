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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tenant Login based on Carbon Authentication.
 * NOTE: This cannot be used for multiple hosts. For multiple hosts use {@link ServerTenantLoginTask}
 */
public class TenantLoginTask implements DeploymentMonitorTask {

    private static final String TASK_NAME = "TenantLoginTask";

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        GlobalConfig.TenantConfig tenantConfig = (GlobalConfig.TenantConfig) customParams
                .get(MonitoringConstants.DEFAULT_TENANT_KEY);
        //Timeout value is defined in seconds, hence converting to milli seconds
        int timeout = ((int) customParams.get(MonitoringConstants.TIMEOUT)) * 1000;
        String failedMsg = serverGroup.getName() + " : " + TASK_NAME + " Failed : ";

        //In this test we consider only one host, hence getting the first element.
        String host = serverGroup.getHosts().get(0);
        String endPoint = host + "/services/AuthenticationAdmin";
        AuthenticationAdminStub authenticationAdminStub;
        try {
            authenticationAdminStub = new AuthenticationAdminStub(endPoint);
        } catch (AxisFault axisFault) {
            return createFaultStatus(failedMsg + axisFault.getMessage(), axisFault);
        }

        try {
            authenticationAdminStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
            boolean loginSuccess = authenticationAdminStub
                    .login(tenantConfig.getUsername(), tenantConfig.getPassword(), "localhost");
            if (loginSuccess) {
                RunStatus status = new RunStatus();
                status.setSuccess(true);
                status.setMessage(serverGroup.getName() + " : " + TASK_NAME + " SUCCESS");
                return status;
            } else {
                return createFaultStatus(failedMsg + "Invalid Credentials", null);
            }
        } catch (RemoteException | LoginAuthenticationExceptionException e) {
            return createFaultStatus(failedMsg + e.getMessage(), e);
        }
    }

    private RunStatus createFaultStatus(String msg, Object e) {
        RunStatus status = new RunStatus();
        Map<String, Object> customReturnDetails;
        status.setSuccess(false);
        status.setMessage(msg);
        customReturnDetails = new HashMap<>();
        customReturnDetails.put("Exception", e);
        status.setCustomTaskDetails(customReturnDetails);
        return status;
    }
}
