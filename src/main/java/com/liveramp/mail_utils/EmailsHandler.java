package com.liveramp.mail_utils;

import java.io.IOException;

import com.liveramp.java_support.alerts_handler.MailOptions;

public interface EmailsHandler {

  public void sendEmail(MailOptions options) throws IOException;

}

