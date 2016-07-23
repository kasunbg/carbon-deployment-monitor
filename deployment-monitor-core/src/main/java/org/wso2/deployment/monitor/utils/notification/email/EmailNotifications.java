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

package org.wso2.deployment.monitor.utils.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.deployment.monitor.core.ConfigurationManager;
import org.wso2.deployment.monitor.core.model.NotificationsConfig;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.wso2.deployment.monitor.utils.notification.email.EmailConstants.EMAIL_BODY;
import static org.wso2.deployment.monitor.utils.notification.email.EmailConstants.EMAIL_SUBJECT;

/**
 * This class sends email notifications.
 */
public class EmailNotifications extends Observable {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotifications.class);

    private Queue<Map> mailQueue;
    private ExecutorService executorService;

    private boolean isEmailEnabled;
    private Session session;
    private InternetAddress[] toAddresses;
    private InternetAddress fromAddress;
    private String subjectPrefix;



    private static volatile EmailNotifications instance = new EmailNotifications();

    private EmailNotifications() {
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
            this.subjectPrefix = emailConfig.getSubjectPrefix();

            mailQueue = new ConcurrentLinkedQueue<>();
            executorService = Executors.newFixedThreadPool(3);
            MailQueueObserver mailQueueObserver = new MailQueueObserver();
            addObserver(mailQueueObserver);

        } else {
            this.isEmailEnabled = false;
            logger.warn("Email Sender configurations were not found. Email sending will be disabled.");
        }
        if(!this.isEmailEnabled){
            logger.warn("Email Notifications are disabled.");
        }
    }

    private static void initialize() {
        synchronized (EmailNotifications.class) {
            if (instance == null) {
                instance = new EmailNotifications();
            }
        }
    }

    public static EmailNotifications getInstance() {
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
            if (!executorService.awaitTermination(EmailConstants.DEFAULT_TIMEOUT_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(EmailConstants.DEFAULT_TIMEOUT_VALUE, TimeUnit.SECONDS)) {
                    logger.error("Email sender executor pool did not terminate");
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
     * Add email to the mail queue
     *
     * @param messageBody email body
     * @param subject     email subject
     */
    public void sendMail(String subject, String messageBody) {
        if(isEmailEnabled) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending Email: " + subjectPrefix + subject);
            }
            Map<String, Object> mail = new Hashtable<>();
            mail.put(EmailConstants.EMAIL_SUBJECT, subjectPrefix + subject);
            mail.put(EmailConstants.EMAIL_BODY, messageBody);
            mailQueue.add(mail);
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Mail queue observer which allocates the threads and sends the email
     */
    private class MailQueueObserver implements Observer {

        private MailSenderErrorObserver mailSenderErrorObserver;

        MailQueueObserver() {
            mailSenderErrorObserver = new MailSenderErrorObserver();
        }

        /**
         * {@inheritDoc}
         */
        @Override public void update(Observable o, Object arg) {
            while (!mailQueue.isEmpty()) {
                Map email = mailQueue.poll();
                MailSender mailSender = new MailSender(mailSenderErrorObserver, email);
                executorService.execute(mailSender);
            }
        }
    }

    /**
     * This is to keep the sending failed emails and try sending them again
     * once the error has been fixed
     */
    private static class MailSenderErrorObserver implements Observer {

        private Queue<Map> failedEmailQueue;

        MailSenderErrorObserver() {
            failedEmailQueue = new ConcurrentLinkedQueue<>();
        }

        /**
         * {@inheritDoc}
         */
        @Override public void update(Observable o, Object arg) {
            if (arg instanceof Map) {
                Map email = (Map) arg;
                failedEmailQueue.add(email);
            } else if (arg instanceof Boolean && (Boolean) arg) {
                while (!failedEmailQueue.isEmpty()) {
                    Map email = failedEmailQueue.poll();
                    EmailNotifications.getInstance()
                            .sendMail((String) email.get(EMAIL_BODY), (String) email.get(EMAIL_SUBJECT));
                }
            } else {
                logger.error("No argument specified. ");
            }
        }
    }

    /**
     * Mail sending thread
     */
    private class MailSender extends Observable implements Runnable {

        private Map email;

        MailSender(MailSenderErrorObserver errorObserver, Map email) {
            this.email = email;
            addObserver(errorObserver);
        }

        /**
         * {@inheritDoc}
         */
        @Override public void run() {
            Message simpleMessage = new MimeMessage(session);
            try {
                simpleMessage.setFrom(fromAddress);
                simpleMessage.setRecipients(Message.RecipientType.TO, toAddresses);
                simpleMessage.setSubject((String) email.get(EMAIL_SUBJECT));

                Multipart multipart = new MimeMultipart();

                BodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(email.get(EMAIL_BODY), EmailConstants.BODY_CONTENT_TYPE);

                multipart.addBodyPart(bodyPart);

                simpleMessage.setContent(multipart);
                Transport.send(simpleMessage);
            } catch (MessagingException e) {
                logger.error("Error occurred while sending email ", e);
            }
        }
    }
}
