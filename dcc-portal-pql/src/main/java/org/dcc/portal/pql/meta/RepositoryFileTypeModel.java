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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
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

  /*
   * We enforce ImmutableList here (instead of List).
   */
  public static ImmutableList<String> toAliasList(Fields... fields) {
    return ImmutableList.copyOf(transform(newArrayList(fields),
        field -> field.alias));
  }

  /**
   * Aliases for Es fields
   */
  @RequiredArgsConstructor(access = PRIVATE)
  public static enum Fields {

    ID("id"),
    STUDY("study"),
    ACCESS("access"),
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
    REPO_SERVER_OBJECT("repoServer"),
    REPO_NAME("repoName"),
    REPO_CODE("repoCode"),
    REPO_COUNTRY("repoCountry"),
    REPO_BASE_URL("repoBaseUrl");

    @NonNull
    private final String alias;

  }

  private static final class EsFieldNames {

    private static final String REPOSITORY = "repository";
    private static final String REPO_SERVER = "repo_server";

  }

  public static final List<String> PUBLIC_FIELDS = toAliasList(Fields.values());

  private static final String TYPE_PREFIX = "file";
  private static final List<String> AVAILABLE_FACETS = toAliasList(
      Fields.STUDY,
      Fields.DATA_TYPE,
      Fields.DATA_FORMAT,
      Fields.ACCESS,
      Fields.PROJECT_CODE,
      Fields.PRIMARY_SITE,
      Fields.DONOR_STUDY,
      // "repositoryNames",
      Fields.EXPERIMENTAL_STRATEGY);
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of(
      EsFieldNames.REPOSITORY + "." + EsFieldNames.REPO_SERVER);
  // TODO: should this really be empty??
  private static final Map<String, String> INTERNAL_ALIASES = emptyMap();

  // This represents the mapping of file index type in ElasticSearch.
  private static final List<FieldModel> MAPPINGS = new ImmutableList.Builder<FieldModel>()
      .add(string("id", Fields.ID.alias))
      .add(string("study", Fields.STUDY.alias))
      .add(string("access", Fields.ACCESS.alias))

      .add(object("data_type",
          string("data_format", Fields.DATA_FORMAT.alias),
          string("data_type", Fields.DATA_TYPE.alias),
          string("experimental_strategy", Fields.EXPERIMENTAL_STRATEGY.alias)))

      .add(object("donor",
          string("project_code", Fields.PROJECT_CODE.alias),
          string("program", Fields.PROGRAM.alias),
          string("study", Fields.DONOR_STUDY.alias),
          string("primary_site", Fields.PRIMARY_SITE.alias),
          string("donor_id", Fields.DONOR_ID.alias),
          string("specimen_id", Fields.SPECIFMEN_ID.alias),
          string("specimen_type", Fields.SPECIMEN_TYPE.alias),
          string("sample_id", Fields.SAMPLE_ID.alias),
          string("submitted_donor_id", Fields.DONOR_SUBMITTER_ID.alias),
          string("submitted_specimen_id", Fields.SPECIMEN_SUBMITTER_ID.alias),
          string("submitted_sample_id", Fields.SAMPLE_SUBMITTER_ID.alias),
          string("tcga_participant_barcode", Fields.TCGA_PARTICIPANT_BARCODE.alias),
          string("tcga_sample_barcode", Fields.TCGA_SAMPLE_BARCODE.alias),
          string("tcga_aliquot_barcode", Fields.TCGA_ALIQUOT_BARCODE.alias)))

      .add(object(EsFieldNames.REPOSITORY,
          string("repo_type", Fields.REPO_TYPE.alias),
          string("repo_org", Fields.REPO_ORG.alias),
          string("repo_entity_id", Fields.REPO_ENTITY_ID.alias),
          string("repo_metadata_path", Fields.REPO_METADATA_PATH.alias),
          string("repo_data_path", Fields.REPO_DATA_PATH.alias),
          string("file_name", Fields.FILE_NAME.alias),
          string("file_md5sum", Fields.FILE_MD5SUM.alias),
          string("last_modified", ImmutableSet.of(Fields.LAST_MODIFIED.alias, Fields.LAST_UPDATE.alias)),
          long_("file_size", Fields.FILE_SIZE.alias),

          arrayOfObjects(EsFieldNames.REPO_SERVER, Fields.REPO_SERVER_OBJECT.alias, object(
              string("repo_name", Fields.REPO_NAME.alias),
              string("repo_code", Fields.REPO_CODE.alias),
              string("repo_country", Fields.REPO_COUNTRY.alias),
              string("repo_base_url", Fields.REPO_BASE_URL.alias)))))
      .build();

}
