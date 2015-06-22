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
package org.dcc.portal.pql.meta;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static lombok.AccessLevel.PRIVATE;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfObjects;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.dcc.portal.pql.meta.field.FieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * TypeModel for Repository File index type
 */
public class RepositoryFileTypeModel extends TypeModel {

  public RepositoryFileTypeModel() {
    super(MAPPINGS, INTERNAL_ALIASES, PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return Type.REPOSITORY_FILE;
  }

  @Override
  public List<String> getFacets() {
    return AVAILABLE_FACETS;
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  @RequiredArgsConstructor(access = PRIVATE)
  public static enum Fields {

    ID("id"),
    STUDY("study"),
    ACCESS("access"),
    DATA_TYPE_OBJECT("dataTypeObject"),
    DATA_FORMAT("dataFormat"),
    DATA_TYPE("dataType"),
    EXPERIMENTAL_STRATEGY("experimentalStrategy"),
    PROJECT_CODE("projectCode"),
    PROGRAM("program"),
    DONOR_STUDY("donorStudy"),
    PRIMARY_SITE("primarySite"),
    DONOR_ID("donorId"),
    SPECIFMEN_ID("specimenId"),
    SPECIMEN_TYPE("specimenType"),
    SAMPLE_ID("sampleId"),
    DONOR_SUBMITTER_ID("donorSubmitterId"),
    SPECIMEN_SUBMITTER_ID("specimenSubmitterId"),
    SAMPLE_SUBMITTER_ID("sampleSubmitterId"),
    TCGA_PARTICIPANT_BARCODE("tcgaParticipantBarcode"),
    TCGA_SAMPLE_BARCODE("tcgaSampleBarcode"),
    TCGA_ALIQUOT_BARCODE("tcgaAliquotBarcode"),
    REPO_TYPE("repoType"),
    REPO_ORG("repoOrg"),
    REPO_ENTITY_ID("repoEntityId"),
    REPO_METADATA_PATH("repoMetadataPath"),
    REPO_DATA_PATH("repoDataPath"),
    LAST_MODIFIED("lastModified"),
    LAST_UPDATE("lastUpdate"), // An alias to LAST_MODIFIED
    FILE_NAME("fileName"),
    FILE_MD5SUM("fileMd5sum"),
    FILE_SIZE("fileSize"),
    REPO_NAME("repoName"),
    REPO_CODE("repoCode"),
    REPO_COUNTRY("repoCountry"),
    REPO_BASE_URL("repoBaseUrl");

    @NonNull
    private final String name;

  }

  public static final List<String> PUBLIC_FIELDS = newArrayList(transform(newArrayList(Fields.values()),
      field -> field.name));

  private static final String TYPE_PREFIX = "file";
  private static final List<String> AVAILABLE_FACETS = ImmutableList.of(
      Fields.STUDY.name,
      Fields.DATA_TYPE.name,
      Fields.DATA_FORMAT.name,
      Fields.ACCESS.name,
      Fields.PROJECT_CODE.name,
      Fields.PRIMARY_SITE.name,
      Fields.DONOR_STUDY.name,
      // "repositoryNames",
      Fields.EXPERIMENTAL_STRATEGY.name);
  // TODO: is it really that we have no include fields??
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of();
  // TODO: should this really be empty??
  private static final Map<String, String> INTERNAL_ALIASES = emptyMap();

  // This represents the mapping of file index type in ElasticSearch.
  private static final List<FieldModel> MAPPINGS = new ImmutableList.Builder<FieldModel>()
      .add(string("id", Fields.ID.name))
      .add(string("study", Fields.STUDY.name))
      .add(string("access", Fields.ACCESS.name))
      // TODO: do we need an alias for the object
      .add(object("data_type", Fields.DATA_TYPE_OBJECT.name,
          string("data_format", Fields.DATA_FORMAT.name),
          string("data_type", Fields.DATA_TYPE.name),
          string("experimental_strategy", Fields.EXPERIMENTAL_STRATEGY.name)))
      // TODO: do we need an alias for the object
      .add(object("donor",
          string("project_code", Fields.PROJECT_CODE.name),
          string("program", Fields.PROGRAM.name),
          string("study", Fields.DONOR_STUDY.name),
          string("primary_site", Fields.PRIMARY_SITE.name),
          string("donor_id", Fields.DONOR_ID.name),
          string("specimen_id", Fields.SPECIFMEN_ID.name),
          string("specimen_type", Fields.SPECIMEN_TYPE.name),
          string("sample_id", Fields.SAMPLE_ID.name),
          string("submitted_donor_id", Fields.DONOR_SUBMITTER_ID.name),
          string("submitted_specimen_id", Fields.SPECIMEN_SUBMITTER_ID.name),
          string("submitted_sample_id", Fields.SAMPLE_SUBMITTER_ID.name),
          string("tcga_participant_barcode", Fields.TCGA_PARTICIPANT_BARCODE.name),
          string("tcga_sample_barcode", Fields.TCGA_SAMPLE_BARCODE.name),
          string("tcga_aliquot_barcode", Fields.TCGA_ALIQUOT_BARCODE.name)))
      // TODO: do we need an alias for the object
      .add(object("repository",
          string("repo_type", Fields.REPO_TYPE.name),
          string("repo_org", Fields.REPO_ORG.name),
          string("repo_entity_id", Fields.REPO_ENTITY_ID.name),
          string("repo_metadata_path", Fields.REPO_METADATA_PATH.name),
          string("repo_data_path", Fields.REPO_DATA_PATH.name),
          string("file_name", Fields.FILE_NAME.name),
          string("file_md5sum", Fields.FILE_MD5SUM.name),
          string("last_modified", ImmutableSet.of(Fields.LAST_MODIFIED.name, Fields.LAST_UPDATE.name)),
          long_("file_size", Fields.FILE_SIZE.name),
          // TODO: fix the alias?
          arrayOfObjects("repo_server", "repoServer", object(
              string("repo_name", Fields.REPO_NAME.name),
              string("repo_code", Fields.REPO_CODE.name),
              string("repo_country", Fields.REPO_COUNTRY.name),
              string("repo_base_url", Fields.REPO_BASE_URL.name)))))
      .build();

}
