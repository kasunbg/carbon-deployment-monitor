/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.deployment.monitor.scheduler.utils;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Utilities for Quartz triggers implemented in this class
 */
public class TriggerUtilities {

    private static final Logger logger = LoggerFactory.getLogger(TriggerUtilities.class);

    /**
     * Enum for trigger types
     */
    private enum TriggerType {
        CRON, SIMPLE, NONE
    }

    /**
     * Trigger types in underscore case format
     */
    private static final String CRON_TRIGGER = "cron";
    private static final String SIMPLE_TRIGGER = "simple";

    private static final String TRIGGER_TYPE = "triggerType";

    /**
     * for simple trigger specifications
     */
    public static final String MINUTES = "m";
    public static final String HOURS = "h";
    public static final String SECONDS = "s";

    /**
     * Returns trigger type of a particular Task
     *
     * @return Quartz trigger type
     */
    public static TriggerType getTriggerType(Map testGroup) {
        String triggerType = (String) testGroup.get(TRIGGER_TYPE);

        if (SIMPLE_TRIGGER.equals(triggerType)) {
            return TriggerType.CRON;
        } else if (CRON_TRIGGER.equals(triggerType)) {
            return TriggerType.SIMPLE;
        } else {
            logger.warn("Error while getting trigger type");
            return TriggerType.NONE;
        }
    }

    /**
     * Checks validity of a Cron expression
     *
     * @param expression Cron expression
     * @return True if the Cron expression is valid
     */
    public static boolean isValidCronExpression(String expression) {
        return CronExpression.isValidExpression(expression);
    }

    /**
     * Checks validity of a Simple trigger expression
     *
     * @param expression Simple trigger expression
     * @return True if the simple trigger expression is valid
     */
    public static boolean isValidSimpleTriggerExpression(String expression) {
        if (expression.endsWith(HOURS)) {
            return isContainsInt(expression.split(HOURS)[0]);
        } else if (expression.endsWith(MINUTES)) {
            return isContainsInt(expression.split(MINUTES)[0]);
        } else {
            return isContainsInt(expression.split(SECONDS)[0]);
        }
    }

    /**
     * Checks the expression contains an integer
     *
     * @param expression Simple trigger expression after removing hour/minute tag
     * @return True if the expression contains an integer
     */
    private static boolean isContainsInt(String expression) {
        try {
            Integer.parseInt(expression.replace(" ", ""));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
