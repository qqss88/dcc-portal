package org.icgc.dcc.portal.auth.openid;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.icgc.dcc.portal.util.AuthUtils.stringToUuid;
import static org.icgc.dcc.portal.util.AuthUtils.throwRedirectException;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.util.Scheme;
import org.icgc.dcc.icgc.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.resource.OpenIDResource;
import org.icgc.dcc.portal.service.AuthService;
import org.icgc.dcc.portal.service.AuthenticationException;
import org.icgc.dcc.portal.service.DistributedCacheService;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

import com.google.inject.Inject;

/**
 * The service provides utilities to perform OpenID authentication.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class OpenIDAuthService {

  public static final String OPENID_EMAIL = "http://schema.openid.net/contact/email";
  public static final String AXSCHEMA_EMAIL = "http://axschema.org/contact/email";

  public static final String YAHOO_ENDPOINT = "https://me.yahoo.com";
  public static final String GOOGLE_ENDPOINT = "https://www.google.com/accounts/o8/id";

  private static final int DEFAULT_HTTP_PORT = 80;
  private static final String DEFAULT_USER_MESSAGE = "An error occurred while trying to log in.";

  @NonNull
  private final DistributedCacheService cacheService;
  @NonNull
  private final ConsumerManager consumerManager;
  @NonNull
  private final AuthService authService;

  /**
   * Prepares an authentication request which the user sends to the OpenID provider.
   * 
   * @return AuthRequest message to be sent to the OpenID provider
   * @throws MessageException
   * @throws ConsumerException
   * @throws DiscoveryException
   */
  public AuthRequest createAuthRequest(String serverName, int serverPort, String identifier, String currentUrl)
      throws Exception {

    // This token will tag along to the response from the provider and allows us to link the response to a user
    val sessionToken = UUID.randomUUID();

    // The OpenID provider will use this endpoint to provide authentication
    val returnToUrl = formatReturnToUrl(serverName, serverPort, sessionToken, currentUrl);
    log.info("Return to URL '{}'", returnToUrl);

    // Perform discovery on the user-supplied identifier
    List<?> discoveries = consumerManager.discover(identifier);

    // Attempt to associate with the OpenID provider and retrieve one service endpoint for authentication
    val discoveryInfo = consumerManager.associate(discoveries);

    // Persist the discovery info in the cache for further verification
    cacheService.putDiscoveryInfo(sessionToken, discoveryInfo);

    // Build the AuthRequest message to be sent to the OpenID provider
    val authRequest = consumerManager.authenticate(discoveryInfo, returnToUrl);

    // Build the FetchRequest containing the information to be copied from the OpenID provider
    val fetch = FetchRequest.createFetchRequest();

    // Request email from the OpenID provider
    fetch.addAttribute("email", getEmailSchema(identifier), true);

    // Attach the extension to the authentication request
    authRequest.addExtension(fetch);

    return authRequest;
  }

  /**
   * Verifies a response received from the OpenID Provider. Creates and persists a session User in case of success.
   * 
   * @param sessionToken identifies users during authentication and HTTP sessions.
   * @return an authenticated User
   */
  public User verify(String sessionToken, String receivingUrl, ParameterList parameterList, URI redirect) {
    val verification = verifyProviderResponse(sessionToken, receivingUrl, parameterList, redirect);

    return createUser(sessionToken, verification, redirect);
  }

  /**
   * Verifies OP response.
   * 
   * @throws AuthenticationException
   */
  private VerificationResult verifyProviderResponse(String sessionToken, String receivingUrl,
      ParameterList parameterList, URI redirect) {

    checkState(!isNullOrEmpty(sessionToken), "Null or empty token", redirect);
    val sessionTokenUuid = stringToUuid(sessionToken, redirect);

    val discoveryInfoOptional = cacheService.getDiscoveryInfo(sessionTokenUuid);
    cacheService.removeDiscoveryInfo(sessionTokenUuid);

    checkState(discoveryInfoOptional.isPresent(), "Authentication failed due to no discovery information matching "
        + "session token " + sessionToken, redirect);
    val discoveryInfo = discoveryInfoOptional.get();

    try {
      return consumerManager.verify(receivingUrl, parameterList, discoveryInfo);
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new AuthenticationException(DEFAULT_USER_MESSAGE, false, redirect);
    }
  }

  /**
   * Creates a {@link User}.
   * 
   * @param sessionToken tags temporary authentication information
   * @return authenticated session user
   * @throws AuthenticationException if user creation failed
   */
  private User createUser(String sessionToken, VerificationResult verification, URI redirect) {
    // AKA OpenID URL or XRI
    val userIdentifier = verification.getVerifiedId();
    checkState(userIdentifier != null, "Received null user identifier", redirect);

    // sessionToken is not generated as it's set couple steps later
    User user = new User();
    user.setOpenIDIdentifier(userIdentifier.getIdentifier());
    checkState(setUserEmail(verification, user), "Failed to set user's email", redirect);

    val sessionTokenUuid = stringToUuid(sessionToken, redirect);
    user = configureDacoAccess(sessionTokenUuid, checkOtherSessions(user, redirect), redirect);
    user.setSessionToken(sessionTokenUuid);

    // Persist the user in the cache with the authentication token
    cacheService.putUser(sessionTokenUuid, user);

    return user;
  }

  private static boolean setUserEmail(VerificationResult verification, User user) {
    val authSuccess = (AuthSuccess) verification.getAuthResponse();

    if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
      try {
        user.setEmailAddress(extractEmailAddress(authSuccess));
      } catch (MessageException e) {
        log.info(e.getMessage());

        return false;
      }
    }

    return true;
  }

  /**
   * Checks and grant DACO access in case the <tt>user</tt> has it.
   * 
   * @throws AuthenticationException if DACO access check fails
   */
  private User configureDacoAccess(UUID authToken, User user, URI redirect) {
    try {
      val hasEmail = !isNullOrEmpty(user.getEmailAddress());
      val hasIdentifier = !isNullOrEmpty(user.getOpenIDIdentifier());

      if (hasEmail && authService.hasDacoAccess(user.getEmailAddress(), UserType.OPENID)) {
        user.setDaco(true);
      } else if (hasIdentifier && authService.hasDacoAccess(user.getOpenIDIdentifier(), UserType.OPENID)) {
        user.setDaco(true);
      }

    } catch (NoSuchElementException e) {
      throwRedirectException(DEFAULT_USER_MESSAGE, "Failed to check DACO access settings for the user", redirect);
    }

    return user;
  }

  /**
   * Checks if there is another user with same credentials in the cache. Updates <tt>user's</tt> info in case this is a
   * re-registration.
   * 
   * @param lookupUser used to search for already logged in users
   * @throws AuthenticationException if no user found
   */
  private User checkOtherSessions(User lookupUser, URI redirect) {
    // Search for already logged-in user (another browser?)
    val userByIdentifierOptional = cacheService.getUserByOpenidIdentifier(lookupUser.getOpenIDIdentifier());
    if (userByIdentifierOptional.isPresent()) {
      log.info("Found an existing User using OpenID identifier {}", lookupUser);

      return userByIdentifierOptional.get();
    }

    // This is either a new registration or the user's OpenID URL(XRI) has changed
    if (lookupUser.getEmailAddress() != null) {
      val userOptional = cacheService.getUserByEmail(lookupUser.getEmailAddress());
      if (!userOptional.isPresent()) {
        log.info("No authenticated users found. Registering a new {}", lookupUser);

        return lookupUser;
      } else {
        // The user's OpenID URL(XRI) has changed so update it
        log.info("Updating user's OpenID URL(XRI) for {}", lookupUser);
        val newUser = userOptional.get();
        newUser.setOpenIDIdentifier(lookupUser.getOpenIDIdentifier());

        return newUser;
      }
    }

    // No email address to use as backup
    throwRedirectException(DEFAULT_USER_MESSAGE, "Rejecting valid authentication. No email address for "
        + lookupUser, redirect);

    // Unreachable because of throwAuthenticationException()
    return lookupUser;
  }

  private static String extractEmailAddress(AuthSuccess authSuccess) throws MessageException {
    val fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
    return getAttributeValue(
        fetchResp,
        "email",
        "",
        String.class);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <T> T getAttributeValue(FetchResponse fetchResponse, String attribute, T defaultValue, Class<T> clazz) {
    val list = fetchResponse.getAttributeValues(attribute);
    if (list != null && !list.isEmpty()) {
      return (T) list.get(0);
    }

    return defaultValue;
  }

  private static String getEmailSchema(String identifier) {
    if (identifier.startsWith(GOOGLE_ENDPOINT) || identifier.startsWith(YAHOO_ENDPOINT)) {
      return AXSCHEMA_EMAIL;
    } else {
      // FIXME Does not work for VeriSign. It seems it does not support Attributes Exchange.
      return OPENID_EMAIL;
    }
  }

  private static String formatReturnToUrl(String hostname, int port, UUID sessionToken, String returnTo) {

    UriBuilder redirectBuilder;
    UriBuilder returnBuilder;
    if (port == DEFAULT_HTTP_PORT) {
      redirectBuilder = UriBuilder.fromPath("/").scheme(Scheme.HTTP.getId());
      returnBuilder = UriBuilder.fromPath("api").scheme(Scheme.HTTP.getId());
    } else {
      redirectBuilder = UriBuilder.fromPath("/").scheme(Scheme.HTTPS.getId()).port(port);
      returnBuilder = UriBuilder.fromPath("api").scheme(Scheme.HTTPS.getId()).port(port);
    }

    redirectBuilder.host(hostname)
        .path(returnTo);

    returnBuilder.host(hostname)
        .path(OpenIDResource.class)
        .path("verify")
        .queryParam("token", sessionToken)
        .queryParam("redirect", redirectBuilder.build().toString());

    val returnToURLStr = returnBuilder.build().toString();
    log.info("Return to URL: {}", returnToURLStr);

    return returnToURLStr;
  }

  /**
   * Ensures the <tt>expression</tt> is <tt>true</tt>. Throws {@link AuthenticationException} and log
   * <tt>logMessage</tt> if <tt>expression</tt> is <tt>false</tt>
   * 
   * @throws AuthenticationException if <tt>expression</tt> is <tt>false</tt>
   */
  private static void checkState(boolean expression, String logMessage, URI redirect) {
    if (!expression) throwRedirectException(DEFAULT_USER_MESSAGE, logMessage, redirect);
  }

}
