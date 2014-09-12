package org.icgc.dcc.portal.config;

import lombok.Getter;
import lombok.ToString;

import org.hibernate.validator.constraints.Email;

@Getter
@ToString
public class MailConfiguration {

  private final boolean enabled = false;

  private final String smtpServer = "***REMOVED***";

  @Email
  private final String senderEmail = "no-reply@oicr.on.ca";

  private final String senderName = "DCC Portal";

  @Email
  private final String recipientEmail = "***REMOVED***";

}
