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

package org.icgc.dcc.portal.model;

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import lombok.Value;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Keyword")
public class Keyword {

  @ApiModelProperty(value = "ID", required = true)
  String id;
  @ApiModelProperty(value = "Type", required = true)
  String type;
  @ApiModelProperty(required = false)
  String name;
  @ApiModelProperty(required = false)
  String symbol;
  @ApiModelProperty(required = false)
  String ensemblTranscriptId;
  @ApiModelProperty(required = false)
  String ensemblTranslationId;
  @ApiModelProperty(required = false)
  List<String> synonyms;
  @ApiModelProperty(required = false)
  List<String> uniprotkbSwissprot;
  @ApiModelProperty(required = false)
  List<String> omimGene;
  @ApiModelProperty(required = false)
  List<String> entrezGene;
  @ApiModelProperty(required = false)
  List<String> hgnc;
  @ApiModelProperty(required = false)
  List<String> altIds;

  @ApiModelProperty(required = false)
  String projectId;
  @ApiModelProperty(required = false)
  String submittedId;
  @ApiModelProperty(required = false)
  List<String> specimenIds;
  @ApiModelProperty(required = false)
  List<String> submittedSpecimenIds;
  @ApiModelProperty(required = false)
  List<String> sampleIds;
  @ApiModelProperty(required = false)
  List<String> submittedSampleIds;

  @ApiModelProperty(required = false)
  String primarySite;
  @ApiModelProperty(required = false)
  String tumourType;
  @ApiModelProperty(required = false)
  String tumourSubtype;

  @ApiModelProperty(required = false)
  String mutation;
  @ApiModelProperty(required = false)
  List<String> geneMutations;

  @ApiModelProperty(required = false)
  String url;

  @ApiModelProperty(required = false)
  String source;

  @ApiModelProperty(required = false)
  String donorId;

  @ApiModelProperty(required = false)
  String fileName;

  @ApiModelProperty(required = false)
  List<String> tcgaParticipantBarcode;

  @ApiModelProperty(required = false)
  List<String> tcgaSampleBarcode;

  @ApiModelProperty(required = false)
  List<String> tcgaAliquotBarcode;

  @SuppressWarnings("unchecked")
  public Keyword(Map<String, Object> fieldMap) {
    ImmutableMap<String, String> fields = FIELDS_MAPPING.get(Kind.KEYWORD);
    type = getString(fieldMap.get(fields.get("type")));
    name = getString(fieldMap.get(fields.get("name")));

    symbol = getString(fieldMap.get(fields.get("symbol")));
    ensemblTranscriptId = getString(fieldMap.get(fields.get("ensemblTranscriptId")));
    ensemblTranslationId = getString(fieldMap.get(fields.get("ensemblTranslationId")));
    synonyms = (List<String>) fieldMap.get(fields.get("synonyms"));
    uniprotkbSwissprot = (List<String>) fieldMap.get(fields.get("uniprotkbSwissprot"));
    omimGene = (List<String>) fieldMap.get(fields.get("omimGene"));
    entrezGene = (List<String>) fieldMap.get(fields.get("entrezGene"));
    hgnc = (List<String>) fieldMap.get(fields.get("hgnc"));
    altIds = (List<String>) fieldMap.get(fields.get("altIds"));

    projectId = getString(fieldMap.get(fields.get("projectId")));
    submittedId = getString(fieldMap.get(fields.get("submittedId")));
    specimenIds = (List<String>) fieldMap.get(fields.get("specimenIds"));
    submittedSpecimenIds = (List<String>) fieldMap.get(fields.get("submittedSpecimenIds"));
    sampleIds = (List<String>) fieldMap.get(fields.get("sampleIds"));
    submittedSampleIds = (List<String>) fieldMap.get(fields.get("submittedSampleIds"));

    primarySite = getString(fieldMap.get(fields.get("primarySite")));
    tumourType = getString(fieldMap.get(fields.get("tumourType")));
    tumourSubtype = getString(fieldMap.get(fields.get("tumourSubtype")));

    mutation = getString(fieldMap.get(fields.get("mutation")));
    geneMutations = (List<String>) fieldMap.get(fields.get("geneMutations"));

    url = getString(fieldMap.get(fields.get("url")));
    source = getString(fieldMap.get(fields.get("source")));

    // File
    fileName = getString(fieldMap.get(fields.get("file_name")));
    donorId = getString(fieldMap.get(fields.get("donor_id")));
    tcgaParticipantBarcode = (List<String>) fieldMap.get(fields.get("TCGAParticipantBarcode"));
    tcgaAliquotBarcode = (List<String>) fieldMap.get(fields.get("TCGAAliquotBarcode"));
    tcgaSampleBarcode = (List<String>) fieldMap.get(fields.get("TCGASampleBarcode"));

    // Generic id
    id = getString(fieldMap.get(fields.get("id")));
  }
}
