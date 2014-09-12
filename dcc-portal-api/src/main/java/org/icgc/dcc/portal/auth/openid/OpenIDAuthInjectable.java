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
package org.icgc.dcc.portal.auth.openid;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.icgc.dcc.portal.config.CrowdConfiguration.SESSION_TOKEN_NAME;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.yammer.dropwizard.auth.AuthenticationException;
import com.yammer.dropwizard.auth.Authenticator;

/**
 * An {@code Injectable} which provides the following to {@link OpenIDAuthProvider}:
 * <ul>
 * <li>Performs decode from HTTP request</li>
 * <li>Carries OpenID authentication data</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
class OpenIDAuthInjectable<T> extends AbstractHttpContextInjectable<T> {

  /**
   * The Authenticator that will compare credentials
   */
  @Getter
  @NonNull
  private final Authenticator<OpenIDCredentials, T> authenticator;

  /**
   * The authentication realm
   */
  @Getter
  @NonNull
  private final String realm;

  /**
   * Is an authenticated user required?
   */
  private final boolean required;

  @Override
  public T getValue(HttpContext httpContext) {
    UUID sessionToken = resolveSessionToken(httpContext);
    if (sessionToken != null) {
      val result = handleAuthentication(sessionToken);
      if (result != null) {
        return result;
      }
    }

    handleUnauthenticated();

    return null;
  }

  private static UUID resolveSessionToken(HttpContext httpContext) {
    List<String> headers = httpContext.getRequest().getRequestHeader("X-Auth-Token");
    Map<String, Cookie> cookies = httpContext.getRequest().getCookies();

    UUID token = null;

    try {
      if (cookies.containsKey(SESSION_TOKEN_NAME)) {
        token = UUID.fromString(cookies.get(SESSION_TOKEN_NAME).getValue());
      } else if (!headers.isEmpty()) {
        token = UUID.fromString(headers.get(0));
      }
    } catch (IllegalArgumentException e) {
      log.debug("Invalid token passed in request");
    } catch (NullPointerException e) {
      log.debug("No token passed in request");
    }

    return token;
  }

  private T handleAuthentication(UUID sessionToken) {
    val credentials = new OpenIDCredentials(sessionToken);

    try {
      val result = authenticator.authenticate(credentials);
      if (result.isPresent()) {
        return result.get();
      }
    } catch (AuthenticationException e) {
      log.warn("Problem authenticating '" + credentials + "':", e);
    }

    return null;
  }

  private void handleUnauthenticated() {
    if (required) {
      throw new WebApplicationException(Response.status(UNAUTHORIZED)
          .entity("A valid Session token is required to access this resource.")
          .type(TEXT_PLAIN_TYPE)
          .build());
    }
  }
}
