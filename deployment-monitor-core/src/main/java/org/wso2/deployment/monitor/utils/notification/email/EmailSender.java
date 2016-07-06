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
    private static volatile EmailSender emailSender = null;

    private Session session;
    private boolean isEmailEnabled;
    private InternetAddress fromAddress;
    private InternetAddress[] toAddresses;

    private EmailSender() {
        NotificationsConfig.EmailConfig emailConfig = ConfigurationManager.getConfiguration().
                getNotificationsConfig().getEmailConfig();
        if (emailConfig != null) {
            this.isEmailEnabled = emailConfig.isEnabled();
            Properties props = new Properties();
            props.put(EmailConstants.MAIL_SMTP_STARTTLS_ENABLE, String.valueOf(emailConfig.isTlsEnabled()));
            props.put(EmailConstants.MAIL_SMTP_SERVER, emailConfig.getSmtpServer());
            props.put(EmailConstants.MAIL_SMTP_PORT, emailConfig.getSmtpPort());

            if (emailConfig.isAuthentication()) {
                props.put(EmailConstants.MAIL_SMTP_AUTH, "true");
                session = Session.getInstance(props,
                        new MailAuthenticator(emailConfig.getUsername(), emailConfig.getPassword()));
            } else {
                session = Session.getDefaultInstance(props);
            }

            List<InternetAddress> to = new ArrayList<>(emailConfig.getToAddresses().size());
            for (String toAddress : emailConfig.getToAddresses()) {
                try {
                    to.add(new InternetAddress(toAddress));
                } catch (AddressException e) {
                    logger.error("Error occurred while creating recipient address : " + toAddress, e);
                }
            }
            toAddresses = new InternetAddress[to.size()];
            toAddresses = to.toArray(toAddresses);

            try {
                fromAddress = new InternetAddress(emailConfig.getFromAddress());
            } catch (AddressException e) {
                //disables email sending in this case
                this.isEmailEnabled = false;
                logger.error("Email Notification will be disabled. Error occurred while creating from address : "
                        + emailConfig.getFromAddress(), e);
            }
        } else {
            this.isEmailEnabled = false;
            logger.warn("Email Sender configurations were not found. Email sending will be disabled.");
        }
    }

    private static void initialize() {
        synchronized (EmailSender.class) {
            if (emailSender == null) {
                emailSender = new EmailSender();
            }
        }
    }

    public static EmailSender getInstance() {
        if (emailSender == null) {
            initialize();
        }
        return emailSender;
    }

    /**
     * Sends mail with specified params
     *
     * @param subject Subject of the mail
     * @param body    Text body part one
     */
    public synchronized void send(String subject, String body) {
        if (isEmailEnabled) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending Email: " + subject);
            }

            Message simpleMessage = new MimeMessage(session);
            try {
                simpleMessage.setFrom(fromAddress);
                simpleMessage.setRecipients(RecipientType.TO, toAddresses);
                simpleMessage.setSubject(EmailConstants.SUBJECT_START + subject);

                Multipart multipart = new MimeMultipart();

                BodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(body, EmailConstants.BODY_CONTENT_TYPE);

                multipart.addBodyPart(bodyPart);

                simpleMessage.setContent(multipart);
                Transport.send(simpleMessage);
            } catch (MessagingException e) {
                logger.error("Error occurred while sending email ", e);
            }
        } else {
            logger.warn("Email Notification is disabled.");
        }
    }
}
