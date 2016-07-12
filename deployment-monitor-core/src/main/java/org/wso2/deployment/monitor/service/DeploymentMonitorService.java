package org.wso2.deployment.monitor.service;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.scheduler.ScheduleManager;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/deployment-monitor") public class DeploymentMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentMonitorService.class);

    @POST @Path("/scheduling") public Response scheduling(@QueryParam("action") String action,
            @QueryParam("task") String task, @QueryParam("server") String server) {
        ScheduleManager scheduleManager;
        boolean isSuccess = false;
        String responseMsg = null;

        //Checking the mandatory parameters
        if (action == null) {
            logger.warn("Action has not been specified.");
            responseMsg = "Action name is not specified. "
                    + "Please specify a action { schedule | unschedule | pause | resume }";
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseMsg).build();
        }

        if (server == null) {
            logger.warn("Server name has not been specified");
            responseMsg = "Please specify the server";
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseMsg).build();
        }

        logger.info("Action : " + action + ", Task : " + task + ", Server : " + server);

        try {
            scheduleManager = ScheduleManager.getInstance();
            //by task and server both
            if (task != null) {
                switch (action) {

                case "schedule":
                    isSuccess = scheduleManager.scheduleTaskForServer(task, server);
                    break;
                case "unschedule":
                    isSuccess = scheduleManager.unScheduleTaskForServer(task, server);
                    break;
                case "pause":
                    isSuccess = scheduleManager.pauseTaskForServer(task, server);
                    break;
                case "resume":
                    isSuccess = scheduleManager.resumeTaskForServer(task, server);
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
                    isSuccess = scheduleManager.scheduleTasksOfServer(server);
                    break;
                case "unschedule":
                    isSuccess = scheduleManager.unScheduleTasksOfServer(server);
                    break;
                case "pause":
                    isSuccess = scheduleManager.pauseTasksOfServer(server);
                    break;
                case "resume":
                    isSuccess = scheduleManager.resumeTasksOfServer(server);
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
            responseMsg = "Error occurred while running the Scheduling Service";
        }

        if (isSuccess) {
            return Response.status(Response.Status.OK).entity("Hello from WSO2 MSF4J").build();
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseMsg).build();
        }
    }
}
