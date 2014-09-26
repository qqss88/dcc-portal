/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.icgc.client.api.ICGCException;
import org.icgc.dcc.icgc.client.api.cud.CUDClient;
import org.icgc.dcc.icgc.client.api.cud.User;
import org.icgc.dcc.icgc.client.api.daco.DACOClient;
import org.icgc.dcc.icgc.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.config.ICGCConfiguration;

import com.google.inject.Inject;

/**
 * Provides authentication services against the Central User Directory (CUD) and utilities to check if the user is a
 * DACO approved user.
 * 
 * @see https://wiki.oicr.on.ca/display/icgcweb/CUD-LOGIN
 */
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class AuthService {

  @NonNull
  private CUDClient cudClient;
  @NonNull
  private DACOClient dacoClient;
  @NonNull
  private ICGCConfiguration icgcConfig;

  /**
   * Checks Central User Directory(CUD) if <tt>username</tt> has DACO access.
   * 
   * @throws ICGCException and its sub-classes
   */
  public boolean hasDacoAccess(String userId, UserType userType) {
    return dacoClient.hasDacoAccess(userId, userType);
  }

  /**
   * Logins <tt>username</tt> to Central User Directory(CUD).
   * 
   * @throws ICGCException and its sub-classes
   */
  public String loginUser(String username, String password) {
    return cudClient.login(username, password);
  }

  /**
   * Get user info from Central User Directory(CUD).
   * 
   * @throws ICGCException and its sub-classes
   */
  public User getCudUserInfo(@NonNull String userToken) {
    val authToken = getAuthToken();

    return cudClient.getUserInfo(authToken, userToken);
  }

  /**
   * Login as the service user defined in the configuration file.
   * 
   * @return session token
   * @throws ICGCException and its sub-classes
   */
  private String getAuthToken() {
    return loginUser(icgcConfig.getCudUser(), icgcConfig.getCudPassword());
  }

}
