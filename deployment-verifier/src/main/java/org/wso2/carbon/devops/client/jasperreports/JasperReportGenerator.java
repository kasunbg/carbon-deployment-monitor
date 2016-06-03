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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JasperReportGenerator {


    public static void generateReport(InputStream jasperTemplateInputStream, Path destFile, JRDataSource dataSource)
            throws IOException, JRException {

        Files.createDirectories(destFile.getParent());
        OutputStream compileOut = new BufferedOutputStream(new FileOutputStream(destFile.toFile()));

        Map<String, Object> params = new HashMap<>();
        params.put("Author", "WSO2 Carbon");
        params.put("ReportTitle", "WSO2 Carbon product deployment information");

        JasperCompileManager.compileReportToStream(jasperTemplateInputStream, compileOut);
        JasperPrint jasperPrint = JasperFillManager
                .fillReport(destFile.toAbsolutePath().toString(), params, dataSource);

        JasperExportManager.exportReportToHtmlFile(jasperPrint, "dest.html");
        JasperExportManager.exportReportToPdfFile(jasperPrint, "dest.pdf");

        JasperViewer jasperViewer = new JasperViewer(jasperPrint);
        jasperViewer.setVisible(true);


    }
}
