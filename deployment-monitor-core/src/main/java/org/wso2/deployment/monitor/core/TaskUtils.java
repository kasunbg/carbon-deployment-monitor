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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

public class TaskUtils {

    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);

    public static void callTask(String taskClassName, String callbackClassName,
            ServerGroup serverGroup, Properties customParams) {
        try {
            Class<?> taskClass = Class.forName(taskClassName);
            Method executeMethod = taskClass.getDeclaredMethod("execute", ServerGroup.class, Properties.class);

            Class<?> callbackClass = Class.forName(callbackClassName);
            Method callbackMethod = callbackClass.getDeclaredMethod("callback", RunStatus.class);

            Object taskInstance = taskClass.newInstance();
            Object callbackInstance = callbackClass.newInstance();
            RunStatus runStatus = (RunStatus) executeMethod.invoke(taskInstance, serverGroup, customParams);

            callbackMethod.invoke(callbackInstance, runStatus);

        } catch (ClassNotFoundException e) {
            logger.error("The task or callback class not found - {}", e.getMessage());
        } catch (InstantiationException | IllegalAccessException
                | NoSuchMethodException | InvocationTargetException | ExceptionInInitializerError e) {
            logger.error("Error while instantiating task classes", e);
        }

    }
}
