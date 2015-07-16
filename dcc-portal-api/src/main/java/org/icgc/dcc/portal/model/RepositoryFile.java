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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Models a file from external repositories such as CGHub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "RepositoryFile")
public class RepositoryFile {

  private static final Class<RepositoryFile> MY_CLASS = RepositoryFile.class;
  private static final ObjectMapper MAPPER = createJacksonMapper();

  @SneakyThrows
  @NonNull
  public static RepositoryFile parse(String json) {
    return MAPPER.readValue(json, MY_CLASS);
  }

  /*
   * Fields
   */
  @ApiModelProperty(value = "ID of a repository file")
  String id;

  @ApiModelProperty(value = "Study type of a repository file")
  String study;

  @ApiModelProperty(value = "Access type of a repository file")
  String access;

  @ApiModelProperty(value = "Data type details of a repository file")
  DataType dataType;

  @ApiModelProperty(value = "Repository details of a repository file")
  Repository repository;

  @ApiModelProperty(value = "Donor details of a repository file")
  Donor donor;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class DataType {

    @ApiModelProperty(value = "Data type of a repository file")
    String dataType;

    @ApiModelProperty(value = "Data format of a repository file")
    String dataFormat;

    @ApiModelProperty(value = "Experimental strategy of a repository file")
    String experimentalStrategy;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class Repository {

    @ApiModelProperty(value = "Repository type")
    String repoType;

    @ApiModelProperty(value = "Repository organization")
    String repoOrg;

    @ApiModelProperty(value = "Repository entity ID")
    String repoEntityId;

    @ApiModelProperty(value = "List of repository details")
    List<RepoServer> repoServer;

    @ApiModelProperty(value = "Path to repository's meta-data")
    String repoMetadataPath;

    @ApiModelProperty(value = "Path to Repository data file")
    String repoDataPath;

    @ApiModelProperty(value = "Repository file name")
    String fileName;

    @ApiModelProperty(value = "Repository file size")
    Long fileSize;

    @ApiModelProperty(value = "MD5 checksum of a repository file")
    String fileMd5sum;

    @ApiModelProperty(value = "Last modified timestamp of a repository file")
    String lastModified;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class RepoServer {

    @ApiModelProperty(value = "Repository name")
    String repoName;

    @ApiModelProperty(value = "Repository code")
    String repoCode;

    @ApiModelProperty(value = "Country where the repository resides")
    String repoCountry;

    @ApiModelProperty(value = "Base URL of a repository")
    String repoBaseUrl;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class Donor {

    @ApiModelProperty(value = "Project code")
    String projectCode;

    @ApiModelProperty(value = "Program")
    String program;

    @ApiModelProperty(value = "Study")
    String study;

    @ApiModelProperty(value = "Primary Site")
    String primarySite;

    @ApiModelProperty(value = "Donor ID")
    String donorId;

    @ApiModelProperty(value = "Donor submitter ID")
    String submittedDonorId;

    @ApiModelProperty(value = "Specimen ID")
    String specimenId;

    @ApiModelProperty(value = "Specimen submitter ID")
    String submittedSpecimenId;

    @ApiModelProperty(value = "Sample ID")
    String sampleId;

    @ApiModelProperty(value = "Sample submitter ID")
    String submittedSampleId;

    @ApiModelProperty(value = "TCGA participant barcode")
    String tcgaParticipantBarcode;

    @ApiModelProperty(value = "TCGA sample barcode")
    String tcgaSampleBarcode;

    @ApiModelProperty(value = "TCGA aliquot barcode")
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
