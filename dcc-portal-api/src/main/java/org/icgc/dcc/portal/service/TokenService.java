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

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static org.icgc.dcc.portal.util.AuthUtils.throwForbiddenException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.auth.oauth.OAuthClient;
import org.icgc.dcc.portal.auth.oauth.Tokens;
import org.icgc.dcc.portal.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * OAuth access tokens management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class TokenService {

  @NonNull
  private final OAuthClient client;

  public String create(User user, String scope) {
    log.debug("Creating access token of scope '{}' for user '{}'...", scope, user);
    val userId = user.getEmailAddress();
    if (user.getDaco() == FALSE) {
      throwForbiddenException("The user is not DACO approved",
          format("User %s is not DACO approved to access the create access token resource", userId));
    }

    validateScope(user, scope);
    val token = client.createToken(userId, scope);
    log.debug("Created token '{}' for '{}'", token, userId);

    return token.getId();
  }

  public Tokens list(@NonNull User user) {
    return client.listTokens(user.getEmailAddress());
  }

  public void delete(@NonNull String tokenId) {
    client.revokeToken(tokenId);
  }

  public String userScopes(User user) {
    // FIXME: return JSON and correctly resolve scopes
    return "s3.download";
  }

  private void validateScope(User user, String scope) {
    // FIXME: ensure user is allowed to generate tokens of such a scope
  }

}
