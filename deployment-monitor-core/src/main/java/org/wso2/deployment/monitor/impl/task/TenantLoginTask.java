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
import org.wso2.deployment.monitor.api.HostBean;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tenant login test scenario for set of hosts in a cluster implemented in this class
 */
public class TenantLoginTask implements DeploymentMonitorTask {

    private List<HostBean> failedHosts = new ArrayList<>();
    private List<HostBean> successHosts = new ArrayList<>();

    //We use this map to send the details of each failed host
    private Map<String, Object> resultMap = new HashMap<>();

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        GlobalConfig.TenantConfig tenantConfig = (GlobalConfig.TenantConfig) customParams
                .get(MonitoringConstants.DEFAULT_TENANT_KEY);
        //Timeout value is defined in seconds, hence converting to milli seconds
        int timeout = ((int) customParams.get(MonitoringConstants.TIMEOUT)) * 1000;

        AuthenticationAdminStub authenticationAdminStub;
        RunStatus status = new RunStatus();

        for (String host : serverGroup.getHosts()) {
            HostBean hostBean = new HostBean();
            hostBean.setHostName(host);
            hostBean.setNodeIndex(serverGroup.getHosts().indexOf(host) + 1);
            String endPoint = host + "/services/AuthenticationAdmin";
            try {
                authenticationAdminStub = new AuthenticationAdminStub(endPoint);
            } catch (AxisFault axisFault) {
                addErrorDetails(hostBean, axisFault.getMessage());
                continue;
            }

            try {
                authenticationAdminStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
                String username = tenantConfig.getUsername() + "@" + tenantConfig.getDomain();
                boolean loginSuccess = authenticationAdminStub.login(username, tenantConfig.getPassword(), "localhost");
                if (loginSuccess) {
                    hostBean.setTaskSuccess(true);
                    successHosts.add(hostBean);
                } else {
                    addErrorDetails(hostBean, "Returned false as login Status. Please check the credentials");
                }
            } catch (RemoteException | LoginAuthenticationExceptionException e) {
                addErrorDetails(hostBean, e.getMessage());
            }
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

    private void addErrorDetails(HostBean hostBean, String errorMsg) {
        hostBean.setTaskSuccess(false);
        hostBean.setErrorMsg(errorMsg);
        failedHosts.add(hostBean);
    }
}
