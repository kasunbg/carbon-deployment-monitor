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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;

public class SimpleReport {
    static DefaultTableModel tableModel;

    public SimpleReport() {
        JasperPrint jasperPrint = null;
        TableModelData();
        try {

            String tmpDir = System.getProperty("java.io.tmpdir");
            Path compiledFile = Paths.get(tmpDir, "deployment-verifier", "CarbonDeploymentReportColumnIndex.jasper");
            Files.createDirectories(compiledFile.getParent());

            OutputStream compileOut = new BufferedOutputStream(new FileOutputStream(compiledFile.toFile()));

            URL reportURL = this.getClass().getResource("/jasperreports/report-template.jrxml");
            JasperCompileManager.compileReportToStream(reportURL.openStream(), compileOut);
//            JasperCompileManager.compileReportToFile("deployment-verifier/src/main/resources/jasperreports/report-template.jrxml");
            jasperPrint = JasperFillManager
                    .fillReport(compiledFile.toAbsolutePath().toString(), new HashMap(), new JRTableModelDataSource(tableModel));
            JasperViewer jasperViewer = new JasperViewer(jasperPrint);
            jasperViewer.setVisible(true);
        } catch (JRException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static DefaultTableModel TableModelData() {
        String[] columnNames = { "Id", "Name", "Department", "Email" };
        String[][] data = {
                { "111", "G Conger", " Orthopaedic", "jim@wheremail.com" },
                { "222", "A Date", "ENT", "adate@somemail.com" },
                { "333", "R Linz", "Paedriatics", "rlinz@heremail.com" },
                { "444", "V Sethi", "Nephrology", "vsethi@whomail.com" },
                { "555", "K Rao", "Orthopaedics", "krao@whatmail.com" },
                { "666", "V Santana", "Nephrology", "vsan@whenmail.com" },
                { "777", "J Pollock", "Nephrology", "jpol@domail.com" },
                { "888", "H David", "Nephrology", "hdavid@donemail.com" },
                { "999", "P Patel", "Nephrology", "ppatel@gomail.com" },
                { "101", "C Comer", "Nephrology", "ccomer@whymail.com" }
        };
        tableModel = new DefaultTableModel(data, columnNames);
        return tableModel;
    }

    public static void main(String[] args) {
        new SimpleReport();
    }
}