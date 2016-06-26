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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.GlobalConfig;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

import java.util.List;

/**
 * The entry point.
 */
public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    private enum RunStrategy {
        RUN,
        SCHEDULE
    }

//    @Option(name="-run", usage="Run the specified tasks once",
//            forbids = "schedule", metaVar="task-names")
//    private String run;
//
//    @Option(name="-schedule", usage="Scheduler the specified tasks as per the trigger",
//            forbids = "run", metaVar="task-names")
//    private String schedule;
//
    /**
     *  receives other command line parameters than options
     */
    @Argument(index = 0, usage = "The run strategy", metaVar = "Run strategy")
    private RunStrategy runStrategy;

    /**
     *  receives other command line parameters than options
     */
    @Argument(index = 1, multiValued = false, usage = "testss", metaVar = "task-list")
    private String tasks ;
//    private List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {

        new Launcher().doMain(args);
    }

    private void doMain(String[] args) {
        if (System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME) == null) {
            System.setProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME, ".");
        }

        Launcher launcher = new Launcher();
        DeploymentMonitorConfiguration config = ConfigurationManager.getConfiguration();
        Monitor monitor = new Monitor();
        CmdLineParser parser = new CmdLineParser(monitor);
        try {
            parser.parseArgument(args);

            logger.info("Deployment Monitor Home {}", System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME));

            List<ServerGroup> serverGroups = config.getServerGroups();
            GlobalConfig global = config.getGlobal();
            List<TaskConfig> tasks = config.getTasks();

            launcher.mergeGlobalConfigToTaskConfig(tasks, global);
            launcher.mergeGlobalConfigToServerGroups(serverGroups, global);

            //setting general trust store params. If required tests can override these
            launcher.setKeyStoreProperties(global.getKeyStore(), global.getKeyStorePassword());
            launcher.setTrustStoreParams(global.getTrustStore(), global.getTrustStorePassword());

            monitor.cmd.execute(config);

        } catch (CmdLineException e) {
            //            parser.printSingleLineUsage(System.err);
            //            logger.error(e.getMessage());
            System.out.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }
    }

    /**
     * Merge following global parameters into TaskConfig
     *  - onResult
     *  - tenantConfig
     *
     */
    private void mergeGlobalConfigToTaskConfig(List<TaskConfig> tasks, GlobalConfig global) {
        for (TaskConfig taskConfig : tasks) {
            if (taskConfig.getOnResult().isEmpty()) {
                taskConfig.setOnResult(global.getOnResult());
            }
            taskConfig.getTaskParams().put(MonitoringConstants.DEFAULT_TENANT_KEY, global.getTenant());
        }
    }

    /**
     * Merge following global parameters into ServerGroups
     *  - trustStore
     *  - trustStorePassword
     *
     */
    private void mergeGlobalConfigToServerGroups(List<ServerGroup> serverGroups, GlobalConfig global) {
        for (ServerGroup serverGroup : serverGroups) {
            if (serverGroup.getTrustStore().isEmpty()) {
                serverGroup.setTrustStore(global.getTrustStore());
            }
            if (serverGroup.getTrustStorePassword().isEmpty()) {
                serverGroup.setTrustStorePassword(global.getTrustStorePassword());
            }
        }
    }

    private void setKeyStoreProperties(String path, String password) {
        System.setProperty("javax.net.ssl.keyStore", path);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("Security.KeyStore.Location", path);
        System.setProperty("Security.KeyStore.Password", password);
    }

    private void setTrustStoreParams(String path, String password) {
        System.setProperty("javax.net.ssl.trustStore", path);
        System.setProperty("javax.net.ssl.trustStorePassword", password);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }
}
