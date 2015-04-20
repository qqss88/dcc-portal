package org.icgc.dcc.portal.resource;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.icgc.dcc.portal.util.AuthUtils.createSessionCookie;
import static org.icgc.dcc.portal.util.AuthUtils.deleteCookie;
import static org.icgc.dcc.portal.util.AuthUtils.stringToUuid;
import static org.icgc.dcc.portal.util.AuthUtils.throwAuthenticationException;

import java.util.AbstractMap.SimpleImmutableEntry;
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

import org.icgc.dcc.common.client.api.ICGCException;
import org.icgc.dcc.common.client.api.cms.CMSClient;
import org.icgc.dcc.common.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.config.PortalProperties.CrowdProperties;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.AuthService;
import org.icgc.dcc.portal.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

@Component
@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class AuthResource extends BaseResource {

  private static final String DACO_ACCESS_KEY = "daco";
  private static final String TOKEN_KEY = "token";
  private static final String PASSWORD_KEY = "password";
  private static final String USERNAME_KEY = "username";

  @NonNull
  private final AuthService authService;
  @NonNull
  private final SessionService sessionService;
  @NonNull
  private final CMSClient cmsClient;

  /**
   * This is only used by UI.
   */
  @GET
  @Path("/verify")
  public Response verify(
      @CookieParam(value = CrowdProperties.SESSION_TOKEN_NAME) String sessionToken,
      @CookieParam(value = CrowdProperties.CUD_TOKEN_NAME) String cudToken,
      @CookieParam(value = CrowdProperties.CMS_TOKEN_NAME) String cmsToken) {
    log.info("Received an authorization request. Session token: '{}'. CUD token: '{}'", sessionToken, cudToken);

    // Already logged in and knows credentials
    if (sessionToken != null) {
      log.info("[{}] Looking for already authenticated user in the cache", sessionToken);
      val user = getAuthenticatedUser(sessionToken);
      val verifiedResponse = verifiedResponse(user);
      log.info("[{}] Finished authorization for user '{}'. DACO access: '{}'",
          new Object[] { sessionToken, user.getOpenIDIdentifier(), user.getDaco() });

      return verifiedResponse;
    }

    if (!isNullOrEmpty(cudToken) || !isNullOrEmpty(cmsToken)) {
      log.info("[{}, {}] The user has been authenticated by the ICGC authenticator.", cudToken, cmsToken);
      val tokenUserEntry = resolveIcgcUser(cudToken, cmsToken);
      val token = tokenUserEntry.getKey();
      val icgcUser = tokenUserEntry.getValue();

      val dccUser = createUser(icgcUser.getUserName(), token);
      val verifiedResponse = verifiedResponse(dccUser);
      log.info("[{}] Finished authorization for user '{}'. DACO access: '{}'",
          new Object[] { token, icgcUser.getUserName(), dccUser.getDaco() });

      return verifiedResponse;
    }

    val userMessage = "Authorization failed due to missing token";
    val logMessage = "Couldn't authorize the user. No session token found.";
    val invalidateCookie = true;
    throwAuthenticationException(userMessage, logMessage, invalidateCookie);

    // Will not come to this point because of throwAuthenticationException()
    return null;
  }

  private SimpleImmutableEntry<String, org.icgc.dcc.common.client.api.cud.User> resolveIcgcUser(String cudToken,
      String cmsToken) {
    org.icgc.dcc.common.client.api.cud.User user = null;
    String token = null;

    log.debug("[{}, {}] Looking for user info in the CUD", cudToken, cmsToken);
    try {
      if (!isNullOrEmpty(cudToken)) {
        user = authService.getCudUserInfo(cudToken);
        token = cudToken;
      } else {
        user = cmsClient.getUserInfo(cmsToken);
        token = cmsToken;
      }
    } catch (ICGCException e) {
      log.warn("[{}, {}] Failed to authorize ICGC user. Exception: {}",
          new Object[] { cudToken, cmsToken, e.getMessage() });
      throwAuthenticationException("Authorization failed due to expired token", true);
    }

    log.debug("[{}] Retrieved user information: {}", token, user);

    return new SimpleImmutableEntry<String, org.icgc.dcc.common.client.api.cud.User>(token, user);
  }

  /**
   * Gets already authenticated user.
   * 
   * @throws AuthenticationException
   */
  private User getAuthenticatedUser(String sessionToken) {
    val token = stringToUuid(sessionToken);
    val tempUserOptional = sessionService.getUserBySessionToken(token);

    if (!tempUserOptional.isPresent()) {
      throwAuthenticationException(
          "Authentication failed due to no User matching session token: " + sessionToken,
          String.format("[%s] Could not find any user in the cache. The session must have expired.", sessionToken),
          true);
    }
    log.info("[{}] Found user in the cache: {}", sessionToken, tempUserOptional.get());

    return tempUserOptional.get();
  }

  /**
   * Create a valid session user or throws {@link AuthenticationException} in case of failure.
   * 
   * @throws AuthenticationException
   */
  private User createUser(String userName, String cudToken) {
    val sessionToken = randomUUID();
    val sessionTokenString = cudToken == null ? sessionToken.toString() : cudToken;
    log.info("[{}] Creating and persisting user '{}' in the cache.", sessionTokenString, userName);
    val user = new User(null, sessionToken);
    user.setEmailAddress(userName);
    log.debug("[{}] Created user: {}", sessionTokenString, user);

    try {
      log.debug("[{}] Checking if the user has the DACO access", sessionTokenString);
      if (authService.hasDacoAccess(userName, UserType.CUD)) {
        log.info("[{}] Granted DACO access to the user", sessionTokenString);
        user.setDaco(true);
      }
    } catch (Exception e) {
      throwAuthenticationException("Failed to grant DACO access to the user",
          format("[%s] Failed to grant DACO access to the user. Exception: %s", sessionTokenString, e.getMessage()));
    }

    log.debug("[{}] Saving the user in the cache", sessionTokenString);
    sessionService.putUser(sessionToken, user);
    log.debug("[{}] Saved the user in the cache", sessionTokenString);

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

    log.info("[{}] Checking if the user has been already authenticated.", username);
    val userOptional = sessionService.getUserByEmail(username);
    if (userOptional.isPresent()) {
      log.info("[{}] The user is already authenticated.", username);

      return verifiedResponse(userOptional.get());
    }

    // Login user.
    try {
      log.info("[{}] The user is not authenticated yet. Authenticating...", username);
      authService.loginUser(username, creds.get(PASSWORD_KEY));
    } catch (ICGCException e) {
      throwAuthenticationException("Username and password are incorrect",
          String.format("[%s] Failed to login the user. Exception %s", username, e.getMessage()));
    }

    return verifiedResponse(createUser(username, null));
  }

  @POST
  @Path("/logout")
  public Response logout(@Context HttpServletRequest request) {
    val sessionToken = getSessionToken(request);
    log.info("[{}] Terminating session", sessionToken);

    if (sessionToken != null) {
      val userOptional = sessionService.getUserBySessionToken(sessionToken);

      if (userOptional.isPresent()) {
        sessionService.removeUser(userOptional.get());
      }
      log.info("[{}] Successfully terminated session", sessionToken);

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
    return cookie.getName().equals(CrowdProperties.SESSION_TOKEN_NAME);
  }

  private static Response verifiedResponse(User user) {
    log.debug("Creating successful verified response for user: {}", user);
    val cookie = createSessionCookie(CrowdProperties.SESSION_TOKEN_NAME, user.getSessionToken().toString());

    return Response.ok(ImmutableMap.of(TOKEN_KEY, user.getSessionToken(), USERNAME_KEY, user.getEmailAddress(),
        DACO_ACCESS_KEY, user.getDaco()))
        .header(SET_COOKIE, cookie.toString())
        .build();
  }

  private static Response createLogoutResponse(Status status, String message) {
    val dccCookie = deleteCookie(CrowdProperties.SESSION_TOKEN_NAME);
    val crowdCookie = deleteCookie(CrowdProperties.CUD_TOKEN_NAME);

    return status(status)
        .header(SET_COOKIE, dccCookie.toString())
        .header(SET_COOKIE, crowdCookie.toString())
        .entity(new org.icgc.dcc.portal.model.Error(status, message))
        .build();
  }

}
