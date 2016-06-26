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

package org.wso2.deployment.monitor.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationConfigs bean
 */
public class NotificationsConfig {

    private EmailConfig email = new EmailConfig();

    public EmailConfig getEmailConfig() {
        return email;
    }

    /**
     * todo
     */
    public static class EmailConfig {
        private boolean enabled = false;
        private boolean tlsEnabled = true;
        private boolean authentication = true;
        private String username = "";
        private String password = "";
        private String smtpServer = "";
        private int smtpPort = 25;
        private String fromAddress = "";
        private List<String> toAddresses = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isTlsEnabled() {
            return tlsEnabled;
        }

        public boolean isAuthentication() {
            return authentication;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getSmtpServer() {
            return smtpServer;
        }

        public List<String> getToAddresses() {
            return toAddresses;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public int getSmtpPort() {
            return smtpPort;
        }
    }

}
