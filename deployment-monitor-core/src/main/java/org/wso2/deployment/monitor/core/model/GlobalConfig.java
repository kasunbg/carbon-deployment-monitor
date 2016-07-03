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
package org.wso2.deployment.monitor.core.model;


/**
 * todo
 */
public class GlobalConfig {

    private String onResult = "";
    private String keyStore = "wso2carbon.jks";
    private String keyStorePassword = "wso2carbon";
    private String trustStore = "client-truststore.jks";
    private String trustStorePassword = "wso2carbon";
    private TenantConfig tenant = new TenantConfig();

    public String getOnResult() {
        return onResult;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public TenantConfig getTenant() {
        return tenant;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    /**
     * todo
     */
    public static class TenantConfig {
        private String domain = "carbon.super";
        private String tenantID = "-1234";
        private String username = "admin";
        private String password = "admin";

        public String getDomain() {
            return domain;
        }

        public String getTenantID() {
            return tenantID;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

}
