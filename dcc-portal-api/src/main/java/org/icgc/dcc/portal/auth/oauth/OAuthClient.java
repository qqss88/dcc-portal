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
package org.icgc.dcc.portal.auth.oauth;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.sun.jersey.api.client.Client.create;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.icgc.dcc.portal.config.PortalProperties.OAuthProperties;
import org.icgc.dcc.portal.model.AccessToken;
import org.icgc.dcc.portal.model.Tokens;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Slf4j
@Component
public class OAuthClient {

  /**
   * Constants.
   */
  private static final String PASSWORD_GRANT_TYPE = "password";
  private static final String SCOPE_PARAM = "scope";
  private static final String USERNAME_PARAM = "username";
  private static final String GRANT_TYPE_PARAM = "grant_type";
  private static final String SCOPE_SEPARATOR = " ";
  private static final String TOKENS_URL = "tokens";
  private static final String CREATE_TOKEN_URL = "oauth/token";

  private static final Joiner SPACE_JOINER = Joiner.on(SCOPE_SEPARATOR);

  private final String clientId;
  private final WebResource resource;

  @Autowired
  public OAuthClient(@NonNull OAuthProperties config) {
    val jerseyClient = create(getClientConfig(config));
    configureFilters(jerseyClient, config);

    this.resource = jerseyClient.resource(config.getServiceUrl());
    this.clientId = config.getClientId();
  }

  public AccessToken createToken(@NonNull String userId, @NonNull String... scope) {
    checkArguments(userId);
    val response = resource.path(CREATE_TOKEN_URL)
        .type(APPLICATION_FORM_URLENCODED_TYPE)
        .accept(APPLICATION_JSON_TYPE)
        .post(ClientResponse.class, createParameters(userId, scope));
    validateResponse(response);
    val accessToken = response.getEntity(AccessTokenInternal.class);

    return convertToAccessToken(accessToken);
  }

  public Tokens listTokens(@NonNull String userId) {
    return listTokens(clientId, userId);
  }

  public Tokens listTokens(@NonNull String clientId, @NonNull String userId) {
    checkArguments(userId, clientId);
    val response = resource.path(TOKENS_URL).path(clientId).path(userId).get(ClientResponse.class);
    validateResponse(response);

    return response.getEntity(Tokens.class);
  }

  public void revokeToken(@NonNull String token) {
    checkArguments(token);
    val response = resource.path(TOKENS_URL).path(token).delete(ClientResponse.class);
    validateResponse(response);
  }

  private static MultivaluedMapImpl createParameters(String userId, String... scope) {
    val params = new MultivaluedMapImpl();
    params.add(GRANT_TYPE_PARAM, PASSWORD_GRANT_TYPE);
    params.add(USERNAME_PARAM, userId);
    params.add(SCOPE_PARAM, convertScope(scope));

    return params;
  }

  private static void validateResponse(ClientResponse response) {
    checkState(response.getClientResponseStatus() == OK);
  }

  private static ClientConfig getClientConfig(OAuthProperties config) {
    val cc = new DefaultClientConfig();
    cc.getFeatures().put(FEATURE_POJO_MAPPING, TRUE);
    cc.getClasses().add(JacksonJsonProvider.class);

    return configureSSLCertificatesHandling(cc, config);
  }

  @SneakyThrows
  private static ClientConfig configureSSLCertificatesHandling(ClientConfig config, OAuthProperties oauthConfig) {
    if (!oauthConfig.isEnableStrictSSL()) {
      log.debug("Setting up SSL context");
      val context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] {
          new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

          } },
          null);

      config.getProperties().put(PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
          new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
              return true;
            }

          }, context
          ));
    }

    return config;
  }

  private static void configureFilters(Client jerseyClient, OAuthProperties config) {
    jerseyClient.addFilter(new HTTPBasicAuthFilter(config.getClientId(), config.getClientSecret()));

    if (config.isEnableHttpLogging()) {
      jerseyClient.addFilter(new LoggingFilter());
    }
  }

  private static AccessToken convertToAccessToken(AccessTokenInternal token) {
    return new AccessToken(token.getId(), token.getExpiresIn(), convertScope(token.getScope()));
  }

  private static Set<String> convertScope(String scope) {
    return copyOf(scope.split(SCOPE_SEPARATOR));
  }

  private static String convertScope(String[] scopes) {
    return SPACE_JOINER.join(scopes);
  }

  private static void checkArguments(String... args) {
    for (val arg : args) {
      checkArgument(!isNullOrEmpty(arg));
    }
  }

}
