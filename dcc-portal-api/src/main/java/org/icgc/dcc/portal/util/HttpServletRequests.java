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
package org.icgc.dcc.portal.util;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;

import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import lombok.NonNull;
import lombok.val;
import lombok.experimental.UtilityClass;

import com.google.common.base.Joiner;

/**
 * HttpServletRequest-related helpers
 */
@UtilityClass
public class HttpServletRequests {

  private final String RESERVED_IP_FOR_ZERO_NETWORK = "0.0.0.0";
  private final String UNKNOWN_HOST_NAME = "Unknown";
  private final char SPACE = ' ';
  private static final Joiner JOINER = Joiner.on(SPACE).skipNulls();

  /*
   * A helper to return a string containing hostname and IP-related info for a web request. The format is "Web request
   * received on x.x.x.x (web-server-hostname) from y.y.y.y via z.z.z.z (proxy-hostname)
   */
  @NonNull
  public String getHttpRequestCallerInfo(final HttpServletRequest request) {
    val intro = "Web request received";

    return JOINER.join(intro, getLocalNetworkInfo(request), getRemoteUserNetworkInfo(request),
        getProxyNetworkInfo(request));
  }

  private boolean isIpValid(final String... ipAddresses) {
    for (val ip : ipAddresses) {
      if (isNullOrEmpty(ip) || ip.equals(RESERVED_IP_FOR_ZERO_NETWORK)) {
        return false;
      }
    }

    return true;
  }

  private String formatNetworkInfo(final String hostName, final String ipAddress) {
    val info = isNullOrEmpty(hostName) ? UNKNOWN_HOST_NAME : hostName.trim();

    return isNullOrEmpty(ipAddress) ? info : info + " (" + ipAddress.trim() + ")";
  }

  private String getLocalNetworkInfo(final HttpServletRequest request) {
    String localHostName = request.getLocalName();
    String localHostIp = request.getLocalAddr();

    if (!isIpValid(localHostName, localHostIp)) {
      try {
        val host = InetAddress.getLocalHost();

        localHostName = host.getHostName();
        localHostIp = host.getHostAddress();
      } catch (Exception e) {
        // Simply ignore this.
      }
    }

    return "on " + formatNetworkInfo(localHostName, localHostIp);
  }

  private String getProxyNetworkInfo(final HttpServletRequest request) {
    val proxyHostName = request.getRemoteHost();
    val proxyHostIp = request.getRemoteAddr();

    return "via " + formatNetworkInfo(proxyHostName, proxyHostIp);
  }

  private String getRemoteUserNetworkInfo(final HttpServletRequest request) {
    val userIp = request.getHeader(X_FORWARDED_FOR);

    val result = isNullOrEmpty(userIp) ? null : userIp.trim();
    return (null == result) ? null : "from " + result;
  }

}
