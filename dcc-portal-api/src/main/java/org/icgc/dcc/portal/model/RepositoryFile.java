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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Models a file from external repositories such as CGHub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "RepositoryFile")
public class RepositoryFile {

  private static final Class<RepositoryFile> MY_CLASS = RepositoryFile.class;
  private static final ObjectMapper MAPPER = createMapper();

  @SneakyThrows
  public static RepositoryFile parse(@NonNull String json) {
    return MAPPER.readValue(json, MY_CLASS);
  }

  /*
   * Fields
   */
  @ApiModelProperty(value = "ID of a repository file")
  String id;

  String fileId;

  @ApiModelProperty(value = "Access type of a repository file")
  String access;

  @ApiModelProperty(value = "Study type of a repository file")
  List<String> study;

  DataCategorization dataCategorization;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class DataCategorization {

    @ApiModelProperty(value = "Data type of a repository file")
    String dataType;

    @ApiModelProperty(value = "Experimental strategy of a repository file")
    String experimentalStrategy;

  }

  DataBundle dataBundle;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class DataBundle {

    @ApiModelProperty(value = "")
    String dataBundleId;

  }

  List<FileCopy> fileCopies;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class FileCopy {

    @ApiModelProperty(value = "")
    String repoCode;
    String repoOrg;
    String repoName;
    String repoType;
    String repoCountry;
    String repoBaseUrl;
    String repoDataPath;
    String repoMetadataPath;

    IndexFile indexFile;

    String fileName;
    String fileFormat;
    String fileMd5sum;
    Long fileSize;
    Long lastModified;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class IndexFile {

    @ApiModelProperty(value = "")
    String id;
    String fileId;
    String fileName;
    String fileFormat;
    String fileMd5sum;
    Long fileSize;

  }

  List<Donor> donors;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Donor {

    @ApiModelProperty(value = "")
    String donorId;
    String program;
    String primarySite;
    String projectCode;

    String study;
    String sampleId;
    String specimenId;
    String specimenType;

    String submittedDonorId;
    String submittedSampleId;
    String submittedSpecimenId;

    String matchedControlSampleId;

    OtherIdentifiers otherIdentifiers;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class OtherIdentifiers {

    @ApiModelProperty(value = "")
    String tcgaSampleBarcode;
    String tcgaAliquotBarcode;
    String tcgaParticipantBarcode;

  }

  AnalysisMethod analysisMethod;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class AnalysisMethod {

    String analysisType;
    String software;

  }

  ReferenceGenome referenceGenome;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ReferenceGenome {

    String genomeBuild;
    String referenceName;
    String downloadUrl;

  }

  // Helpers
  static final ObjectMapper createMapper() {
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
