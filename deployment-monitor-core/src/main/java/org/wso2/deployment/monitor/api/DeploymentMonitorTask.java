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
package org.wso2.deployment.monitor.api;

import java.util.List;
import java.util.Properties;

/**
 * This interface should be implemented to register a test instance.
 *
 */
public interface DeploymentMonitorTask {

    /**
     * This method gets executed each time the scheduler is run.
     * A new instance of the implemented classes will be created for each
     * schedule run.
     *
     * @param serverGroupList List of server groups.
     * @param customParams custom parameters defined in the job configuration
     * @return true or false depending on the test success status
     */
    RunStatus execute(List<ServerGroup> serverGroupList, Properties customParams);

}
