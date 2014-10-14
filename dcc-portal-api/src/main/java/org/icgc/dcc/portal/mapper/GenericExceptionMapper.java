/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.mapper;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.Error;
import org.icgc.dcc.portal.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Provider
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Random RANDOM = new Random();

  @NonNull
  private final MailService mailService;

  @Context
  private HttpHeaders headers;
  @Context
  private HttpServletRequest request;

  @Override
  public Response toResponse(Throwable t) {
    if (t instanceof WebApplicationException) {
      return ((WebApplicationException) t).getResponse();
    }

    val id = randomId();
    logException(id, t);
    sendEmail(id, t);

    return serverError()
        .type(headers.getMediaType())
        .entity(errorResponse(t, id))
        .build();
  }

  private Error errorResponse(Throwable t, final long id) {
    return new Error(INTERNAL_SERVER_ERROR, formatResponseEntity(id, t));
  }

  protected void logException(long id, Throwable t) {
    log.error(formatLogMessage(id, t), t);
  }

  protected String formatResponseEntity(long id, Throwable exception) {
    return String.format("There was an error processing your request. It has been logged (ID %016x).\n", id);
  }

  protected String formatLogMessage(long id, Throwable exception) {
    return String.format("Error handling a request: %016x", id);
  }

  protected static long randomId() {
    return RANDOM.nextLong();
  }

  protected void sendEmail(long id, Throwable t) {
    try {
      val subject = "DCC Portal - Exception @ " + new Date();
      val message =
          request.getRemoteHost() + " " + request + "\n\n" + formatLogMessage(id, t) + "\n\n"
              + getStackTraceAsString(t);
      val async = true;

      mailService.sendEmail(subject, message, async);
    } catch (Exception e) {
      log.error("Exception mailing:", e);
    }
  }

}
