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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.Error;
import org.icgc.dcc.portal.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Provider
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final boolean SEND_EMAIL_ASYNC = true;
  private static final Random RANDOM = new Random();

  @NonNull
  private final MailService mailService;

  @Context
  private HttpHeaders headers;
  @Context
  private HttpServletRequest request;

  private static Response buildErrorResponse(@NonNull final ResponseBuilder builder, @NonNull final Error error) {
    return builder.type(APPLICATION_JSON_TYPE)
        .entity(error)
        .build();
  }

  @Override
  @SneakyThrows
  public Response toResponse(Throwable t) {
    val id = randomId();

    if (t instanceof WebApplicationException) {
      val response = ((WebApplicationException) t).getResponse();
      val responseBuilder = Response.fromResponse(response);
      val statusCode = response.getStatus();

      val ok = statusCode < 400;
      if (ok) {
        return responseBuilder.build();
      } else {
        logException(id, t);

        if (statusCode >= 500) {
          sendEmail(id, t);
        }

        return buildErrorResponse(responseBuilder, webErrorResponse(t, id, statusCode));
      }
    }

    logException(id, t);
    sendEmail(id, t);

    return buildErrorResponse(serverError(), errorResponse(t, id));
  }

  private Error webErrorResponse(Throwable t, final long id, final int statusCode) {
    return new Error(statusCode, t.getMessage());
  }

  private Error errorResponse(Throwable t, final long id) {
    return new Error(INTERNAL_SERVER_ERROR, formatResponseEntity(id, t));
  }

  protected void logException(long id, Throwable t) {
    log.error(formatLogMessage(id, t), t);
  }

  protected String formatResponseEntity(long id, Throwable t) {
    val message =
        "There was an error processing your request, with the message of '%s'. It has been logged (ID %016x).\n";
    return String.format(message, t.getMessage(), id);
  }

  protected String formatLogMessage(long id, Throwable t) {
    val message = "Error handling a request: %016x, with the message of '%s'.";
    return String.format(message, id, t.getMessage());
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

      mailService.sendEmail(subject, message, SEND_EMAIL_ASYNC);
    } catch (Exception e) {
      log.error("Exception mailing:", e);
    }
  }

}
