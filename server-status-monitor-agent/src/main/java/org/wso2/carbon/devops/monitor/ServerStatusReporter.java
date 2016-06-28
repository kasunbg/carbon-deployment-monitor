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
package org.wso2.carbon.devops.monitor;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.Utils;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.devops.monitor.beans.Bundle;
import org.wso2.carbon.devops.monitor.beans.Patch;
import org.wso2.carbon.devops.monitor.beans.ServerInfo;
import org.wso2.carbon.devops.monitor.internal.OSGiDataHolder;
import org.wso2.carbon.ui.CarbonUIUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An axis2 service that returns the Carbon server info
 * This includes :
 *      Patch details
 *      Deployment Artifacts
 *      Dropins
 *      //Configurations
 *      //Cluster information
 *
 *      todo create a serverId and save it in the local registry
 *      todo extend admin service
 */
@SuppressWarnings("unused")
public class ServerStatusReporter {

    private static OSGiDataHolder dataHolder = OSGiDataHolder.getInstance();

    private static final String SERVER_URL = "https://${carbon.local.ip}:${carbon.management.port}"
            + "${carbon.context}/services/";

    public ServerInfo getServerInfo() {

        ServerInfo serverInfo = new ServerInfo();

        //1
        setProductMetadata(serverInfo);

        //2 get patch info
        Patch[] patches = getPatchInfo();
        serverInfo.setPatchInfo(patches);

        //3 get dropins info

        return serverInfo;
    }

    private void setProductMetadata(ServerInfo serverInfo) {
        ServerConfigurationService configService = dataHolder.getServerConfigurationService();
        String productName = configService.getFirstProperty("Name");
        String productVersion = configService.getFirstProperty("Version");
        String profile = System.getProperty("profile", "default");

        ConfigurationContext configContext = dataHolder.getConfigContextService().getServerConfigContext();
//        String serverURL = CarbonUtils.getServerURL(configService, configContext);
        String serverURL = CarbonUIUtil.getAdminConsoleURL(configService.getFirstProperty("WebContextRoot"));

        String ip;
        try {
            ip = Utils.getIpAddress();
        } catch (SocketException e) {
            ip = "";
        }

        serverInfo.setProductName(productName);
        serverInfo.setProductVersion(productVersion);
        serverInfo.setServerProfile(profile);
        serverInfo.setServerURL(serverURL);
        serverInfo.setServerIP(ip);
    }

    private Patch[] getPatchInfo() {
        File patchPath = getPath(System.getProperty("carbon.home"), "repository", "components", "patches");
        File[] patchFiles = patchPath.listFiles();
        if (patchFiles == null) {
            return new Patch[0];
        }

        //iterate patches
        Patch[] patches = new Patch[patchFiles.length - 1]; //todo infer the length with skipped urls
        for (int i = 0; i < patchFiles.length; i++) {
            File patchFile = patchFiles[i];
            if ("patch0000".equals(patchFile.getName()) || !patchFile.getName().startsWith("patch")) {
                continue;
            }

            Patch patch = new Patch();
            patch.setPatchId(patchFile.getName());

            //iterate bundles inside patches
            File[] bundleFiles = patchFile.listFiles();
            if (bundleFiles == null) {
                continue;
            }
            Bundle[] bundles = new Bundle[bundleFiles.length];
            for (int j = 0; j < bundleFiles.length; j++) {
                File bundleFile = bundleFiles[j];
                Bundle bundle = new Bundle();
                bundle.setFileName(bundleFile.getName());
                bundle.setMd5sum(md5sum(bundleFile));
                bundles[j] = bundle;
            }
            patch.setBundles(bundles);

            patches[i] = patch;
        }

        return patches;
    }

    private File getPath(String firstComponent, String... pathComponents) {
        StringBuilder builder = new StringBuilder();
        builder.append(firstComponent);
        builder.append(File.separatorChar);

        for (String path : pathComponents) {
            builder.append(path);
            builder.append(File.separator);
        }

        return new File(builder.toString());
    }

    private String md5sum(File file) {
        try {
            InputStream is = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] bytes = new byte[2048];
            int numBytes;
            while ((numBytes = is.read(bytes)) != -1) {
                md.update(bytes, 0, numBytes);
            }
            byte[] digest = md.digest();

            BigInteger bigInt = new BigInteger(1, digest);
            String hashtext = bigInt.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            return hashtext;
        } catch (NoSuchAlgorithmException | IOException e) {
            //md5 algorithm should exist.
            return "##ERROR## - " + e.getMessage();
        }

    }

}
