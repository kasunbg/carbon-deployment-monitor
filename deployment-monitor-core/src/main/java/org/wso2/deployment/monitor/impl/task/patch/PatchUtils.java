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

import org.wso2.carbon.devops.monitor.beans.xsd.Patch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * todo
 */
public class PatchUtils {

    /**
     * todo
     */
    public static PatchDiffBean compare(Map<String, Patch> first, Map<String, Patch> second) {
        Set<String> firstKeySet = new HashSet<>(first.keySet());
        Set<String> secondKeySet = new HashSet<>(second.keySet());

        firstKeySet.removeAll(second.keySet()); //missing in second list
        secondKeySet.removeAll(first.keySet()); //extras in second list

        PatchDiffBean patchDiffBean = new PatchDiffBean();
        patchDiffBean.setMissingPatches(firstKeySet);
        patchDiffBean.setExtraPatches(secondKeySet);

        return patchDiffBean;
    }

}
