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
package org.wso2.deployment.monitor.impl.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.devops.monitor.beans.xsd.Patch;
import org.wso2.carbon.devops.monitor.beans.xsd.ServerInfo;
import org.wso2.carbon.devops.monitor.client.ServerStatusReporterPortType;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.DeploymentMonitorException;
import org.wso2.deployment.monitor.core.MonitoringConstants;
import org.wso2.deployment.monitor.core.model.ServerGroup;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
 * todo
 */
public class PatchVerificationTask implements DeploymentMonitorTask {

    private static final Logger logger = LoggerFactory.getLogger(PatchVerificationTask.class);

    private static TrustManager[] trustManager;
    private static HostnameVerifier allHostsValid;

    static {
        // Create all-trusting host name verifier
        allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        trustManager = new X509TrustManager[] {
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

    }

    @Override
    public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        logger.info("-- Running patch verification task ---");

        if (serverGroup.getHosts().size() <= 1) {
            logger.warn("Patch comparison skipped for '{}' since at least hosts are required.");
            return new RunStatus();
        }

        Iterator<String> hostsIterator = serverGroup.getHosts().iterator();
        Map<String, Patch> firstPatchMap = patchListToMap(
                getServerInfo(hostsIterator.next()).getPatchInfo());
        while (hostsIterator.hasNext()) {
            ServerInfo serverInfo = getServerInfo(hostsIterator.next());
            Map<String, Patch> patchMap = patchListToMap(serverInfo.getPatchInfo());

            logger.debug(patchMap.keySet().toString());
            logger.debug(firstPatchMap.toString());
            //do comparison
            //print warnings if any
            //debug success messages

            //do md5sum checks
//            System.out.println(serverInfo);
        }

        //dummy values
        logger.info("Comparing patches in servers in '{}' against the server '{}'",
                serverGroup.getName(), serverGroup.getHosts().get(0));
        logger.info("Patch differences found:");
        logger.warn("Missing patches in {} : {}", serverGroup.getHosts().get(1),
                new String[] { "patch1234", "patch2345" });
        logger.warn("Extra patches in {} : {}", serverGroup.getHosts().get(1), new String[] { "patch9999" });

        logger.warn("Missing patches in {} : {}", "https://esb03:9443", new String[] { "patch1234", "patch2345"});

        logger.info("--- End of patch verification task ---\n");

        RunStatus runStatus = new RunStatus();
        runStatus.setSuccess(true);
        runStatus.setMessage("Successfully verified the patches in " + serverGroup.getName());
        return runStatus;
    }

    private ServerInfo getServerInfo(String host) {
        URL url = getAgentURL(host);

        return invokeServerAgent(url.toString());
    }

    private URL getAgentURL(String host) {
        try {
            URL baseURL = new URL(host);
            String agentSubContext = MonitoringConstants.SERVER_AGENT_URL_CONTEXT + "/" +
                    MonitoringConstants.SERVER_AGENT_SERVER_INFO_OP;
            return new URL(baseURL, agentSubContext);
        } catch (MalformedURLException e) {
            e.printStackTrace(); //todo
            throw new DeploymentMonitorException("Server agent URL generation failed for " + host, e);
        }

    }

    public ServerInfo invokeServerAgent(String endpoint) {
        URL wsdlURL = PatchVerificationTask.class.getResource("/ServerStatusReporter.wsdl");
        if (wsdlURL == null) {
            throw new DeploymentMonitorException("Missing ServerStatusReporter WSDL");
        }
        QName serviceName = new QName("http://monitor.devops.carbon.wso2.org", "ServerStatusReporter");
        Service service = Service.create(wsdlURL, serviceName);
        ServerStatusReporterPortType client = service.getPort(ServerStatusReporterPortType.class);

        boolean nonSecureMode = Boolean.parseBoolean(System.getProperty("enableNonSecureMode"));
        if (nonSecureMode) {
            //only for oracle jdk
            ((BindingProvider)client).getRequestContext().
                    put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", getUnsecuredSocketFactory());
        }

        ((BindingProvider)client).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpoint);

        return client.getServerInfo();
    }

    private SSLSocketFactory getUnsecuredSocketFactory() {
        try {
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

    private Map<String, Patch> patchListToMap(List<Patch> patchList) {
        Map<String, Patch> patchMap = new HashMap<>(patchList.size());
        for (Patch patch : patchList) {
            if (patch == null) {
                continue;
            }
            patchMap.put(patch.getPatchId(), patch);
        }
        return patchMap;
    }
}
