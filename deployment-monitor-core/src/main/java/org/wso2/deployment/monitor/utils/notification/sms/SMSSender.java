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
    private List<String> recipients;
    private String messagePrefix;

    private SMSSender() {
        NotificationsConfig.SMSConfig smsConfig = ConfigurationManager.getConfiguration().getNotificationsConfig()
                .getSms();
        if (smsConfig != null) {
            this.isEnabled = smsConfig.isEnabled();
            this.provider = "clickatell".equalsIgnoreCase(smsConfig.getProvider()) ?
                    SMSProvider.CLICKATELL :
                    SMSProvider.BULKSMS;
            this.recipients = smsConfig.getRecipients();
            this.messagePrefix = smsConfig.getMessagePrefix();
            try {
                if (provider == SMSProvider.CLICKATELL) {
                    ClickatellHTTPGateway gateway = new ClickatellHTTPGateway(smsConfig.getEndpoint(),
                            smsConfig.getApiID(), smsConfig.getUsername(), smsConfig.getPassword());
                    gateway.setOutbound(true);
                    gateway.setSecure(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                } else if (provider == SMSProvider.BULKSMS) {
                    BulkSmsHTTPGateway gateway = new BulkSmsHTTPGateway(smsConfig.getEndpoint(),
                            smsConfig.getUsername(), smsConfig.getPassword());
                    gateway.setOutbound(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                }
            } catch (IOException | InterruptedException | SMSLibException e) {
                this.isEnabled = false;
                logger.error("SMS notification will be disabled. Error occurred while initializing the SMS Sender.", e);
            }
            if(!isEnabled){
                logger.warn("SMS Notifications are disabled");
            }
        } else {
            this.isEnabled = false;
            logger.warn("SMS Sender configurations were not found. SMS sending will be disabled.");
        }

    }

    private static void initialize() {
        synchronized (SMSSender.class) {
            if (instance == null) {
                instance = new SMSSender();
            }
        }
    }

    /**
     * Returns instance of the SMSSender if the instance is null, Initialises the SMSSender
     *
     * @return SMSSender instance
     */
    public static SMSSender getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    public static void cleanUpSMSSender() {
        try {
            Service.getInstance().stopService();
        } catch (IOException | InterruptedException | SMSLibException e) {
            logger.error("Error occurred while cleaning up SMS Sender", e);
        }
    }

    /**
     * Sends a SMS with the given text
     *
     * @param text {@link String}
     */
    public synchronized void send(String text) {
        text = messagePrefix + text;
        try {
            if (isEnabled) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending SMS: " + text);
                }
                for (String recipient : recipients) {
                    OutboundMessage msg = new OutboundMessage(recipient, text);
                    Service.getInstance().sendMessage(msg);
                }
            }
        } catch (IOException | InterruptedException | SMSLibException e) {
            logger.error("Error occurred while sending the sms : " + text, e);
        }
    }
}
