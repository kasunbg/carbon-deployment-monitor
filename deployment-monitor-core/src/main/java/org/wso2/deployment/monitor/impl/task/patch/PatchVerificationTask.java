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
package org.wso2.deployment.monitor.impl.task.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.devops.monitor.beans.xsd.Patch;
import org.wso2.carbon.devops.monitor.beans.xsd.ServerInfo;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.impl.agent.ServerStatusReporterClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * todo
 */
public class PatchVerificationTask implements DeploymentMonitorTask {
    private static final Logger logger = LoggerFactory.getLogger(PatchVerificationTask.class);

    private static final ServerStatusReporterClient serverStatusReporterClient = new ServerStatusReporterClient();

    @Override
    public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        logger.info("--- Running patch verification task ---");

        if (serverGroup.getHosts().size() <= 1) {
            logger.warn("Patch comparison skipped for '{}' since at least two hosts are required.",
                    serverGroup.getName());
            return new RunStatus();
        }

        boolean isSuccess = true;
        Iterator<String> hostsIterator = serverGroup.getHosts().iterator();
        Map<String, Patch> firstPatchMap = patchListToMap(serverStatusReporterClient.
                getServerInfo(hostsIterator.next()).getPatchInfo());
        logger.info("Comparing patches in servers in '{}' against the server '{}'", serverGroup.getName(),
                serverGroup.getHosts().get(0));
        logger.info("Patches in {}: {}", serverGroup.getHosts().get(0), firstPatchMap.keySet());

        while (hostsIterator.hasNext()) {
            String host = hostsIterator.next();
            ServerInfo serverInfo = serverStatusReporterClient.getServerInfo(host);
            Map<String, Patch> patchMap = patchListToMap(serverInfo.getPatchInfo());
            PatchDiffBean patchDiffBean = PatchUtils.compare(firstPatchMap, patchMap);

            if (!patchDiffBean.getMissingPatches().isEmpty()) {
                logger.warn("Missing patches in {} : {}", host, patchDiffBean.getMissingPatches());
                isSuccess = false;
            }
            if (!patchDiffBean.getExtraPatches().isEmpty()) {
                logger.warn("Extra patches in {} : {}", host, patchDiffBean.getExtraPatches());
                isSuccess = false;
            }

            //do md5sum checks
        }

        logger.info("--- End of patch verification task ---\n");

        RunStatus runStatus = new RunStatus();
        runStatus.setSuccess(isSuccess);
        runStatus.setMessage("Verified the patches in " + serverGroup.getName());
        return runStatus;
    }

    private Map<String, Patch> patchListToMap(List<Patch> patchList) {
        Map<String, Patch> patchMap = new HashMap<>(patchList.size());
        for (Patch patch : patchList) {
            if (patch == null) {
                continue;
            }
            patchMap.put(patch.getPatchId(), patch);
        }
        return patchMap;
    }
}
