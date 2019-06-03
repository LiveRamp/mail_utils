package com.liveramp.mail_utils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liveramp.java_support.alerts_handler.AlertHelpers;
import com.liveramp.java_support.alerts_handler.AlertMessage;
import com.liveramp.java_support.alerts_handler.AlertMessages;
import com.liveramp.java_support.alerts_handler.AlertsHandler;
import com.liveramp.java_support.alerts_handler.AlertsUtil;
import com.liveramp.java_support.alerts_handler.MailOptions;
import com.liveramp.java_support.alerts_handler.configs.AlertMessageConfig;
import com.liveramp.java_support.alerts_handler.configs.AlertsHandlerConfig;
import com.liveramp.java_support.alerts_handler.recipients.AddRecipientContext;
import com.liveramp.java_support.alerts_handler.recipients.AlertRecipient;
import com.liveramp.java_support.alerts_handler.recipients.RecipientListBuilder;

public class MailerHelperAlertsHandler implements AlertsHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MailerHelperAlertsHandler.class);

  private final AlertsHandlerConfig alertsHandlerConfig;
  private final SmtpConfig config;

  public MailerHelperAlertsHandler(AlertsHandlerConfig alertsHandlerConfig, SmtpConfig config) {
    this.alertsHandlerConfig = alertsHandlerConfig;
    this.config = config;
  }

  @Override
  public void sendAlert(AlertMessage contents, AlertRecipient recipient, AlertRecipient... additionalRecipients) {
    sendEmail(contents, AlertHelpers.atLeastOneToList(recipient, additionalRecipients));
  }

  @Override
  public void sendAlert(String subject, String body, AlertRecipient recipient, AlertRecipient... additionalRecipients) {
    sendAlert(AlertMessages.build(subject, body), recipient, additionalRecipients);
  }

  @Override
  public void sendAlert(String subject, Throwable t, AlertRecipient recipient, AlertRecipient... additionalRecipients) {
    sendAlert(AlertMessages.build(subject, t), recipient, additionalRecipients);
  }

  @Override
  public void sendAlert(String subject, String body, Throwable t, AlertRecipient recipient, AlertRecipient... additionalRecipients) {
    AlertMessage message = AlertMessages.builder(subject)
        .setBody(body)
        .setThrowable(t)
        .build();
    sendAlert(message, recipient, additionalRecipients);
  }

  @Override
  public RecipientListBuilder resolveRecipients(List<AlertRecipient> recipients) {
    return AlertsUtil.getRecipients(recipients, alertsHandlerConfig, new AddRecipientContext(Optional.empty()));
  }

  private void sendEmail(AlertMessage alertMessage, List<AlertRecipient> recipients) {
    final AlertMessageConfig overriddenConfig = alertMessage.getConfigOverrides().overrideConfig(alertsHandlerConfig.getDefaultMessageConfig());
    final String subject = alertMessage.getSubject(overriddenConfig);
    final String body = alertMessage.getMessage(overriddenConfig);

    try {

      RecipientListBuilder recipientListBuilder = resolveRecipients(recipients);

      final List<String> recipientEmails = Lists.newArrayList();
      recipientEmails.addAll(recipientListBuilder.getEmailRecipients());

      MailOptions options = new MailOptions(recipientEmails, subject, body)
          .setFromEmail(alertsHandlerConfig.getFromAddress());

      if (overriddenConfig.isAllowHtml()) {
        options.setMsgContentType("text/html");
      }

      MailerHelper.mail(options, config);

    } catch (IOException e) {
      LOG.error("Error sending alert email", e);
      throw new RuntimeException(e);
    } finally {
      LOG.info("Alert email contents:\nTo:{}\nSubject:{}\n\n{}",
          new Object[]{recipients, subject, body});
    }
  }
}
