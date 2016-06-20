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
package org.wso2.deployment.monitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * The entry point.
 */
public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        //argument parsing
        //config parsing
        //invoke quartz

        if (System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME) == null) {
            System.setProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME, ".");
        }
        logger.info("Deployment Monitor Home {}", System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME));

        Launcher launcher = new Launcher();
        Map config = launcher.getMonitorConfig();

        List<Map> serverGroups = (List<Map>) config.get(LauncherConstants.SERVER_GROUP_CONFIG);
        Map<String, String> globalConfig = (Map<String, String>) config.get(LauncherConstants.GLOBAL_CONFIG);
        List<Map> tests = (List<Map>) config.get(LauncherConstants.TEST_GROUP_CONFIG);

        //call schedule manager
        for (Map test : tests) {
//            String clazz = (String) test.get("class");
//            clazz = clazz.trim();

            test.clear();
            serverGroups.clear();
            globalConfig.clear();
        }


//        config.get("server_group")

    }

    private Map getMonitorConfig() {
        Path monitorConf = Paths.get(System.getProperty(LauncherConstants.DEPLOYMENT_MONITOR_HOME), "conf",
                LauncherConstants.DEPLOYMENT_MONITOR_CONFIG_FILE);

        try (InputStream confInputStream = Files.newInputStream(monitorConf)) {
            Yaml yaml = new Yaml();
            Map confMap = (Map) yaml.load(confInputStream);

            if (logger.isDebugEnabled()) {
                logger.debug("configuration map = {}", confMap);
            }

            return confMap;

        } catch (IOException e) {
            throw new DeploymentMonitorException(e.getMessage(), e);
        }

    }
}
