package com.liveramp.mail_utils;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liveramp.java_support.alerts_handler.MailOptions;
import com.liveramp.java_support.concurrent.ParallelUtil;

class MailerHelper {
  private static final Logger LOG = LoggerFactory.getLogger(MailerHelper.class);

  public static void mail(MailOptions options, SmtpConfig smtpConfig) throws IOException {
    mail(options.getFromEmail(),
        options.getToEmails(),
        options.getSubject(),
        options.getTags(),
        options.getMsg(),
        options.getMsgContentType(),
        options.getCcEmails(),
        options.getBccEmails(),
        options.getAttachments(),
        smtpConfig);
  }

  private static void mail(String fromEmail,
                           List<String> toEmails,
                           String subject,
                           List<String> tags,
                           String body,
                           String contentType,
                           List<String> ccEmails,
                           List<String> bccEmails,
                           List<String> attachments,
                           SmtpConfig smtpConfig) throws IOException {
    if (toEmails == null || toEmails.isEmpty()) {
      return;
    }
    // Parse emails
    toEmails = getAllEmails(toEmails);

    if (fromEmail == null) {
      // Default to first toEmail
      fromEmail = toEmails.get(0);
    }

    Properties props = System.getProperties();
    props.put("mail.smtp.host", smtpConfig.getHost());
    props.put("mail.smtp.timeout", smtpConfig.getTimeout());
    props.put("mail.smtp.connectiontimeout", smtpConfig.getTimeout());
    props.put("mail.smtp.writetimeout", smtpConfig.getTimeout());


    Session session = Session.getInstance(props, null);

    try {
      boolean smtpPresent = false;
      for (Provider provider : session.getProviders()) {
        if (provider.getProtocol().equals("smtp")) {
          smtpPresent = true;
        }
      }
      if (!smtpPresent) {
        LOG.error("No SMTP provider!");
      }
    } catch (Throwable t) {
      //Keep thing in prod working the same
    }

    try {
      // Define a new mail message
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(fromEmail));
      for (String toEmail : toEmails) {
        message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(toEmail));
      }

      String finalSubject;
      if (tags == null) {
        finalSubject = subject;
      } else {
        StringBuilder subjectBuilder = new StringBuilder();
        for (String tag : tags) {
          subjectBuilder.append("[");
          subjectBuilder.append(tag);
          subjectBuilder.append("]");
        }
        subjectBuilder.append(" ");
        subjectBuilder.append(subject);
        finalSubject = subjectBuilder.toString();
      }
      message.setSubject(finalSubject);
      message.setContent(body, contentType);
      if (ccEmails != null) {
        for (String ccEmail : ccEmails) {
          message.addRecipient(MimeMessage.RecipientType.CC, new InternetAddress(ccEmail));
        }
      }
      if (bccEmails != null) {
        for (String bccEmail : bccEmails) {
          message.addRecipient(MimeMessage.RecipientType.BCC, new InternetAddress(bccEmail));
        }
      }

      if (attachments != null) {
        Multipart multi = new MimeMultipart();
        MimeBodyPart mBody = new MimeBodyPart();
        mBody.setContent(body, contentType);
        multi.addBodyPart(mBody);
        for (String file : attachments) {
          MimeBodyPart attachment = new MimeBodyPart();
          attachment.setDataHandler(new DataHandler(new FileDataSource(file)));
          attachment.setFileName(new File(file).getName());
          multi.addBodyPart(attachment);
        }
        message.setContent(multi);
      }

      boolean messageSent = ParallelUtil.runWithTimeout(() -> {
        try {
          // Send the message
          Transport smtp = session.getTransport("smtp");
          smtp.connect();
          smtp.sendMessage(message, message.getAllRecipients());
          smtp.close();
        } catch (Exception e) {
          LOG.error("Exception sending message", e);
        }
      }, smtpConfig.getTimeout() * 2, TimeUnit.MILLISECONDS);

      if (!messageSent) {
        throw new IOException("Error sending email: Hard cut-off timeout was reached. Timeout hard limit is " +
            smtpConfig.getTimeout() * 2 + " milliseconds");
      }
    } catch (javax.mail.MessagingException e) {
      throw new IOException("Error sending email: ", e);
    }
  }

  private static List<String> getAllEmails(List<String> toEmails) {
    List<String> toEmailsAll = new ArrayList<String>();
    for (String toEmail : toEmails) {
      final List<String> emails = parseEmails(toEmail);
      if (emails != null) {
        toEmailsAll.addAll(emails);
      }
    }
    return toEmailsAll;
  }

  private static List<String> parseEmails(String toEmail) {
    if (toEmail == null) {
      return null;
    } else {
      return Arrays.asList(StringUtils.split(toEmail, ","));
    }
  }

  public static void main(String[] args) throws IOException {
    MailerHelper.mail(new MailOptions(Arrays.asList("bpodgursky@liveramp.com"), "Test", "Test"), new SmtpConfig("mail.liveramp.net"));
  }
}