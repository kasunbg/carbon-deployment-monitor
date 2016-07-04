/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.wso2.deployment.monitor.utils.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.OutboundMessage;
import org.smslib.SMSLibException;
import org.smslib.Service;
import org.smslib.http.BulkSmsHTTPGateway;
import org.smslib.http.ClickatellHTTPGateway;
import org.wso2.deployment.monitor.core.ConfigurationManager;
import org.wso2.deployment.monitor.core.model.NotificationsConfig;

import java.io.IOException;
import java.util.List;

/**
 * SMS notifications implemented in this class. Only "Clickatell" and "BulkSMS", Bulk SMS provider support
 */
public class SMSSender {
    private static final Logger logger = LoggerFactory.getLogger(SMSSender.class);

    private enum SMSProvider {CLICKATELL, BULKSMS}

    private static volatile SMSSender instance;
    private SMSProvider provider;
    private boolean isEnabled;
    private String endpoint;
    private String apiId;
    private String username;
    private String password;
    private List<String> recipients;

    private SMSSender(NotificationsConfig.SMSConfig smsConfig) {
        this.isEnabled = smsConfig.isEnabled();
        this.endpoint = smsConfig.getEndpoint();
        this.provider = "clickatell".equalsIgnoreCase(smsConfig.getProvider()) ?
                SMSProvider.CLICKATELL :
                SMSProvider.BULKSMS;
        if (this.provider == SMSProvider.CLICKATELL) {
            this.apiId = smsConfig.getApiID();
        }
        this.username = smsConfig.getUsername();
        this.password = smsConfig.getPassword();
        this.recipients = smsConfig.getRecipients();
    }

    /**
     * Returns instance of the SMSSender if the instance is null, Initialises the SMSSender
     *
     * @return SMSSender instance
     */
    public static synchronized SMSSender getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    /**
     * Initializes the SMSSender object
     */
    private static void initialize() {
        NotificationsConfig.SMSConfig smsConfig = ConfigurationManager.getConfiguration().getNotificationsConfig()
                .getSms();
        if (smsConfig != null) {
            instance = new SMSSender(smsConfig);
        } else {
            logger.warn("SMS Sender configurations were not found. SMS sending will be disabled.");
            instance = new SMSSender(new NotificationsConfig.SMSConfig());
        }
    }

    /**
     * Sends a SMS with the given text
     *
     * @param text {@link String}
     */
    public synchronized void send(String text) {
        logger.debug("Sending SMS: " + text);
        try {
            if (isEnabled) {
                if (provider == SMSProvider.CLICKATELL) {
                    ClickatellHTTPGateway gateway = new ClickatellHTTPGateway(endpoint, apiId, username, password);
                    gateway.setOutbound(true);
                    gateway.setSecure(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                } else if (provider == SMSProvider.BULKSMS) {
                    BulkSmsHTTPGateway gateway = new BulkSmsHTTPGateway(endpoint, username, password);
                    gateway.setOutbound(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                }
                for (String recipient : recipients) {
                    OutboundMessage msg = new OutboundMessage(recipient, "WSO2DM: " + text);
                    Service.getInstance().sendMessage(msg);
                }
                Service.getInstance().stopService();
            } else {
                logger.warn("SMS Notification is disabled");
            }
        }  catch (IOException | InterruptedException | SMSLibException e) {
            logger.error("SMS Notification: Error occurred while sending the sms : " + text, e);
        }
    }
}
