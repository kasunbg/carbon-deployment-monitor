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
package org.wso2.carbon.devops.monitor;

import org.apache.axis2.util.Utils;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.devops.monitor.beans.DeploymentSynchronizerInfo;
import org.wso2.carbon.devops.monitor.beans.Patch;
import org.wso2.carbon.devops.monitor.beans.ServerInfo;
import org.wso2.carbon.devops.monitor.internal.OSGiDataHolder;
import org.wso2.carbon.ui.CarbonUIUtil;

import java.net.SocketException;

/**
 * An axis2 service that returns the Carbon server info
 * This includes :
 *      Patch details
 *      Deployment Artifacts
 *      Dropins
 *      //Configurations
 *      //Cluster information
 *
 *      todo create a serverId and save it in the local registry
 *      todo extend admin service
 */
@SuppressWarnings("unused")
public class ServerStatusReporter {

    private static OSGiDataHolder dataHolder = OSGiDataHolder.getInstance();

    private static final String SERVER_URL = "https://${carbon.local.ip}:${carbon.management.port}"
            + "${carbon.context}/services/";

    public ServerInfo getServerInfo() {

        ServerInfo serverInfo = new ServerInfo();

        //1
        fillProductMetadata(serverInfo);

        //2 get patch info
        Patch[] patches = new PatchInfoGenerator().generate();
        serverInfo.setPatchInfo(patches);

        //3 get dropins info

        //4 get depsync info
        DeploymentSynchronizerInfo depsyncInfo = new DeploymentSynchronizerInfoGenerator().generate();
        serverInfo.setDeploymentSynchronizerInfo(depsyncInfo);

        return serverInfo;
    }

    public Patch[] getPatchInfo() {
        return new PatchInfoGenerator().generate();
    }

    public DeploymentSynchronizerInfo getDeploymentSynchronizerInfo() {
        return new DeploymentSynchronizerInfoGenerator().generate();
    }

    private void fillProductMetadata(ServerInfo serverInfo) {
        ServerConfigurationService configService = dataHolder.getServerConfigurationService();
        String productName = configService.getFirstProperty("Name");
        String productVersion = configService.getFirstProperty("Version");
        String profile = System.getProperty("profile", "default");

        String serverURL = CarbonUIUtil.getAdminConsoleURL(configService.getFirstProperty("WebContextRoot"));

        String ip;
        try {
            ip = Utils.getIpAddress();
        } catch (SocketException e) {
            ip = "";
        }

        serverInfo.setProductName(productName);
        serverInfo.setProductVersion(productVersion);
        serverInfo.setServerProfile(profile);
        serverInfo.setServerURL(serverURL);
        serverInfo.setServerIP(ip);
    }

}
