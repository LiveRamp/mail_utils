package com.liveramp.mail_utils;

import java.time.Duration;

public class SmtpConfig {
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5l);
  private final String host;
  private final Duration timeout;

  public SmtpConfig(String host, Duration timeout) {
    this.host = host;
    this.timeout = timeout;
  }

  public SmtpConfig(String host) {
    this.host = host;
    this.timeout = DEFAULT_TIMEOUT;
  }

  public String getHost() {
    return host;
  }

  public long getTimeout() {
    return timeout.toMillis();
  }

}
