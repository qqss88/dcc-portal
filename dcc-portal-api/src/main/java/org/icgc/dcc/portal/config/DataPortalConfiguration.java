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

package org.icgc.dcc.portal.config;

import javax.validation.Valid;

import lombok.Getter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

@Getter
@ToString
public class DataPortalConfiguration extends Configuration {

  @Valid
  @JsonProperty
  private final CrowdConfiguration crowd = new CrowdConfiguration();

  @Valid
  @JsonProperty
  private final ElasticSearchConfiguration elastic = new ElasticSearchConfiguration();

  @Valid
  @JsonProperty
  private final BrowserConfiguration browser = new BrowserConfiguration();

  @Valid
  @JsonProperty
  private final MailConfiguration mail = new MailConfiguration();

  @Valid
  @JsonProperty
  private final DataDownloadConfiguration download = new DataDownloadConfiguration();

  @Valid
  @JsonProperty
  private final ICGCConfiguration icgc = new ICGCConfiguration();

  @Valid
  @JsonProperty
  private final HazelcastConfiguration hazelcast = new HazelcastConfiguration();

  @Valid
  @JsonProperty
  private final CacheConfiguration cache = new CacheConfiguration();

  @Valid
  @JsonProperty
  private final WebConfiguration web = new WebConfiguration();

  @Valid
  @JsonProperty
  private final ReleaseConfiguration release = new ReleaseConfiguration();

}
