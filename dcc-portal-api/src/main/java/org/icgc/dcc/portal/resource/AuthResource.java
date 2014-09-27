package org.icgc.dcc.portal.resource;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.icgc.dcc.core.util.FormatUtils._;
import static org.icgc.dcc.portal.config.CrowdConfiguration.CUD_TOKEN_NAME;
import static org.icgc.dcc.portal.config.CrowdConfiguration.SESSION_TOKEN_NAME;
import static org.icgc.dcc.portal.util.AuthUtils.createSessionCookie;
import static org.icgc.dcc.portal.util.AuthUtils.deleteCookie;
import static org.icgc.dcc.portal.util.AuthUtils.stringToUuid;
import static org.icgc.dcc.portal.util.AuthUtils.throwAuthenticationException;

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.icgc.client.api.ICGCAccessException;
import org.icgc.dcc.icgc.client.api.ICGCException;
import org.icgc.dcc.icgc.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.AuthService;
import org.icgc.dcc.portal.service.DistributedCacheService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class AuthResource extends BaseResource {

  private static final String DACO_ACCESS_KEY = "daco";
  private static final String TOKEN_KEY = "token";
  private static final String PASSWORD_KEY = "password";
  private static final String USERNAME_KEY = "username";

  @NonNull
  private final AuthService authService;
  @NonNull
  private final DistributedCacheService cacheService;

  /**
   * This is only used by UI.
   */
  @GET
  @Path("/verify")
  public Response verify(
      @CookieParam(value = SESSION_TOKEN_NAME) String sessionToken,
      @CookieParam(value = CUD_TOKEN_NAME) String cudToken) {

    // Already logged in and knows credentials
    if (sessionToken != null) {
      return verifiedResponse(getAuthenticatedUser(sessionToken));
    }

    // Authenticated by the ICGC SSO authenticator
    if (!isNullOrEmpty(cudToken)) {
      try {
        val cudUser = authService.getCudUserInfo(cudToken);

        return verifiedResponse(createUser(cudUser.getUserName()));
      } catch (ICGCAccessException e) {
        throwAuthenticationException("Authentication failed due to expired token", true);
      }
    }

    throwAuthenticationException("Authentication failed due to missing token", true);

    // Will not come to this point because of throwAuthenticationException()
    return null;
  }

  /**
   * Gets already authenticated user.
   * 
   * @throws AuthenticationException
   */
  private User getAuthenticatedUser(String sessionToken) {
    val token = stringToUuid(sessionToken);
    log.info("Verifying if user with session token '{}' already logged in.", sessionToken);
    val tempUserOptional = cacheService.getUserBySessionToken(token);

    if (!tempUserOptional.isPresent()) {
      throwAuthenticationException(
          "Authentication failed due to no User matching session token: " + sessionToken,
          _("Session token '%s' has expired.", sessionToken),
          true);
    }

    log.info("Found active user by session token '{}'", sessionToken);

    return tempUserOptional.get();
  }

  /**
   * Create a valid session user or throws {@link AuthenticationException} in case of failure.
   * 
   * @throws AuthenticationException
   */
  private User createUser(String userName) {
    val sessionToken = randomUUID();
    val user = new User(null, sessionToken);
    user.setEmailAddress(userName);

    try {
      if (authService.hasDacoAccess(userName, UserType.USERNAME)) {
        log.debug("Granted DACO access to user '{}'", userName);
        user.setDaco(true);
      }
    } catch (ICGCException e) {
      throwAuthenticationException("Failed to grant DACO access", e.getMessage());
    }

    cacheService.putUser(sessionToken, user);

    return user;
  }

  /**
   * This is only used by command-line utilities whose principal is not tied to OpenID, but rather CUD authentication.
   * It is not used by the UI.
   */
  @POST
  @Path("/login")
  public Response login(Map<String, String> creds) {
    val username = creds.get(USERNAME_KEY);
    log.info("Logging into CUD as {}", username);

    log.info("Checking if user '{}' has already authenticated.", username);
    val userOptional = cacheService.getUserByEmail(username);
    if (userOptional.isPresent()) {
      log.info("User '{}' is already authenticated.", username);

      return verifiedResponse(userOptional.get());
    }

    // Login user.
    try {
      authService.loginUser(username, creds.get(PASSWORD_KEY));
    } catch (ICGCException e) {
      throwAuthenticationException("Username and password are incorrect");
    }

    return verifiedResponse(createUser(username));
  }

  @POST
  @Path("/logout")
  public Response logout(@Context HttpServletRequest request) {
    val sessionToken = getSessionToken(request);

    if (sessionToken != null) {
      val userOptional = cacheService.getUserBySessionToken(sessionToken);

      if (userOptional.isPresent()) {
        cacheService.removeUser(userOptional.get());
      }

      log.info("Successfully terminated session {}", sessionToken);

      return createLogoutResponse(OK, "");
    }

    return createLogoutResponse(NOT_MODIFIED, "Did not find a user to log out");
  }

  /**
   * Extracts session token from cookies or HTTP header.
   * 
   * @return sessionToken or <tt>null</tt> if no token found
   */
  private static UUID getSessionToken(HttpServletRequest request) {
    val headerToken = request.getHeader("X-Auth-Token");

    for (val cookie : request.getCookies()) {
      if (isSessionTokenCookie(cookie)) {
        return stringToUuid(cookie.getValue());
      }
    }

    if (headerToken != null) {
      return stringToUuid(headerToken);
    }

    return null;
  }

  private static boolean isSessionTokenCookie(Cookie cookie) {
    return cookie.getName().equals(SESSION_TOKEN_NAME);
  }

  private static Response verifiedResponse(User user) {
    val cookie = createSessionCookie(SESSION_TOKEN_NAME, user.getSessionToken().toString());

    return Response.ok(ImmutableMap.of(TOKEN_KEY, user.getSessionToken(), USERNAME_KEY, user.getEmailAddress(),
        DACO_ACCESS_KEY, user.getDaco()))
        .header(SET_COOKIE, cookie.toString()).build();
  }

  private static Response createLogoutResponse(Status status, String message) {
    val dccCookie = deleteCookie(SESSION_TOKEN_NAME);
    val crowdCookie = deleteCookie(CUD_TOKEN_NAME);

    return status(status)
        .header(SET_COOKIE, dccCookie.toString())
        .header(SET_COOKIE, crowdCookie.toString())
        .entity(new org.icgc.dcc.portal.model.Error(status, message))
        .build();
  }

}
