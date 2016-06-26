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
import org.wso2.deployment.monitor.core.scheduler.ScheduleCommand;

import java.util.List;

public class Monitor {

    @Argument(index = 0, metaVar = "<sub-command>", usage = "Test strategy - Run once or schedule periodically.",
              required = false, handler = SubCommandHandler.class)
    @SubCommands({
                         @SubCommand(name = "run", impl = RunCommand.class),
                         @SubCommand(name = "schedule", impl = ScheduleCommand.class),
                         @SubCommand(name = "list", impl = ListCommand.class)
                 })
    public Command cmd = new DefaultCommand();

    private static class DefaultCommand extends ScheduleCommand {

        DefaultCommand() {
            List<String> taskNamesToRun = getTaskNamesToRun();
            taskNamesToRun.clear();
            taskNamesToRun.add("*");
        }
    }

}
