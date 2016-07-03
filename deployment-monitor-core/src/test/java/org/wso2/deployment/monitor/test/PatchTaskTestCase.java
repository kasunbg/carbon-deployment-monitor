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
package org.wso2.deployment.monitor.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.devops.monitor.beans.xsd.Patch;
import org.wso2.deployment.monitor.impl.task.patch.PatchDiffBean;
import org.wso2.deployment.monitor.impl.task.patch.PatchUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PatchTaskTestCase {

    @Test
    public void testPatchUtils() {
        Map<String, Patch> first = new HashMap<>();
        Map<String, Patch> second = new HashMap<>();

        Patch patch = new Patch();
        first.put("patch0100", patch);
        first.put("patch0110", patch);
        first.put("patch0120", patch);
        first.put("patch0300", patch);

        second.put("patch0100", patch);
        second.put("patch0110", patch);
        second.put("patch0120", patch);
        second.put("patch0400", patch);
        second.put("patch0500", patch);

        PatchDiffBean bean1 = PatchUtils.compare(first, second);

        Set<String> missingPatches1 = bean1.getMissingPatches();
        Set<String> extraPatches1 = bean1.getExtraPatches();
        Assert.assertTrue(missingPatches1.contains("patch0300"));
        Assert.assertTrue(extraPatches1.contains("patch0400"));
        Assert.assertTrue(extraPatches1.contains("patch0500"));
    }
}
