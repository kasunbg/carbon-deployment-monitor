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

package org.wso2.deployment.monitor.utils.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.ConfigurationManager;
import org.wso2.deployment.monitor.core.model.NotificationsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Mail notifications implemented in this class
 */
public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    private static EmailSender emailSender = null;

    private boolean isEmailEnabled;
    private boolean isTlsEnabled;
    private boolean authEnabled;
    private String mailUser;
    private String mailUserPwd;
    private String smtpServer;
    private int smtpPort;
    private String from;
    private List<String> toAddresses;

    private EmailSender(NotificationsConfig.EmailConfig emailConfig) {
        this.isEmailEnabled = emailConfig.isEnabled();
        this.isTlsEnabled = emailConfig.isTlsEnabled();
        this.toAddresses = emailConfig.getToAddresses();
        this.from = emailConfig.getFromAddress();
        this.smtpPort = emailConfig.getSmtpPort();
        this.smtpServer = emailConfig.getSmtpServer();
        this.mailUserPwd = emailConfig.getPassword();
        this.mailUser = emailConfig.getUsername();
        this.authEnabled = emailConfig.isAuthentication();
    }

    public static EmailSender getInstance() {
        if (emailSender == null) {
            initialize();
        }
        return emailSender;
    }

    /**
     * Initializes EmailSender object
     */
    private static void initialize() {
        synchronized (EmailSender.class) {
            NotificationsConfig.EmailConfig emailConfig = ConfigurationManager.getConfiguration().
                    getNotificationsConfig().getEmailConfig();
            if (emailConfig != null) {
                emailSender = new EmailSender(emailConfig);
            } else {
                logger.warn("Email Sender configurations were not found. Email sending will be disabled.");
                emailSender = new EmailSender(new NotificationsConfig.EmailConfig());
            }
        }
    }

    /**
     * Sends mail with specified params
     *
     * @param subject Subject of the mail
     * @param text1   Text body part one
     * @param text2   Text body part two
     */
    public void send(String subject, String text1, String text2) {

        if (isEmailEnabled) {
            Properties props = new Properties();
            props.put("mail.smtp.starttls.enable", isTlsEnabled);
            props.put("mail.smtp.host", smtpServer);
            props.put("mail.smtp.port", smtpPort);

            Session session;
            if (authEnabled) {
                props.put("mail.smtp.auth", "true");
                session = Session.getInstance(props, new MailAuthenticator(mailUser, mailUserPwd));
            } else {
                session = Session.getDefaultInstance(props);
            }
            Message simpleMessage = new MimeMessage(session);
            InternetAddress fromAddress = null;

            List<InternetAddress> addressTo = new ArrayList<>(toAddresses.size());
            for (String toAddress : toAddresses) {
                try {
                    addressTo.add(new InternetAddress(toAddress));
                } catch (AddressException e) {
                    logger.error("Mail Notification: AddressException thrown while sending alerts", e);
                }
            }
            try {
                fromAddress = new InternetAddress(from);
            } catch (AddressException e) {
                logger.error("Mail Notification: AddressException thrown while sending alerts", e);
            }

            try {
                simpleMessage.setFrom(fromAddress);
                InternetAddress[] to = new InternetAddress[addressTo.size()];
                to = addressTo.toArray(to);
                simpleMessage.setRecipients(RecipientType.TO, to);
                simpleMessage.setSubject("Cloud Heartbeat: " + subject);

                Multipart multipart = new MimeMultipart();

                BodyPart part1 = new MimeBodyPart();
                part1.setContent(text1, "text/html");

                BodyPart part2 = new MimeBodyPart();
                part2.setContent(text2, "text/html");

                multipart.addBodyPart(part1);
                multipart.addBodyPart(part2);

                simpleMessage.setContent(multipart);
                Transport.send(simpleMessage);
            } catch (MessagingException e) {
                logger.error("Mail Notification: MessagingException thrown while getting the connection:"
                        + " sending alerts", e);
            }
        } else {
            logger.warn("Email Notifications are disabled.");
        }
    }
}
