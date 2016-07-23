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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.command.Monitor;
import org.wso2.deployment.monitor.core.model.DeploymentMonitorConfiguration;
import org.wso2.deployment.monitor.core.model.GlobalConfig;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;
import org.wso2.deployment.monitor.service.ServiceStarter;
import org.wso2.deployment.monitor.utils.notification.email.EmailNotifications;
import org.wso2.deployment.monitor.utils.notification.sms.SMSNotifications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The entry point.
 */
public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        new Launcher().doMain(args);
    }

    private void doMain(String[] args) {
        if (System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME) == null) {
            System.setProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME, ".");
        }

        writePID(System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME));

        DeploymentMonitorConfiguration config = ConfigurationManager.getConfiguration();
        Monitor monitor = new Monitor();
        CmdLineParser parser = new CmdLineParser(monitor);
        try {
            parser.parseArgument(args);

            logger.debug("Deployment Monitor Home {}", System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME));

            List<ServerGroup> serverGroups = config.getServerGroups();
            GlobalConfig global = config.getGlobal();
            List<TaskConfig> tasks = config.getTasks();

            mergeGlobalConfigToTaskConfig(tasks, global);
            mergeGlobalConfigToServerGroups(serverGroups, global);

            //setting general trust store params. If required tests can override these
            setKeyStoreProperties(global.getKeyStore(), global.getKeyStorePassword());
            setTrustStoreParams(global.getTrustStore(), global.getTrustStorePassword());

            monitor.cmd.execute(config);
            ServiceStarter.startService(config.getServiceConfig());

            //Initializing the Email and SMS Senders
            logger.info("Initializing Email and SMS notifications");
            EmailNotifications.getInstance();
            SMSNotifications.getInstance();

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    /**
     * Write the process ID of this process to the file.
     *
     * @param deploymentMonitorHome deployment.monitor.home sys property value.
     */
    private static void writePID(String deploymentMonitorHome) {

        String[] cmd = { "bash", "-c", "echo $PPID" };
        Process p;
        String pid = "";
        try {
            p = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            //Ignored. We might be invoking this on a Window platform. Therefore if an error occurs
            //we simply ignore the error.
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            pid = builder.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        if (pid.length() != 0) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(Paths.get(deploymentMonitorHome, "carbon.pid").toString()),
                    StandardCharsets.UTF_8))) {
                writer.write(pid);
            } catch (IOException e) {
                logger.warn("Cannot write carbon.pid file");
            }
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
        Path finalPath = sanitizePath(path);

        System.setProperty("javax.net.ssl.keyStore", finalPath.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", password);
    }


    private void setTrustStoreParams(String path, String password) {
        Path finalPath = sanitizePath(path);

        System.setProperty("javax.net.ssl.trustStore", finalPath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", password);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    private Path sanitizePath(String originalPath) {
        Path finalPath = Paths.get(originalPath);
        if (Files.exists(finalPath)) {
            return finalPath;
        }
        if(!Paths.get(originalPath).isAbsolute()) {
            finalPath = Paths.get(TaskUtils.getDeploymentMonitorHome(), originalPath);
            if (Files.exists(finalPath)) {
                return finalPath;
            }
        }

        throw new IllegalArgumentException("Path not found " + originalPath);
    }

}
