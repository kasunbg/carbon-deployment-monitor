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
package org.wso2.carbon.devops.monitor.internal;

/**
 * bean //todo
 */
public class DeploymentSynchronizerInfo {

    private boolean enabled;
    private boolean autoCommit;
    private boolean autoCheckout;

    /**
     * For svn, there is svnkit and cmdline clients.
     * svnkit is recommended.
     */
    private String clientType;

    private long workingCopyRevision;
    private long remoteSvnRevision;

    private String[] errorFiles = new String[0];
    private String[] inconsistentFiles = new String[0];

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public boolean isAutoCheckout() {
        return autoCheckout;
    }

    public void setAutoCheckout(boolean autoCheckout) {
        this.autoCheckout = autoCheckout;
    }

    public String[] getErrorFiles() {
        return errorFiles;
    }

    public void setErrorFiles(String[] errorFiles) {
        this.errorFiles = errorFiles;
    }

    public String[] getInconsistentFiles() {
        return inconsistentFiles;
    }

    public void setInconsistentFiles(String[] inconsistentFiles) {
        this.inconsistentFiles = inconsistentFiles;
    }

    public long getWorkingCopyRevision() {
        return workingCopyRevision;
    }

    public void setWorkingCopyRevision(long workingCopyRevision) {
        this.workingCopyRevision = workingCopyRevision;
    }

    public long getRemoteSvnRevision() {
        return remoteSvnRevision;
    }

    public void setRemoteSvnRevision(long remoteSvnRevision) {
        this.remoteSvnRevision = remoteSvnRevision;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getClientType() {
        return clientType;
    }
}
