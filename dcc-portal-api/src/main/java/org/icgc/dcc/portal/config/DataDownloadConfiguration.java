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

import static com.google.inject.Stage.DEVELOPMENT;
import static org.icgc.dcc.data.archive.ArchiverConstant.ARCHIVE_CURRENT_RELEASE;
import lombok.Getter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Stage;

@Getter
@ToString
public class DataDownloadConfiguration {

  @JsonProperty
  private boolean enabled = true;

  @JsonProperty
  private String dynamicRootPath = "/icgc/download/dynamic";

  @JsonProperty
  private String staticRootPath = "/icgc/download/static";

  @JsonProperty
  private String uri = "";

  @JsonProperty
  private Stage stage = DEVELOPMENT;

  @JsonProperty
  private int maxUsers = 20;

  @JsonProperty
  private String currentReleaseSymlink = "ent /dev";

  @JsonProperty
  private int maxDownloadSizeInMB = 400;

  @JsonProperty
  private String releaseName = ARCHIVE_CURRENT_RELEASE;

  @JsonProperty
  private String quorum = "localhost";

  @JsonProperty
  private String oozieUrl = "http://localhost:11000/oozie";

  @JsonProperty
  private String supportEmailAddress = "***REMOVED***";

  @JsonProperty
  private String appPath = "***REMOVED***";

  @JsonProperty
  private byte capacityThreshold = 20;

}
