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
package org.wso2.carbon.devops.client.jasperreports;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.devops.client.DeploymentVerifierException;
import org.wso2.carbon.devops.monitor.beans.xsd.Patch;
import org.wso2.carbon.devops.monitor.beans.xsd.ServerInfo;
import org.wso2.carbon.devops.monitor.client.ServerStatusReporterPortType;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

public class JasperUtils {

    private static final Logger log = LoggerFactory.getLogger(JasperUtils.class);

    public static JRDataSource getDataSource(List<ServerInfo> serverInfoList) {

        List<JasperServerInfoBean> jasperBeanList = new ArrayList<>(serverInfoList.size());
        jasperBeanList.add(null); //todo
        for (ServerInfo serverInfo : serverInfoList) {
            String product = String
                    .format("%s %s (profile = %s)", serverInfo.getProductName(), serverInfo.getProductVersion(),
                            serverInfo.getServerProfile());
            String url = serverInfo.getServerURL();
            String ip = serverInfo.getServerIP();
            String patches = getPatches(serverInfo.getPatchInfo());

            JasperServerInfoBean jasperBean = new JasperServerInfoBean();
            jasperBean.setProduct(product);
            jasperBean.setServerURL(url);
            jasperBean.setServerIP(ip);
            jasperBean.setPatches(patches);

            jasperBeanList.add(jasperBean);

            log.info("Server info received - " + jasperBean); //todo
            if (log.isDebugEnabled()) {
                log.debug("Server info received - " + jasperBean);
            }
        }

        return new JRBeanCollectionDataSource(jasperBeanList);
    }

    public static JRDataSource generateDataSource() {
        URL wsdlURL = JasperUtils.class.getResource("/ServerStatusReporter.wsdl");

        if (wsdlURL == null) {
            throw new DeploymentVerifierException("Missing ServerStatusReporter WSDL");
        }
        //        URL wsdlURL = new URL("http://localhost/hello?wsdl");
        QName serviceName = new QName("http://monitor.devops.carbon.wso2.org", "ServerStatusReporter");
        Service service = Service.create(wsdlURL, serviceName);
        ServerStatusReporterPortType client = service.getPort(ServerStatusReporterPortType.class);

        boolean nonSecureMode = Boolean.parseBoolean(System.getProperty("enableNonSecureMode"));
        if (nonSecureMode) {
            //only for oracle jdk
            ((BindingProvider)client).getRequestContext().
                    put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", getUnsecuredSocketFactory());
        }
        ServerInfo serverInfo = client.getServerInfo();

        List<ServerInfo> serverInfoList = new ArrayList<>();
        serverInfoList.add(serverInfo);

        return getDataSource(serverInfoList);
    }
    private static SSLSocketFactory getUnsecuredSocketFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());

            return context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace(); //todo
        }

        return null;
    }


    private static String getPatches(List<Patch> patchInfo) {
        StringBuilder b = new StringBuilder();
        for (Patch patch : patchInfo) {
            if (patch != null) {
                b.append(patch.getPatchId());
                b.append("   ");
            }
        }

        return b.toString();
    }

}
