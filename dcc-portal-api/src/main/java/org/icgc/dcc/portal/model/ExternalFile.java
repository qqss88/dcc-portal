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

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;
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
@ApiModel(value = "ExternalFile")
@Slf4j
public class ExternalFile {

  /* File fields */
  @ApiModelProperty(value = "Repo entity id", required = true)
  String repositoryEntityId; // Used as file identifier

  @ApiModelProperty(value = "Repo name", required = true)
  List<String> repositoryNames;

  @ApiModelProperty(value = "File name", required = true)
  String fileName;

  @ApiModelProperty(value = "Repository", required = true)
  List<String> repository;

  @ApiModelProperty(value = "Project Code", required = true)
  String projectCode;

  @ApiModelProperty(value = "Study", required = true)
  String study;

  @ApiModelProperty(value = "Data type", required = true)
  String dataType;

  @ApiModelProperty(value = "File format", required = true)
  String fileFormat;

  @ApiModelProperty(value = "File size", required = true)
  Long fileSize;

  @ApiModelProperty(value = "File access", required = true)
  String access;

  @ApiModelProperty(value = "Donor associated with file", required = true)
  String donorId;

  String md5;

  String url;

  @JsonCreator
  public ExternalFile(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.EXTERNAL_FILE);

    repositoryEntityId = getString(fieldMap.get(fields.get("repositoryEntityId")));
    repositoryNames = (List<String>) (fieldMap.get(fields.get("repositoryNames")));

    fileName = getString(fieldMap.get(fields.get("fileName")));
    projectCode = getString(fieldMap.get(fields.get("projectCode")));
    study = getString(fieldMap.get(fields.get("study")));
    dataType = getString(fieldMap.get(fields.get("dataType")));
    fileFormat = getString(fieldMap.get(fields.get("fileFormat")));
    fileSize = getLong(fieldMap.get(fields.get("fileSize")));
    access = getString(fieldMap.get(fields.get("access")));
    donorId = getString(fieldMap.get(fields.get("donorId")));
    md5 = getString(fieldMap.get("md5"));
    url = getString(fieldMap.get("url"));

    // FIXME: What field is this?
    repository = null;
  }
}
