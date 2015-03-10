/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static javax.mail.Transport.send;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.config.PortalProperties.MailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailService {

  /**
   * Constants.
   */
  public static final String SMTP_CONFIG_KEY_HOST = "mail.smtp.host";
  public static final String SMTP_CONFIG_KEY_PORT = "mail.smtp.port";

  private static final Splitter EMAIL_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  /**
   * Instance variables
   */
  private InternetAddress emailSender = null;
  private Collection<InternetAddress> recipients = null;
  private Properties smtpConfig = null;

  @NonNull
  private final MailProperties config;

  /*
   * Public methods
   */
  public void sendEmail(final String subject, final String message, final boolean async) {
    if (dontSendEmail()) {
      return;
    }

    final Runnable runnable = new Runnable() {

      @Override
      public void run() {
        try {
          val emailMessage = buildEmailMessage(subject, message);
          send(emailMessage);
        } catch (Exception e) {
          log.error("An error occured while emailing: ", e);
        }
      }

    };

    sendNow(runnable, async);
  }

  /*
   * Static helpers
   */
  private static Collection<InternetAddress> tryParseInternetAddresses(Iterable<String> addresses) {
    val result = new ArrayList<InternetAddress>();

    for (val address : addresses) {
      try {
        val internetAddress = new InternetAddress(address);
        result.add(internetAddress);
      } catch (Exception e) {
        log.error("Invalid email address: '{}'", address);
      }
    }

    return result;
  }

  private static Collection<InternetAddress> parseEmailAddresses(final String emailAddresses) {
    checkArgument(!isNullOrEmpty(emailAddresses), "The 'emailAddresses' argument must not be empty or null.");

    val addresses = EMAIL_SPLITTER.split(emailAddresses);
    return tryParseInternetAddresses(newHashSet(addresses));
  }

  private static MimeMessage createMessage(final Properties smtpConfig, final InternetAddress emailSender,
      final Iterable<InternetAddress> emailRecipients) throws MessagingException {
    val session = Session.getDefaultInstance(smtpConfig, null);

    val result = new MimeMessage(session);
    result.setFrom(emailSender);

    for (val recipient : emailRecipients) {
      result.addRecipient(RecipientType.TO, recipient);
    }

    return result;
  }

  private static void sendNow(final Runnable runnable, final boolean isAsync) {
    if (isAsync) {
      new Thread(runnable).start();
    } else {
      runnable.run();
    }
  }

  /*
   * Helpers to lazily load various settings
   */
  private Properties getEmailConfig() {
    if (null == smtpConfig) {
      val smtpServer = config.getSmtpServer();
      val smtpPort = config.getSmtpPort();

      val props = new Properties();
      props.put(SMTP_CONFIG_KEY_HOST, smtpServer);
      props.put(SMTP_CONFIG_KEY_PORT, smtpPort);

      smtpConfig = props;
    }
    return smtpConfig;
  }

  private Iterable<InternetAddress> getEmailRecipients() {
    if (null == recipients) {
      val emailAddressSetting = config.getRecipientEmail();
      val emailAddresses = parseEmailAddresses(emailAddressSetting);

      log.info("Email recipients are: {}.", emailAddresses);
      checkState(emailAddresses.size() > 0, "Error parsing any recipient email addresses from config: "
          + emailAddressSetting);

      recipients = emailAddresses;
    }

    return recipients;
  }

  private InternetAddress getEmailSender() throws UnsupportedEncodingException {
    if (null == emailSender) {
      val senderEmail = config.getSenderEmail();
      val senderName = config.getSenderName();

      emailSender = new InternetAddress(senderEmail, senderName);
    }

    return emailSender;
  }

  private Message buildEmailMessage(final String subject, final String emailBody)
      throws UnsupportedEncodingException, MessagingException {
    val message = createMessage(getEmailConfig(), getEmailSender(), getEmailRecipients());

    message.setSubject(subject);
    message.setText(emailBody);

    return message;
  }

  private boolean dontSendEmail() {
    return !config.isEnabled();
  }

}
