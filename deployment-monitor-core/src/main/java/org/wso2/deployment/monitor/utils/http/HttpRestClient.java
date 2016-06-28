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
package org.wso2.deployment.monitor.utils.http;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;

import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * A wrapper for Apache Wink rest client
 */
public class HttpRestClient {

    /**
     * Generic rest client for basic rest POST calls over REST based on Apache Wink.
     *
     * @param resourceUrl     Resource endpoint Url
     * @param contentType     ContentType of request
     * @param acceptMediaType ContentType for response
     * @param postBody        Body
     * @param queryParamMap   Map of Query parameters
     * @param headerMap       Map of headers
     * @param cookie          jSessionID in form of JSESSIONID=<ID>
     * @return
     */
    public ClientResponse post(String resourceUrl, String contentType,
            String acceptMediaType, Object postBody,
            Map<String, String> queryParamMap,
            Map<String, String> headerMap,
            String cookie) {

        org.apache.wink.client.RestClient client = getClient();

        Resource resource = client.resource(resourceUrl);

        if (!(queryParamMap.size() <= 0)) {
            for (Map.Entry<String, String> queryParamEntry : queryParamMap.entrySet()) {
                resource.queryParam(queryParamEntry.getKey(), queryParamEntry.getValue());
            }
        }
        if (!(headerMap.size() <= 0)) {
            for (Map.Entry<String, String> headerEntry : headerMap.entrySet()) {
                resource.header(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (cookie != null) {
            resource.cookie(cookie);
        }
        return resource.contentType(contentType).
                accept(acceptMediaType).post(postBody);
    }

    public ClientResponse get(String resourceUrl,
            Map<String, String> queryParamMap,
            Map<String, String> headerMap,
            String cookie) {
        org.apache.wink.client.RestClient client = getClient();
        Resource resource = client.resource(resourceUrl);
        MultivaluedMap<String, String> queryParamInMap = new MultivaluedHashMap<>();
        if (!(queryParamMap.size() <= 0)) {
            for (Map.Entry<String, String> queryParamEntry : queryParamMap.entrySet()) {
                queryParamInMap.add(queryParamEntry.getKey(), queryParamEntry.getValue());
            }
            resource.queryParams(queryParamInMap);
        }
        if (!(headerMap.size() <= 0)) {
            for (Map.Entry<String, String> headerEntry : headerMap.entrySet()) {
                resource.header(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (cookie != null) {
            resource.cookie(cookie);
        }
        return resource.get();
    }

    private org.apache.wink.client.RestClient getClient() {
        ClientConfig config = new ClientConfig();
        config.followRedirects(true);

        return new org.apache.wink.client.RestClient(config);
    }

}
