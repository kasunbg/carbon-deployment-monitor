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

package org.wso2.deployment.monitor.impl.task;

import org.apache.commons.httpclient.HttpStatus;
//import org.apache.wink.client.ClientResponse;
import org.wso2.deployment.monitor.api.DeploymentMonitorTask;
import org.wso2.deployment.monitor.api.RunStatus;
import org.wso2.deployment.monitor.core.model.ServerGroup;
import org.wso2.deployment.monitor.api.HostBean;
import org.wso2.deployment.monitor.utils.http.HttpRestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Pings the given servers
 * todo : Commented to resolve the classloading issue for msf4j. Need to move to another http implementation
 */
public class PingTask implements DeploymentMonitorTask {
    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
        return null;
    }
    //
//    //Default values for the check
//    private static final String DEFAULT_PATH = "/carbon/product/about.html";
//    private static final String DEFAULT_RESPONSE_CONTAIN = "About WSO2 Carbon";
//
//    private static final String HTTP_METHOD = "httpMethod";
//    private static final String HTTP_GET = "GET";
//    private static final String HTTP_POST = "POST";
//
//    private static final String PATH = "path";
//
//    private static final String QUERY_PARAMS = "queryParams";
//    private static final String QUERY_PARAM_SEPARATOR = "&";
//    private static final String QUERY_PARAM_VALUE_SEPARATOR = "=";
//
//    private static final String POST_BODY = "postBody";
//    private static final String ACCEPT_MEDIA_TYPE = "acceptMediaType";
//    private static final String CONTENT_TYPE = "contentType";
//
//    private static final String STATUS_CODE = "statusCode";
//
//    private static final String RESPONSE_CONTAINS = "responseContains";
//
//    private static final String HEADERS = "headers";
//    private static final String HEADERS_SEPARATOR = ",";
//    private static final String HEADER_VALUE_SEPARATOR = ":";
//
//    private List<HostBean> failedHosts = new ArrayList<>();
//    private List<HostBean> successHosts = new ArrayList<>();
//
//    //We use this map to send the details of each failed host
//    private Map<String, Object> resultMap = new HashMap<>();
//
//    @Override public RunStatus execute(ServerGroup serverGroup, Properties customParams) {
//
//        RunStatus status = new RunStatus();
//        HttpRestClient restClient = new HttpRestClient();
//
//        String httpMethod;
//        if (customParams.get(HTTP_METHOD) != null) {
//            httpMethod = (String) customParams.get(HTTP_METHOD);
//        } else {
//            httpMethod = HTTP_GET;
//        }
//
//        String path;
//        if (customParams.get(PATH) != null) {
//            path = (String) customParams.get(PATH);
//        } else {
//            path = DEFAULT_PATH;
//        }
//
//        String queryParams = null;
//        if (customParams.get(QUERY_PARAMS) != null) {
//            queryParams = (String) customParams.get(QUERY_PARAMS);
//        }
//        Map<String, String> queryParamsMap = new HashMap<>();
//        if(queryParams != null) {
//            for (String queryParam : queryParams.split(QUERY_PARAM_SEPARATOR)) {
//                String[] tmpArray = queryParam.split(QUERY_PARAM_VALUE_SEPARATOR);
//                queryParamsMap.put(tmpArray[0].trim(), tmpArray[1].trim());
//            }
//        }
//
//        String postBody = null;
//        if (customParams.get(POST_BODY) != null) {
//            postBody = (String) customParams.get(POST_BODY);
//        }
//
//        String acceptMediaType = null;
//        if (customParams.get(ACCEPT_MEDIA_TYPE) != null) {
//            acceptMediaType = (String) customParams.get(ACCEPT_MEDIA_TYPE);
//        }
//
//        String contentType = null;
//        if (customParams.get(CONTENT_TYPE) != null) {
//            contentType = (String) customParams.get(CONTENT_TYPE);
//        }
//
//        int statusCode;
//        if (customParams.get(STATUS_CODE) != null) {
//            statusCode = (Integer) customParams.get(STATUS_CODE);
//        } else {
//            statusCode = HttpStatus.SC_OK;
//        }
//
//        String bodyValue;
//        if (customParams.get(RESPONSE_CONTAINS) != null) {
//            bodyValue = (String) customParams.get(RESPONSE_CONTAINS);
//        } else {
//            bodyValue = DEFAULT_RESPONSE_CONTAIN;
//        }
//
//        //i.e Authorization:Bearer XXXXXXXXXXXX,Accept: Application/json
//        String headers = null;
//        if (customParams.get(HEADERS) != null) {
//            headers = (String) customParams.get(HEADERS);
//        }
//        Map<String, String> headersMap = new HashMap<>();
//        if(headers != null) {
//            for (String header : headers.split(HEADERS_SEPARATOR)) {
//                String[] tmpArray = header.split(HEADER_VALUE_SEPARATOR);
//                headersMap.put(tmpArray[0].trim(), tmpArray[1].trim());
//            }
//        }
//
//        for (String host : serverGroup.getHosts()) {
//            HostBean hostBean = new HostBean();
//            hostBean.setHostName(host);
//            hostBean.setNodeIndex(serverGroup.getHosts().indexOf(host));
//            ClientResponse response;
//            try {
//                if (HTTP_POST.equalsIgnoreCase(httpMethod)) {
//                    response = restClient
//                            .post(host + path, contentType, acceptMediaType, postBody, queryParamsMap, headersMap,
//                                    null);
//                } else {
//                    response = restClient.get(host + path, queryParamsMap, headersMap, null);
//                }
//            } catch (Exception e) {
//                addErrorDetails(hostBean, e.getMessage());
//                continue;
//            }
//
//            if (response.getStatusCode() != statusCode) {
//                addErrorDetails(hostBean, response.getStatusCode() + " " + response.getMessage());
//                continue;
//            }
//
//            String responseAsString = response.getEntity(String.class);
//            if (!responseAsString.contains(bodyValue)) {
//                addErrorDetails(hostBean, "Response body does not contain the defined value");
//            }
//            hostBean.setTaskSuccess(true);
//            successHosts.add(hostBean);
//        }
//
//        if (failedHosts.isEmpty()) {
//            status.setSuccess(true);
//            status.setSuccessHosts(successHosts);
//            return status;
//        } else {
//            status.setSuccess(false);
//            status.setSuccessHosts(successHosts);
//            status.setFailedHosts(failedHosts);
//            status.setCustomTaskDetails(resultMap);
//            return status;
//        }
//    }
//
//    private void addErrorDetails(HostBean hostBean, String message) {
//        hostBean.setTaskSuccess(false);
//        hostBean.setErrorMsg(message);
//        failedHosts.add(hostBean);
//    }
}
