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

import java.util.Set;

public class PatchDiffBean {

    private Set<String> missingPatches;
    private Set<String> extraPatches;

    public Set<String> getMissingPatches() {
        return missingPatches;
    }

    public void setMissingPatches(Set<String> missingPatches) {
        this.missingPatches = missingPatches;
    }

    public Set<String> getExtraPatches() {
        return extraPatches;
    }

    public void setExtraPatches(Set<String> extraPatches) {
        this.extraPatches = extraPatches;
    }
}
