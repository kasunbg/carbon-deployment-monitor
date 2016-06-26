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
package org.wso2.deployment.monitor.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * todo
 */
public class TaskConfig {

    private boolean enable = true;
    private String name = "taskName";
    private String group = "taskGroup";
    private String className = "";
    private String triggerType = "simple";
    private String trigger = "60m";
    private String onResult = "";
    private List<String> servers = new ArrayList<>();
    private Properties taskParams = new Properties();

    public boolean isEnable() {
        return enable;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getClassName() {
        return className;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getOnResult() {
        return onResult;
    }

    public void setOnResult(String onResult) {
        this.onResult = onResult;
    }

    public List<String> getServers() {
        return servers;
    }

    public Properties getTaskParams() {
        return taskParams;
    }

    @Override
    public boolean equals(Object obj) {
       return true;
    }

}
