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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.deployment.monitor.api.RunStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

public class QuartzJobProxy implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        try {
            String taskClassName = dataMap.getString("taskClass");
            String callbackClassName = dataMap.getString("callbackClass");
            Object serverList = dataMap.get("serverGroupList");
            Object customParams = dataMap.get("customParams");

            Class taskClass = Class.forName(taskClassName);
            Method executeMethod = taskClass.getDeclaredMethod("execute", List.class, Properties.class);

            Class callbackClass = Class.forName(callbackClassName);
            Method callbackMethod = callbackClass.getDeclaredMethod("callback", RunStatus.class);


            Object taskInstance = taskClass.newInstance();
            Object callbackInstance = callbackClass.newInstance();
            RunStatus runStatus = (RunStatus) executeMethod.invoke(taskInstance, serverList, customParams);

            callbackMethod.invoke(callbackInstance, runStatus);

        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //todo
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch(ExceptionInInitializerError le) {
            System.err.printf("Seems like a bad class to this JVM: “%s”.", "classname"); //todo
            le.printStackTrace();
        }

    }
}
