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
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import lombok.val;

import com.google.common.base.Joiner;

/**
 * Network-related utility helpers
 */
public class NetworkUtils {

  private static final String ZERO_NETWORK_IP_ADDRESS = "0.0.0.0";
  private static final String UNKNOWN_HOST_NAME = "Unknown";
  private static final char SPACE = ' ';
  private static final Joiner JOINER = Joiner.on(SPACE).skipNulls();

  private static boolean areIPsMeaningful(final String... ipAddresses) {
    for (val ip : ipAddresses) {
      if (isNullOrEmpty(ip) || ip.equals(ZERO_NETWORK_IP_ADDRESS)) {
        return false;
      }
    }

    return true;
  }

  private static String formatNetworkInfo(final String hostName, final String ipAddress) {
    val info = isNullOrEmpty(hostName) ? UNKNOWN_HOST_NAME : hostName.trim();

    return isNullOrEmpty(ipAddress) ? info : info + " (" + ipAddress.trim() + ")";
  }

  private static String getLocalNetworkInfo(final HttpServletRequest request) {
    String localHostName = request.getLocalName();
    String localHostIp = request.getLocalAddr();

    if (!areIPsMeaningful(localHostName, localHostIp)) {
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

  private static String getProxyNetworkInfo(final HttpServletRequest request) {
    val proxyHostName = request.getRemoteHost();
    val proxyHostIp = request.getRemoteAddr();

    return "via " + formatNetworkInfo(proxyHostName, proxyHostIp);
  }

  private static String getRemoteUserNetworkInfo(final HttpServletRequest request) {
    val userIp = request.getHeader(X_FORWARDED_FOR);

    val result = isNullOrEmpty(userIp) ? null : userIp.trim();
    return (null == result) ? null : "from " + result;
  }

  /*
   * A helper to return a string containing hostname and IP-related info for a web request. The format is "Web request
   * received on x.x.x.x (web-server-hostname) from y.y.y.y via z.z.z.z (proxy-hostname)
   */
  public static String getHttpRequestCallerInfo(final HttpServletRequest request) {
    val content = Arrays.asList("Web request received",
        getLocalNetworkInfo(request),
        getRemoteUserNetworkInfo(request),
        getProxyNetworkInfo(request));

    return JOINER.join(content);
  }
}
