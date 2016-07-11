/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.deployment.monitor.scheduling.service;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.model.ServerGroup;

import java.io.File;
import java.util.Properties;

/**
 * This is a Task which exposes the scheduling methods
 */
public class SchedulingServiceTask implements DeploymentMonitorTask {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingServiceTask.class);
    private static final String PORT = "port";

    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        int port = ((int) customParams.get(PORT));
        doMain(port);
        return new RunStatus();
    }

    private void doMain(int port) {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        Context ctx = tomcat.addContext("/", new File(".").getAbsolutePath());
        Tomcat.addServlet(ctx, "manage-server", new ManageServerServlet());
        ctx.addServletMapping("/manage-server", "manage-server");
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            logger.error("Error occurred while starting the Scheduling Service", e);
        }
        tomcat.getServer().await();
    }

}
