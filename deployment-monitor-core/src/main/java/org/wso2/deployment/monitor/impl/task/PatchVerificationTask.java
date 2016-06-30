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
        logger.info("--- Running patch verification task ---");

        if (serverGroup.getHosts().size() <= 1) {
            logger.warn("Patch comparison skipped for '{}' since at least two hosts are required.",
                    serverGroup.getName());
            return new RunStatus();
        }

        boolean isSuccess = true;
        Iterator<String> hostsIterator = serverGroup.getHosts().iterator();
        Map<String, Patch> firstPatchMap = patchListToMap(getServerInfo(hostsIterator.next()).getPatchInfo());
        logger.info("Comparing patches in servers in '{}' against the server '{}'", serverGroup.getName(),
                serverGroup.getHosts().get(0));
        logger.info("Patches in {}: {}", serverGroup.getHosts().get(0), firstPatchMap.keySet());
        while (hostsIterator.hasNext()) {
            String host = hostsIterator.next();
            ServerInfo serverInfo = getServerInfo(host);
            Map<String, Patch> patchMap = patchListToMap(serverInfo.getPatchInfo());
            PatchDiffBean patchDiffBean = PatchUtils.compare(firstPatchMap, patchMap);

            if (!patchDiffBean.getMissingPatches().isEmpty()) {
                logger.warn("Missing patches in {} : {}", host, patchDiffBean.getMissingPatches());
                isSuccess = false;
            }
            if (!patchDiffBean.getExtraPatches().isEmpty()) {
                logger.warn("Extra patches in {} : {}", host, patchDiffBean.getExtraPatches());
                isSuccess = false;
            }

            //do md5sum checks
        }

        logger.info("--- End of patch verification task ---\n");

        RunStatus runStatus = new RunStatus();
        runStatus.setSuccess(isSuccess);
        runStatus.setMessage("Verified the patches in " + serverGroup.getName());
        return runStatus;
    }

    /**
     * Invoke the server agent and get the server info
     *
     * @param host the host - ex. https://localhost:9443/
     * @return The server info bean
     */
    private ServerInfo getServerInfo(String host) {
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
        } catch (MalformedURLException e) {
            throw new DeploymentMonitorException("Server agent URL generation failed for " + host, e);
        }

        URL wsdlURL = PatchVerificationTask.class.getResource("/ServerStatusReporter.wsdl");
        QName serviceName = new QName(MonitoringConstants.SERVER_AGENT_NAMESPACE,
                MonitoringConstants.SERVER_AGENT_NAME);
        Service service = Service.create(wsdlURL, serviceName);
        ServerStatusReporterPortType client = service.getPort(ServerStatusReporterPortType.class);

        boolean nonSecureMode = Boolean.parseBoolean(System.getProperty("enableNonSecureMode"));
        if (nonSecureMode) {
            //only for oracle jdk
            ((BindingProvider) client).getRequestContext().
                    put(MonitoringConstants.JAXWS_SSL_SOCKETFACTORY, getUnsecuredSocketFactory());
        }

        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url.toString());

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
