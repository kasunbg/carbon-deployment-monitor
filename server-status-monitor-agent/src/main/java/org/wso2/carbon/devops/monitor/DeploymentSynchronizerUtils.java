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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.devops.monitor.internal.DeploymentSynchronizerInfo;
import org.wso2.carbon.devops.monitor.internal.OSGiDataHolder;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * depsync utils
 */
public class DeploymentSynchronizerUtils {

    private static final Log log = LogFactory.getLog(DeploymentSynchronizerUtils.class);

    /**
     * Run svn info, get revision.
     * Get svn server's svn revision
     * <p/>
     * run svn status
     * Get whether there are any modified contents, and whether autocommit is true/false -
     * <p/>
     * If conflicted/obstructed -> NOT OK
     * If not autocommit -> modified/unversioned/added/deleted/missing/replaced/merged/  -> NOT OK
     * If no changes (normal/ignored/external/) -> then OK
     */
    public static DeploymentSynchronizerInfo getDeploymentSynchronizerInfo() {

        ServerConfigurationService serverConfig = OSGiDataHolder.getInstance().getServerConfigurationService();

        boolean enabled = Boolean
                .parseBoolean(serverConfig.getFirstProperty(MonitoringAgentConstants.DEPLOYMENT_SYNCHRONIZER_ENABLED));
        String repoType = serverConfig.getFirstProperty(MonitoringAgentConstants.REPOSITORY_TYPE);
        String username = serverConfig.getFirstProperty(MonitoringAgentConstants.SVN_USER);
        String password = serverConfig.getFirstProperty(MonitoringAgentConstants.SVN_PASSWORD);

        boolean autoCommit = Boolean.parseBoolean(serverConfig.getFirstProperty(MonitoringAgentConstants.AUTO_COMMIT));
        boolean autoCheckout = Boolean
                .parseBoolean(serverConfig.getFirstProperty(MonitoringAgentConstants.AUTO_CHECKOUT));

        DeploymentSynchronizerInfo deploymentSynchronizerInfo = new DeploymentSynchronizerInfo();
        deploymentSynchronizerInfo.setEnabled(enabled);
        deploymentSynchronizerInfo.setAutoCommit(autoCommit);
        deploymentSynchronizerInfo.setAutoCheckout(autoCheckout);

        if (!enabled) {
            return deploymentSynchronizerInfo;
        }

        try {
            if (!"svn".equalsIgnoreCase(repoType)) { //todo move string to constant
                log.debug("Only SVN based Deployment Synchronization config is support at the moment.");
                return deploymentSynchronizerInfo; //we only support svn atm
            }
            String clientType = serverConfig.getFirstProperty(MonitoringAgentConstants.SVN_CLIENT);
            if (clientType == null) {
                clientType = SVNClientAdapterFactory.getPreferredSVNClientType();
                if (log.isDebugEnabled()) {
                    log.debug("Deployment Synchronizer SVN client type: " + clientType);
                }
            }

            ISVNClientAdapter svnClient = SVNClientAdapterFactory.createSVNClient(clientType);
            if (username != null) {
                svnClient.setUsername(username);
                svnClient.setPassword(password);
            }
            List<String> errorList = new ArrayList<>();
            List<String> inconsistentStateList = new ArrayList<>();

            //do it for super tenant for the moment
            File repoPath = new File(MultitenantUtils.getAxis2RepositoryPath(MultitenantConstants.SUPER_TENANT_ID));
            ISVNInfo workingCopySvnInfo = svnClient.getInfo(repoPath);
            long workingCopyRevision = workingCopySvnInfo.getLastChangedRevision().getNumber();

            ISVNInfo remoteSvnInfo = svnClient.getInfo(workingCopySvnInfo.getUrl());
            long remoteSvnRevision = remoteSvnInfo.getLastChangedRevision().getNumber();

            //verify svn status
            ISVNStatus[] statusArray = svnClient.getStatus(repoPath, true, false);
            for (ISVNStatus status : statusArray) {
                int intStatus = status.getTextStatus().toInt();
                if (SVNStatusKind.CONFLICTED.toInt() == intStatus || SVNStatusKind.OBSTRUCTED.toInt() == intStatus) {
                    Path absolutePath = Paths.get(status.getFile().getAbsolutePath());
                    errorList.add(Paths.get(repoPath.getAbsolutePath()).relativize(absolutePath).toString());
                } else if (!(autoCommit || SVNStatusKind.NONE.toInt() == intStatus ||
                        SVNStatusKind.NORMAL.toInt() == intStatus || SVNStatusKind.IGNORED.toInt() == intStatus ||
                        SVNStatusKind.EXTERNAL.toInt() == intStatus)) {
                    Path absolutePath = Paths.get(status.getFile().getAbsolutePath());
                    inconsistentStateList
                            .add(Paths.get(repoPath.getAbsolutePath()).relativize(absolutePath).toString());
                }
            }
            String[] errorArr = new String[errorList.size()];
            String[] inconsistentStateArr = new String[inconsistentStateList.size()];
            errorArr = errorList.toArray(errorArr);
            inconsistentStateArr = inconsistentStateList.toArray(inconsistentStateArr);

            deploymentSynchronizerInfo.setErrorFiles(errorArr);
            deploymentSynchronizerInfo.setInconsistentFiles(inconsistentStateArr);
            deploymentSynchronizerInfo.setWorkingCopyRevision(workingCopyRevision);
            deploymentSynchronizerInfo.setRemoteSvnRevision(remoteSvnRevision);

            deploymentSynchronizerInfo.setClientType(clientType);

        } catch (SVNClientException e) {
            log.error("Error while Server Status Reporter collecting SVN Deployment Synchronizer config." + e
                    .getMessage(), e);
        }

        return deploymentSynchronizerInfo;
    }
}
