package com.liveramp.mail_utils;

import java.io.IOException;

import com.liveramp.java_support.alerts_handler.MailOptions;

public class EmailsHandlerImpl implements EmailsHandler {

  private final SmtpConfig smtpConfig;

  public EmailsHandlerImpl(SmtpConfig smtpConfig) {
    this.smtpConfig = smtpConfig;
  }

  @Override
  public void sendEmail(MailOptions options) throws IOException {
    MailerHelper.mail(options, smtpConfig);
  }
}


