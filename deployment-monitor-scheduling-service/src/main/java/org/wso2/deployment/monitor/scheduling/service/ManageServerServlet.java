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

import org.apache.commons.httpclient.HttpStatus;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.scheduler.ScheduleManager;
import org.wso2.deployment.monitor.scheduling.service.utils.CharacterEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Manage Servlet
 * todo Do we need HTTPS.? Auth.? etc
 */
public class ManageServerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ManageServerServlet.class);

    private static final long serialVersionUID = 9175142573301887173L;

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ScheduleManager scheduleManager;
        boolean isSuccess = false;
        String responseMsg = null;

        String action = CharacterEncoder.getSafeText(req.getParameter("action"));
        String taskName = CharacterEncoder.getSafeText(req.getParameter("task"));
        String serverName = CharacterEncoder.getSafeText(req.getParameter("server"));

        //Checking the mandatory parameters
        if (action == null) {
            logger.warn("Action has not been specified.");
            responseMsg = "Action name is not specified. "
                    + "Please specify a action { schedule | unschedule | pause | resume }";
            resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println(responseMsg);
            return;
        }

        if (serverName == null) {
            logger.warn("Server name has not been specified");
            responseMsg = "Please specify the server";
            resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println(responseMsg);
            return;
        }

        logger.info("Action : " + action + ", Task : " + taskName + ", Server : " + serverName);

        try {
            scheduleManager = ScheduleManager.getInstance();
            //by task and server both
            if (taskName != null) {
                switch (action) {

                case "schedule":
                    isSuccess = scheduleManager.scheduleTaskForServer(taskName, serverName);
                    break;
                case "unschedule":
                    isSuccess = scheduleManager.unScheduleTaskForServer(taskName, serverName);
                    break;
                case "pause":
                    isSuccess = scheduleManager.pauseTaskForServer(taskName, serverName);
                    break;
                case "resume":
                    isSuccess = scheduleManager.resumeTaskForServer(taskName, serverName);
                    break;

                default:
                    logger.warn("Incorrect Action has been specified");
                    responseMsg = "Incorrect Action has been specified. "
                            + "Please specify a action { schedule | unschedule | pause | resume }";
                    break;
                }
                //only by server
            } else {
                switch (action) {

                case "schedule":
                    isSuccess = scheduleManager.scheduleTasksOfServer(serverName);
                    break;
                case "unschedule":
                    isSuccess = scheduleManager.unScheduleTasksOfServer(serverName);
                    break;
                case "pause":
                    isSuccess = scheduleManager.pauseTasksOfServer(serverName);
                    break;
                case "resume":
                    isSuccess = scheduleManager.resumeTasksOfServer(serverName);
                    break;

                default:
                    logger.warn("Incorrect Action has been specified");
                    responseMsg = "Incorrect Action has been specified."
                            + " Please specify a action { schedule | unschedule | pause | resume }";
                    break;
                }
            }

        } catch (SchedulerException e) {
            logger.error("Error occurred while running the Scheduling Service {}", e);
            responseMsg = "Error occurred while running the Scheduling Service" ;
        }

        if (isSuccess) {
            resp.setStatus(HttpStatus.SC_OK);
        } else {
            resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println(responseMsg);
        }
    }
}
