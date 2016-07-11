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
package org.wso2.deployment.monitor.impl.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.devops.monitor.beans.xsd.DeploymentSynchronizerInfo;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.MonitoringConstants;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.impl.agent.ServerStatusReporterClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Verify Deployment Synchronizer functionality
 * <p/>
 * 1. verify a single server - by comparing against the remote svn server
 * 2. verify it across the servers
 * <p/>
 * <p/>
 * <p/>
 * Check missing DepSync config
 * check client type is svnkit
 * check remote and working copy revisions are the same
 * Check for errorFiles and inconsistent files
 */
public class DeploymentSynchronizerTestTask implements DeploymentMonitorTask {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentSynchronizerTestTask.class);
    private static final ServerStatusReporterClient serverStatusReporterClient = new ServerStatusReporterClient();

    public static final String DEPSYNC_ENABLED = "DEPSYNC_ENABLED";
    public static final String MISSING_SVNKIT = "MISSING_SVNKIT";
    public static final String OUT_OF_DATE_WORKING_COPIES = "OUT_OF_DATE_WORKING_COPIES";
    public static final String CONFLICTED_WORKING_COPIES = "CONFLICTED_WORKING_COPIES";
    public static final String INCONSISTENT_WORKING_COPIES = "INCONSISTENT_WORKING_COPIES";
    public static final String NO_DEPSYNC = "NO_DEPSYNC";

    @Override
    public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        List<String> notEnabledHosts = new ArrayList<>();
        boolean isEnabledAtLeastOnce = false;
        List<String> missingSvnkitHosts = new ArrayList<>();
        List<String> outOfDateWorkingCopy = new ArrayList<>();
        List<String> workingCopyFileErrors = new ArrayList<>();
        List<String> workingCopyFileInconsistencies = new ArrayList<>();

        if (serverGroup.getHosts().isEmpty()) {
            logger.info("No hosts");
        }

        for (String host : serverGroup.getHosts()) {
            DeploymentSynchronizerInfo info = serverStatusReporterClient.getDeploymentSynchronizerInfo(host);

            //test if config is missing
            if (!info.isEnabled()) {
                notEnabledHosts.add(host);
                continue;
            } else {
                isEnabledAtLeastOnce = true;
            }

            if (!MonitoringConstants.DEFAULT_DEPSYNC_CLIENT_TYPE.equalsIgnoreCase(info.getClientType())) {
                missingSvnkitHosts.add(host);
            }

            if (info.getRemoteSvnRevision() > info.getWorkingCopyRevision()) {
                String logMessage = String
                        .format("%s has a out of date working copy. Working Copy Revision: %d, Remote revision: %d",
                                host, info.getWorkingCopyRevision(), info.getRemoteSvnRevision());
                outOfDateWorkingCopy.add(logMessage);
            }

            //verify working copy is conflicted
            if (info.getErrorFiles().size() >= 1) {
                StringBuilder fileErrorMessage = new StringBuilder();
                fileErrorMessage.append(host);
                fileErrorMessage.append(" host working copy has an unrecoverable error on these files : ");

                for (String errorFile : info.getErrorFiles()) {
                    fileErrorMessage.append(errorFile);
                    fileErrorMessage.append(", ");
                }
                workingCopyFileErrors.add(fileErrorMessage.toString());
            }

            //verify working copy is locally modified even though autocommit is set to false
            if (info.getInconsistentFiles().size() >= 1) {
                StringBuilder fileInconsistenciesMessage = new StringBuilder();
                fileInconsistenciesMessage.append(host);
                fileInconsistenciesMessage
                        .append(" host working copy is in inconsistent state due to modified files: ");

                for (String inconsistentFile : info.getInconsistentFiles()) {
                    fileInconsistenciesMessage.append(inconsistentFile);
                    fileInconsistenciesMessage.append(", ");
                }
                workingCopyFileInconsistencies.add(fileInconsistenciesMessage.toString());
            }

        }

        //runstatus object for callback
        RunStatus runStatus = new RunStatus();
        Map<String, Object> taskDetails = new HashMap<>();
        taskDetails.put(DEPSYNC_ENABLED, isEnabledAtLeastOnce);
        taskDetails.put(NO_DEPSYNC, notEnabledHosts);
        taskDetails.put(MISSING_SVNKIT, missingSvnkitHosts);
        taskDetails.put(OUT_OF_DATE_WORKING_COPIES, outOfDateWorkingCopy);
        taskDetails.put(CONFLICTED_WORKING_COPIES, workingCopyFileErrors);
        taskDetails.put(INCONSISTENT_WORKING_COPIES, workingCopyFileInconsistencies);
        runStatus.setCustomTaskDetails(taskDetails);

        //log
        if (!isEnabledAtLeastOnce) {
            logger.info("Deployment Synchronizer is not enabled for '{}'", serverGroup.getName());
            return runStatus;
        }

        logger.info("Verifying Deployment Synchronizer configuration in '{}'", serverGroup.getName());
        if (!notEnabledHosts.isEmpty()) {
            logger.error("Hosts without DepSync : {}", notEnabledHosts);
        }

        if (!missingSvnkitHosts.isEmpty()) {
            logger.error("Svnkit library is missing in: {}", missingSvnkitHosts);
        }

        for (String outOfDateMessage : outOfDateWorkingCopy) {
            logger.error(outOfDateMessage);
        }

        for (String fileErrorMessage : workingCopyFileErrors) {
            logger.error(fileErrorMessage);
        }

        for (String fileInconsistencyMessage : workingCopyFileInconsistencies) {
            logger.error(fileInconsistencyMessage);
        }

        return runStatus;

    }
}
