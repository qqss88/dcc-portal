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
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.portal.auth.oauth.AccessToken;
import org.icgc.dcc.portal.auth.oauth.OAuthClient;
import org.icgc.dcc.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {

  /**
   * Scope supported by default. <br>
   * // TODO: Make sure scope is correctly resolved.
   */
  private static final String SCOPE = "s3.download";
  private static final String USER_ID = "userId";
  private static final String TOKEN_ID = "123";
  private static final int EXPIRES = 10;

  TokenService tokenService;

  @Mock
  OAuthClient client;

  @Before
  public void setUp() {
    tokenService = new TokenService(client);
  }

  @Test
  public void createTest_successful() {
    when(client.createToken(USER_ID, SCOPE)).thenReturn(createAccessToken());
    val result = tokenService.create(createUser(USER_ID, TRUE), SCOPE);
    assertThat(result).isEqualTo(TOKEN_ID);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void createTest_noDaco() {
    tokenService.create(createUser(USER_ID, FALSE), SCOPE);
  }

  @Test
  public void deleteTest() {
    tokenService.delete(TOKEN_ID);
    verify(client).revokeToken(TOKEN_ID);
  }

  @Test
  public void listTest() {
    tokenService.list(createUser(USER_ID, TRUE));
    verify(client).listTokens(USER_ID);
  }

  @Test
  public void userScopesTest() {
    val result = tokenService.userScopes(createUser(USER_ID, TRUE));
    assertThat(result).isEqualTo(SCOPE);
  }

  private static User createUser(String userId, Boolean hasDaco) {
    val user = new User(null, null);
    user.setEmailAddress(USER_ID);
    user.setDaco(hasDaco);

    return user;
  }

  private static AccessToken createAccessToken() {
    return new AccessToken(TOKEN_ID, EXPIRES, singleton(SCOPE));
  }

}
