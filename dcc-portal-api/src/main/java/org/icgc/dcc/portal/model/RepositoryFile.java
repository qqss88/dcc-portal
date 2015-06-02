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

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.toStringList;

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Models a file from external repositories such as CGHub
 */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "RepositoryFile")
@Slf4j
public class RepositoryFile {

  private static final Map<String, String> FIELDS = FIELDS_MAPPING.get(Kind.REPOSITORY_FILE);

  @ApiModelProperty(value = "_id", required = true)
  String _id; // This is the internal ES document id

  @ApiModelProperty(value = "Id", required = true)
  String id;

  /* File fields */
  @ApiModelProperty(value = "Repo entity id", required = true)
  String repositoryEntityId;

  @ApiModelProperty(value = "Repo name", required = true)
  List<String> repositoryNames;

  @ApiModelProperty(value = "Base URLs", required = true)
  List<String> repositoryBaseURLs;

  @ApiModelProperty(value = "Repository data path", required = true)
  String repositoryDataPath;

  @ApiModelProperty(value = "File name", required = true)
  String fileName;

  @ApiModelProperty(value = "Repository", required = true)
  List<String> repository;

  @ApiModelProperty(value = "Project Code", required = true)
  String projectCode;

  @ApiModelProperty(value = "Study", required = true)
  String study;

  @ApiModelProperty(value = "Data type", required = true)
  List<String> dataType;

  @ApiModelProperty(value = "File format", required = true)
  List<String> dataFormat;

  @ApiModelProperty(value = "File size", required = true)
  Long fileSize;

  @ApiModelProperty(value = "File access", required = true)
  String access;

  @ApiModelProperty(value = "Donor associated with file", required = true)
  String donorId;

  String donorSubmitterId;

  String specimenId;

  String specimenSubmitterId;

  String sampleId;

  String sampleSubmitterId;

  String repositoryType;

  String checksum;

  String lastUpdate;

  @JsonCreator
  public RepositoryFile(Map<String, Object> fieldMap) {
    _id = getString(getFromMap(fieldMap, "_id"));
    id = getString(getFromMap(fieldMap, "id"));

    repositoryEntityId = getString(getFromMap(fieldMap, "repositoryEntityId"));
    repositoryDataPath = getString(getFromMap(fieldMap, "repositoryDataPath"));

    // Array fields
    repositoryNames = toStringList(getFromMap(fieldMap, "repositoryNames"));
    repositoryBaseURLs = toStringList(getFromMap(fieldMap, "repositoryBaseURLs"));
    dataType = toStringList(getFromMap(fieldMap, "dataType"));
    dataFormat = toStringList(getFromMap(fieldMap, "dataFormat"));

    fileName = getString(getFromMap(fieldMap, "fileName"));
    projectCode = getString(getFromMap(fieldMap, "projectCode"));
    study = getString(getFromMap(fieldMap, "study"));
    fileSize = getLong(getFromMap(fieldMap, "fileSize"));
    access = getString(getFromMap(fieldMap, "access"));
    donorId = getString(getFromMap(fieldMap, "donorId"));

    donorSubmitterId = getString(getFromMap(fieldMap, "donorSubmitterId"));
    specimenId = getString(getFromMap(fieldMap, "specimenId"));
    specimenSubmitterId = getString(getFromMap(fieldMap, "specimenSubmitterId"));
    sampleId = getString(getFromMap(fieldMap, "sampleId"));
    sampleSubmitterId = getString(getFromMap(fieldMap, "sampleSubmitterId"));

    repositoryType = getString(getFromMap(fieldMap, "repositoryType"));
    checksum = getString(getFromMap(fieldMap, "checksum"));
    lastUpdate = getString(getFromMap(fieldMap, "lastUpdate"));

    // FIXME: What field is this?
    repository = null;
  }

  private static Object getFromMap(Map<String, Object> fieldMap, String fieldId) {
    return fieldMap.get(FIELDS.get(fieldId));
  }
}
