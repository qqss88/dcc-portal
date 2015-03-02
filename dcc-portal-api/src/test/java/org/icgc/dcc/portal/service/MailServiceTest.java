/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.portal.config.PortalProperties.MailProperties;
import org.junit.Test;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

/**
 * Test suite for MailService.
 */
public class MailServiceTest {

  private final static String SMTP_HOST = "localhost";
  private final static int SMTP_PORT = 2525;
  private final static String SUBJECT_HEADER = "Subject";

  private final static MailProperties mailConfig = new MailProperties();
  private final static MailService mailService;

  static {
    mailConfig.setEnabled(true);
    mailConfig.setSmtpServer(SMTP_HOST);
    mailConfig.setSmtpPort(SMTP_PORT);

    mailService = new MailService(mailConfig);
  }

  @Test
  public void testSend() {
    val emailServer = SimpleSmtpServer.start(SMTP_PORT);

    val emailSubject = "TEST - please ignore";
    val emailBody = "foo";
    val async = false;

    mailService.sendEmail(emailSubject, emailBody, async);

    emailServer.stop();

    assertThat(emailServer.getReceivedEmailSize()).isEqualTo(1);

    val email = (SmtpMessage) emailServer.getReceivedEmail().next();

    assertThat(email.getHeaderValue(SUBJECT_HEADER)).isEqualTo(emailSubject);
    assertThat(email.getBody()).isEqualTo(emailBody);
  }
}
