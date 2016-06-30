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
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.core.model.TaskConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TaskUtils {

    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);

    /**
     * Call a given task.
     * todo handle failure scenarios.
     */
    public static void callTask(String taskName, String taskClassName, String callbackClassName,
            ServerGroup serverGroup, Properties customParams) {
        try {
            Class<?> taskClass = Class.forName(taskClassName);
            Method executeMethod = taskClass.getDeclaredMethod("execute", ServerGroup.class, Properties.class);

            Class<?> callbackClass = Class.forName(callbackClassName);
            Method callbackMethod = callbackClass.getDeclaredMethod("callback", RunStatus.class);

            Object taskInstance = taskClass.newInstance();
            Object callbackInstance = callbackClass.newInstance();
            RunStatus runStatus = (RunStatus) executeMethod.invoke(taskInstance, serverGroup, customParams);
            runStatus.setTaskName(taskName);
            runStatus.setServerGroupName(serverGroup.getName());

            callbackMethod.invoke(callbackInstance, runStatus);
        } catch (ClassNotFoundException e) {
            logger.error("The task or callback class not found - {}", e.getMessage());
        } catch (InstantiationException | IllegalAccessException
                | NoSuchMethodException | InvocationTargetException | ExceptionInInitializerError e) {
            logger.error("Error while instantiating task classes", e);
        }
    }

    /**
     * Returns a filtered list of TaskConfig that contain the provided list of task names.
     *
     */
    public static List<TaskConfig> filterTasksByName(List<TaskConfig> allTasks, List<String> taskNamesFilter) {
        List<TaskConfig> filteredTasks = new ArrayList<>(taskNamesFilter.size());
        if (taskNamesFilter.size() == 1 && taskNamesFilter.contains("*")) {
            filteredTasks = allTasks;
            taskNamesFilter.remove(0);
        } else {
            for (TaskConfig taskConfig : allTasks) {
                Iterator<String> it = taskNamesFilter.iterator();
                while (it.hasNext()) {
                    if (it.next().equals(taskConfig.getName())) {
                        filteredTasks.add(taskConfig);
                        it.remove();
                        break;
                    }
                }
            }
        }

        if (!taskNamesFilter.isEmpty()) {
            logger.warn("task name(s) {} were not found in {}",
                    taskNamesFilter, MonitoringConstants.DEPLOYMENT_MONITOR_CONFIG_FILE);
        }

        return filteredTasks;
    }

    /**
     * Returns a Map of the server groups of the given task
     * @param taskConfig {@link TaskConfig}
     * @return {@link Map}
     */
    public static Map<String, ServerGroup> getServerGroupsByTaskConfig(TaskConfig taskConfig) {
        Map<String, ServerGroup> serverGroupMap = new Hashtable<>();

        for (ServerGroup serverGroup : ConfigurationManager.getConfiguration().getServerGroups()) {
            serverGroupMap.put(serverGroup.getName(), serverGroup);
        }

        if (taskConfig.getServers().size() == 1 && ("ALL".equalsIgnoreCase(taskConfig.getServers().get(0)))) {
            logger.debug("Scheduling the task " + taskConfig.getName() + " for all the server groups");
            return serverGroupMap;
        } else {
            Map<String, ServerGroup> tempMap = new Hashtable<>();
            for (String server : taskConfig.getServers()) {
                if (!serverGroupMap.containsKey(server)) {
                    logger.warn("Unable to find a Server Group with the name : " + server
                            + ". Task will not be scheduled for this server");
                } else {
                    tempMap.put(server, serverGroupMap.get(server));
                }
            }
            return tempMap;
        }
    }

    /**
     * Returns a Map of the server groups of the Task with the given name
     * @param name Name of the task
     * @return {@link Map}
     */
    public static Map<String, ServerGroup> getServerGroupsByTaskName(String name) {
        TaskConfig taskConfig = getTaskConfigByName(name);
        if(taskConfig != null){
            return getServerGroupsByTaskConfig(taskConfig);
        } else {
            return new Hashtable<>();
        }
    }

    /**
     * Returns a {@link TaskConfig} with the given name
     * @param name Task Name
     * @return {@link TaskConfig}
     */
    public static TaskConfig getTaskConfigByName(String name){
        List<TaskConfig> taskConfigs = ConfigurationManager.getConfiguration().getTasks();
        for (TaskConfig taskConfig : taskConfigs) {
            if (taskConfig.getName().equals(name)){
                return taskConfig;
            }
        }
        return null;
    }

    public static String getDeploymentMonitorHome() {
        String home = System.getenv(MonitoringConstants.DEPLOYMENT_MONITOR_HOME);
        if (home == null) {
            home = System.getProperty(MonitoringConstants.DEPLOYMENT_MONITOR_HOME);
        }

        return home;
    }
}
