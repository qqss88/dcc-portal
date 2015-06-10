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
package org.icgc.dcc.portal.model;

import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Models a file from external repositories such as CGHub
 */
@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "RepositoryFile")
public class RepositoryFile {

  private static final Class<RepositoryFile> MY_CLASS = RepositoryFile.class;
  private static final ObjectMapper MAPPER = createJacksonMapper();

  @SneakyThrows
  @NonNull
  public static RepositoryFile of(String json) {
    return MAPPER.readValue(json, MY_CLASS);
  }

  /*
   * Fields
   */
  @ApiModelProperty(value = "id")
  String id;

  String study;

  String access;

  DataType dataType;

  Repository repository;

  Donor donor;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class DataType {

    String dataType;

    String dataFormat;

    String experimentalStrategy;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class Repository {

    String repoType;

    String repoOrg;

    String repoEntityId;

    List<RepoServer> repoServer;

    String repoMetadataPath;

    String repoDataPath;

    String fileName;

    Long fileSize;

    String fileMd5sum;

    String lastModified;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class RepoServer {

    String repoName;

    String repoCode;

    String repoCountry;

    String repoBaseUrl;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class Donor {

    String projectCode;

    String program;

    String study;

    String primarySite;

    String donorId;

    String submittedDonorId;

    String specimenId;

    String submittedSpecimenId;

    String sampleId;

    String submittedSampleId;

    String tcgaParticipantBarcode;

    String tcgaSampleBarcode;

    String tcgaAliquotBarcode;

  }

  /*
   * Here the visibility is package-private because the unit test for this class calls this method.
   */
  static final ObjectMapper createJacksonMapper() {
    /*
     * We read fields in snake case from an ES response into fields in camel case in Java. Note: Due to this, the serde
     * process is one-way only (deserializing from snake case but serializing in camel case). Don't expect a serialized
     * JSON to be deserialized back into an instance. However, for our current use case, this is okay as we don't expect
     * to consume (JSON with field names in camel case) but produce only.
     */
    val strategy = PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;
    val result = new ObjectMapper();
    result.setPropertyNamingStrategy(strategy);
    return result;
  }

}
