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

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.config.MailConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class MailService {

  /**
   * Constants.
   */
  public static final String SMTP_HOST = "mail.smtp.host";

  @NonNull
  private final MailConfiguration mailConfig;

  public void sendEmail(final String subject, final String message, boolean async) {
    if (!mailConfig.isEnabled()) {
      return;
    }

    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        try {
          Properties props = new Properties();
          props.put(SMTP_HOST, mailConfig.getSmtpServer());
          Session session = Session.getDefaultInstance(props, null);

          Message msg = new MimeMessage(session);
          msg.setFrom(new InternetAddress(mailConfig.getSenderEmail(), mailConfig.getSenderName()));
          msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mailConfig.getRecipientEmail()));
          msg.setSubject(subject);
          msg.setText(message);

          Transport.send(msg);
        } catch (Exception e) {
          log.error("An error occured while emailing: ", e);
        }
      }

    };

    if (async) {
      new Thread(runnable).start();
    } else {
      runnable.run();
    }

  }

}
