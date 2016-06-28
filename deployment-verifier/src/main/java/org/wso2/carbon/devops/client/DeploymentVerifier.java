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
package org.wso2.carbon.devops.client;

import net.sf.jasperreports.engine.JRException;
import org.wso2.carbon.devops.client.jasperreports.JasperReportGenerator;
import org.wso2.carbon.devops.client.jasperreports.JasperUtils;
import org.wso2.carbon.devops.monitor.client.ServerStatusReporterPortType;
import org.wso2.carbon.devops.monitor.beans.xsd.ServerInfo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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

/**
 * todo.
 */
public class DeploymentVerifier {

//    private static final String JASPER_SOURCE = "/jasperreports/jasper2.jrxml";
//    private static final String JASPER_SOURCE = "/jasperreports/report-template.jrxml";
    private static final String JASPER_SOURCE = "/jasperreports/carbon-deployment-report.jrxml";

    private void execute() throws IOException, JRException {
        URL wsdlURL = this.getClass().getResource("/ServerStatusReporter.wsdl");

        if (wsdlURL == null) {
            throw new DeploymentVerifierException("Missing ServerStatusReporter WSDL");
        }
        //        URL wsdlURL = new URL("http://localhost/hello?wsdl");
        QName serviceName = new QName("http://monitor.devops.carbon.wso2.org", "ServerStatusReporter");
        Service service = Service.create(wsdlURL, serviceName);
        ServerStatusReporterPortType client = service.getPort(ServerStatusReporterPortType.class);

        ((BindingProvider)client).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "https://esb01:9443/services/ServerStatusReporter/getServerInfo");

        boolean nonSecureMode = Boolean.parseBoolean(System.getProperty("enableNonSecureMode"));
        if (nonSecureMode) {
            //only for oracle jdk
            ((BindingProvider)client).getRequestContext().
                put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", getUnsecuredSocketFactory());
        }
        ServerInfo serverInfo = client.getServerInfo();

        //generate report
        URL reportURL = this.getClass().getResource(JASPER_SOURCE);
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path compiledFile = Paths.get(tmpDir, "deployment-verifier", "CarbonDeploymentReportColumnIndex.jasper");

        List<ServerInfo> serverInfoList = new ArrayList<>();
        serverInfoList.add(serverInfo);
        serverInfoList.add(serverInfo);//todo
        JasperReportGenerator.generateReport(reportURL.openStream(), compiledFile,
                JasperUtils.getDataSource(serverInfoList));

        System.out.
                println(serverInfo.getPatchInfo().size());
    }

    private SSLSocketFactory getUnsecuredSocketFactory() {
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

    public static void main(String[] args) throws IOException, JRException {
        System.setProperty("enableNonSecureMode", "true"); //todo
        new DeploymentVerifier().execute();
    }
}
