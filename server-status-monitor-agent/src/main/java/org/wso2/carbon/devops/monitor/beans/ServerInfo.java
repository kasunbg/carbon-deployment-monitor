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
package org.wso2.carbon.devops.monitor.beans;

import java.util.List;

/**
 * The bean that is populated with the server data
 */
public class ServerInfo {

    private Patch[] patchInfo;

    public Patch[] getPatchInfo() {
        return patchInfo;
    }

    public void setPatchInfo(Patch[] patchInfo) {
        this.patchInfo = patchInfo;
    }

    public List<Bundle> plugins;
    public List<DeploymentArtifact> deploymentArtifactInfo;
    public List<DropinsBundle> dropins;
    public List<Bundle> libs;

    //  public List<> configurations;

}
