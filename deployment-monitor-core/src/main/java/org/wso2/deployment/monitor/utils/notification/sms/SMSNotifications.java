/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class sends SMS notifications.
 */
public class SMSNotifications extends Observable {

    private static final Logger logger = LoggerFactory.getLogger(SMSNotifications.class);

    private Queue<Map> smsQueue;
    private ExecutorService executorService;

    private boolean isSMSEnabled;
    private List<String> recipients;
    private String messagePrefix;
    private SMSNotifications.SMSProvider provider;

    private enum SMSProvider {CLICKATELL, BULKSMS}

    private static volatile SMSNotifications instance = new SMSNotifications();

    private SMSNotifications() {
        NotificationsConfig.SMSConfig smsConfig = ConfigurationManager.getConfiguration().getNotificationsConfig()
                .getSms();
        if (smsConfig != null) {
            this.isSMSEnabled = smsConfig.isEnabled();
            this.provider = SMSConstants.CLICKATELL.equalsIgnoreCase(smsConfig.getProvider()) ?
                    SMSNotifications.SMSProvider.CLICKATELL :
                    SMSNotifications.SMSProvider.BULKSMS;
            this.recipients = smsConfig.getRecipients();
            this.messagePrefix = smsConfig.getMessagePrefix();
            try {
                if (provider == SMSNotifications.SMSProvider.CLICKATELL) {
                    ClickatellHTTPGateway gateway = new ClickatellHTTPGateway(smsConfig.getEndpoint(),
                            smsConfig.getApiID(), smsConfig.getUsername(), smsConfig.getPassword());
                    gateway.setOutbound(true);
                    gateway.setSecure(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                } else if (provider == SMSNotifications.SMSProvider.BULKSMS) {
                    BulkSmsHTTPGateway gateway = new BulkSmsHTTPGateway(smsConfig.getEndpoint(),
                            smsConfig.getUsername(), smsConfig.getPassword());
                    gateway.setOutbound(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                }

                smsQueue = new ConcurrentLinkedQueue<>();
                executorService = Executors.newFixedThreadPool(3);
                SMSQueueObserver smsQueueObserver = new SMSQueueObserver();
                addObserver(smsQueueObserver);

            } catch (IOException | InterruptedException | SMSLibException e) {
                this.isSMSEnabled = false;
                logger.error("SMS notification will be disabled. Error occurred while initializing the SMS Sender.", e);
            }
            if (!isSMSEnabled) {
                logger.warn("SMS Notifications are disabled");
            }
        } else {
            this.isSMSEnabled = false;
            logger.warn("SMS Sender configurations were not found. SMS sending will be disabled.");
        }

    }

    private static void initialize() {
        synchronized (SMSNotifications.class) {
            if (instance == null) {
                instance = new SMSNotifications();
            }
        }
    }

    public static SMSNotifications getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    /**
     * Shutdown the thread pool
     */
    public void shutdownAndAwaitTermination() {
        executorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(SMSConstants.DEFAULT_TIMEOUT_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(SMSConstants.DEFAULT_TIMEOUT_VALUE, TimeUnit.SECONDS)) {
                    logger.error("SMS sender executor pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            logger.error("An error occurred when shutting down and awaiting the termination of the existing tasks", ie);
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Add SMS to the sms queue
     *
     * @param message Message
     */
    public void sendSMS(String message) {
        if (isSMSEnabled) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending SMS: " + messagePrefix + message);
            }
            Map<String, Object> sms = new Hashtable<>();
            sms.put(SMSConstants.SMS_MESSAGE, messagePrefix + message);
            smsQueue.add(sms);
            setChanged();
            notifyObservers();
        }
    }

    /**
     * SMS queue observer which allocates the threads and sends the sms
     */
    private class SMSQueueObserver implements Observer {

        private SMSSenderErrorObserver smsSenderErrorObserver;

        SMSQueueObserver() {
            smsSenderErrorObserver = new SMSSenderErrorObserver();
        }

        /**
         * {@inheritDoc}
         */
        @Override public void update(Observable o, Object arg) {
            while (!smsQueue.isEmpty()) {
                Map sms = smsQueue.poll();
                SMSSender smsSender = new SMSSender(smsSenderErrorObserver, sms);
                executorService.execute(smsSender);
            }
        }
    }

    /**
     * This is to keep the sending failed sms and try sending them again
     * once the error has been fixed
     */
    private static class SMSSenderErrorObserver implements Observer {

        private Queue<Map> failedSMSQueue;

        SMSSenderErrorObserver() {
            failedSMSQueue = new ConcurrentLinkedQueue<>();
        }

        /**
         * {@inheritDoc}
         */
        @Override public void update(Observable o, Object arg) {
            if (arg instanceof Map) {
                Map sms = (Map) arg;
                failedSMSQueue.add(sms);
            } else if (arg instanceof Boolean && (Boolean) arg) {
                while (!failedSMSQueue.isEmpty()) {
                    Map sms = failedSMSQueue.poll();
                    SMSNotifications.getInstance().sendSMS((String) sms.get(SMSConstants.SMS_MESSAGE));
                }
            } else {
                logger.error("No argument specified. ");
            }
        }
    }

    /**
     * SMS sending thread
     */
    private class SMSSender extends Observable implements Runnable {

        private Map sms;

        SMSSender(SMSSenderErrorObserver errorObserver, Map sms) {
            this.sms = sms;
            addObserver(errorObserver);
        }

        /**
         * {@inheritDoc}
         */
        @Override public void run() {
            try {
                for (String recipient : recipients) {
                    OutboundMessage msg = new OutboundMessage(recipient, (String) sms.get(SMSConstants.SMS_MESSAGE));
                    Service.getInstance().sendMessage(msg);
                }
            } catch (IOException | InterruptedException | SMSLibException e) {
                logger.error("Error occurred while sending the sms : " + sms.get(SMSConstants.SMS_MESSAGE), e);
            }
        }
    }
}
