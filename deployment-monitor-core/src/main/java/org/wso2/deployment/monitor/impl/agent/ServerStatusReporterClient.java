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
package org.wso2.deployment.monitor.impl.agent;

import org.wso2.carbon.devops.monitor.beans.xsd.DeploymentSynchronizerInfo;
import org.wso2.carbon.devops.monitor.beans.xsd.Patch;
import org.wso2.carbon.devops.monitor.beans.xsd.ServerInfo;
import org.wso2.carbon.devops.monitor.client.ServerStatusReporterPortType;
import org.wso2.deployment.monitor.core.DeploymentMonitorException;
import org.wso2.deployment.monitor.core.MonitoringConstants;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

/**
 * A service client for ServerStatusReporter agent
 *
 * todo handle connection refused gracefully
 */
public class ServerStatusReporterClient {

    private static final Service serverStatusReporter;

    static {
        URL wsdlURL = ServerStatusReporterClient.class.getResource("/ServerStatusReporter.wsdl");
        QName serviceName = new QName(MonitoringConstants.SERVER_AGENT_NAMESPACE,
                MonitoringConstants.SERVER_AGENT_NAME);
        serverStatusReporter = Service.create(wsdlURL, serviceName);
        ServerStatusReporterPortType client = serverStatusReporter.getPort(ServerStatusReporterPortType.class);

        boolean nonSecureMode = Boolean.parseBoolean(System.getProperty("enableNonSecureMode"));
        if (nonSecureMode) {
            //only for oracle jdk
            ((BindingProvider) client).getRequestContext().
                    put(MonitoringConstants.JAXWS_SSL_SOCKETFACTORY, getUnsecuredSocketFactory());
        }
    }

    /**
     *
     * Returns all the product information of the server including patch, depsync, dropins info.
     *
     * @param host host in the format - <code>https://<hostname:port>/</code>
     * @return server info bean
     */
    public ServerInfo getServerInfo(String host) {
        String endpoint = getEndpoint(host);
        ServerStatusReporterPortType client = serverStatusReporter.getPort(ServerStatusReporterPortType.class);
        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        return client.getServerInfo();
    }

    /**
     * @param host host in the format - <code>https://<hostname:port>/</code>
     * @return patch info
     */
    public List<Patch> getPatchInfo(String host) {
        String endpoint = getEndpoint(host);
        ServerStatusReporterPortType client = serverStatusReporter.getPort(ServerStatusReporterPortType.class);
        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        return client.getPatchInfo();
    }

    /**
     *
     * @param host host in the format - <code>https://<hostname:port>/</code>
     * @return depsync info bean
     */
    public DeploymentSynchronizerInfo getDeploymentSynchronizerInfo(String host) {
        String endpoint = getEndpoint(host);
        ServerStatusReporterPortType client = serverStatusReporter.getPort(ServerStatusReporterPortType.class);
        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        return client.getDeploymentSynchronizerInfo();
    }

    private static String getEndpoint(String host) {
        URL url;
        try {
            URL baseURL = new URL(host);
            String agentSubContext = MonitoringConstants.SERVER_AGENT_URL_CONTEXT + "/" +
                    MonitoringConstants.SERVER_AGENT_SERVER_INFO_OP;
            url = new URL(baseURL, agentSubContext);

            if (!"https".equals(url.getProtocol())) {
                throw new DeploymentMonitorException(
                        "The server agent is only available over https. " + "Failed URL: " + url);
            }
            return url.toString();
        } catch (MalformedURLException e) {
            throw new DeploymentMonitorException("Server agent URL generation failed for " + host, e);
        }

    }

    private static SSLSocketFactory getUnsecuredSocketFactory() {
        try {
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            TrustManager[] trustManager = new X509TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManager, new SecureRandom());

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            return context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace(); //todo
        }

        return null;
    }

}
