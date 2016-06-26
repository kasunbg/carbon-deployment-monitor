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
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.util.ArrayList;
import java.util.List;

public class CmdOptions {

    @Argument(index = 0, metaVar = "<sub-command>", usage = "Test strategy - Run once or schedule periodically.",
              required = false, handler = SubCommandHandler.class)
    @SubCommands({
                         @SubCommand(name = "run", impl = Run.class),
                         @SubCommand(name = "schedule", impl = Schedule.class)
                 })
    private Command cmd = new Schedule() {
        public List<String> getTaskNames() {
            List<String> defaultTasks = new ArrayList<>();
            defaultTasks.add("*");
            return defaultTasks;
        }

    };

    public Command getCmd() {
        return cmd;
    }

    public abstract static class Command {
        /**
         * The list of task names to run/schedule.
         * Added into the parent class since this is common for both Run and Schedule.
         */
        @Argument(index = 0, metaVar = "<task-names>", usage = "list of task names. Specify * for all.",
                  required = true, multiValued = true)
        private List<String> taskNames = new ArrayList<>();

        public List<String> getTaskNames() {
            return taskNames;
        }
    }

    public static class Run extends Command { }

    public static class Schedule extends Command { }

}
