package service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class EmailSender {

    private static final Logger LOGGER = Logger.getLogger(EmailSender.class.getName());

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String GMAIL_ADDRESS = System.getenv("EMAIL_ADDRESS");
    private static final String GMAIL_APP_PASSWORD = System.getenv("EMAIL_APP_PASSWORD");

    public boolean send(String toAddress, String subject, String htmlTemplate, Map<String, String> variables) {
        String html = fillTemplate(htmlTemplate, variables);

        LOGGER.info("Sending email to " + toAddress + " with subject '" + subject + "'");

        try {
            MimeMessage message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(GMAIL_ADDRESS));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=utf-8");

            Transport.send(message);
            LOGGER.info("Email sent to " + toAddress);
            return true;
        } catch (MessagingException e) {
            LOGGER.severe("Email to " + toAddress + " failed: " + e.getMessage());
            return false;
        }
    }

    private Session buildSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        return Session.getInstance(properties, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_ADDRESS, GMAIL_APP_PASSWORD);
            }
        });
    }

    private String fillTemplate(String htmlTemplate, Map<String, String> variables) {
        String result = htmlTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}