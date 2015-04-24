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

import static java.util.Collections.emptyList;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Occurrence")
public class Occurrence {

  @ApiModelProperty(value = "Affected Donor ID", required = true)
  String donorId;
  @ApiModelProperty(value = "Mutation ID", required = true)
  String mutationId;
  @ApiModelProperty(value = "Chromosome", required = true)
  String chromosome;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "submitted Mutation ID", required = true)
  String submittedMutationId;
  @ApiModelProperty(value = "Matched Sample ID", required = true)
  String matchedSampleId;
  @ApiModelProperty(value = "Submitted Matched Sample ID", required = true)
  String submittedMatchedSampleId;
  @ApiModelProperty(value = "Affected Project Identifiable", required = true)
  String projectId;
  @ApiModelProperty(value = "Sample ID", required = true)
  String sampleId;
  @ApiModelProperty(value = "Specimen ID", required = true)
  String specimenId;
  @ApiModelProperty(value = "Analysis ID", required = true)
  String analysisId;
  @ApiModelProperty(value = "Analyzed Sample ID", required = true)
  String analyzedSampleId;
  @ApiModelProperty(value = "Base Calling Algorithm", required = true)
  String baseCallingAlgorithm;
  @ApiModelProperty(value = "Strand", required = true)
  Long strand;
  @ApiModelProperty(value = "Control Genotype", required = true)
  String controlGenotype;
  @ApiModelProperty(value = "Experimental Protocol", required = true)
  String experimentalProtocol;
  @ApiModelProperty(value = "Expressed Allele", required = true)
  String expressedAllele;
  @ApiModelProperty(value = "Platform", required = true)
  String platform;
  @ApiModelProperty(value = "Probability", required = true)
  Double probability;
  @ApiModelProperty(value = "Quality Score", required = true)
  Double qualityScore;
  @ApiModelProperty(value = "Raw Data Accession", required = true)
  String rawDataAccession;
  @ApiModelProperty(value = "Raw Data Repository", required = true)
  String rawDataRepository;
  @ApiModelProperty(value = "Read Count", required = true)
  Double readCount;
  @ApiModelProperty(value = "Ref Snp Allele", required = true)
  String refsnpAllele;
  @ApiModelProperty(value = "Sequence Coverage", required = true)
  Double seqCoverage;
  @ApiModelProperty(value = "Sequencing Strategy", required = true)
  String sequencingStrategy;
  @ApiModelProperty(value = "SSM M Db Xref", required = true)
  String ssmMDbXref;
  @ApiModelProperty(value = "SSM M URI", required = true)
  String ssmMUri;
  @ApiModelProperty(value = "SSM P URI", required = true)
  String ssmPUri;
  @ApiModelProperty(value = "Tumour Genotype", required = true)
  String tumourGenotype;
  @ApiModelProperty(value = "Variation Calling Algorithm", required = true)
  String variationCallingAlgorithm;
  @ApiModelProperty(value = "Verification Platform", required = true)
  String verificationPlatform;
  @ApiModelProperty(value = "Verification Status", required = true)
  String verificationStatus;
  @ApiModelProperty(value = "xref Ensembl VarId", required = true)
  String xrefEnsemblVarId;
  @ApiModelProperty(value = "Mutation", required = true)
  String mutation;
  @ApiModelProperty(value = "Observation", required = true)
  List<Observation> observations;

  @JsonCreator
  public Occurrence(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.OCCURRENCE);
    donorId = getString(fieldMap.get(fields.get("donorId")));
    mutationId = getString(fieldMap.get(fields.get("mutationId")));
    chromosome = getString(fieldMap.get(fields.get("chromosome")));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    submittedMutationId = (String) fieldMap.get(fields.get("submittedMutationId"));
    matchedSampleId = (String) fieldMap.get(fields.get("matchedSampleId"));
    submittedMatchedSampleId = (String) fieldMap.get(fields.get("submittedMatchedSampleId"));
    projectId = getString(fieldMap.get(fields.get("projectId")));
    sampleId = getString(fieldMap.get(fields.get("sampleId")));
    specimenId = getString(fieldMap.get(fields.get("specimenId")));
    analysisId = getString(fieldMap.get(fields.get("analysisId")));
    analyzedSampleId = getString(fieldMap.get(fields.get("analyzedSampleId")));
    baseCallingAlgorithm = getString(fieldMap.get(fields.get("baseCallingAlgorithm")));
    strand = getLong(fieldMap.get(fields.get("strand")));
    controlGenotype = getString(fieldMap.get(fields.get("controlGenotype")));
    experimentalProtocol = getString(fieldMap.get(fields.get("experimentalProtocol")));
    expressedAllele = getString(fieldMap.get(fields.get("expressedAllele")));
    platform = getString(fieldMap.get(fields.get("platform")));

    probability = (Double) fieldMap.get(fields.get("probability"));
    qualityScore = (Double) fieldMap.get(fields.get("qualityScore"));
    rawDataAccession = getString(fieldMap.get(fields.get("rawDataAccession")));
    rawDataRepository = getString(fieldMap.get(fields.get("rawDataRepository")));
    readCount = (Double) fieldMap.get(fields.get("readCount"));
    refsnpAllele = getString(fieldMap.get(fields.get("refsnpAllele")));
    seqCoverage = (Double) fieldMap.get(fields.get("seqCoverage"));
    sequencingStrategy = getString(fieldMap.get(fields.get("sequencingStrategy")));
    ssmMDbXref = getString(fieldMap.get(fields.get("ssmMDbXref")));
    ssmMUri = getString(fieldMap.get(fields.get("ssmMUri")));
    ssmPUri = getString(fieldMap.get(fields.get("ssmPUri")));
    tumourGenotype = getString(fieldMap.get(fields.get("tumourGenotype")));
    variationCallingAlgorithm = getString(fieldMap.get(fields.get("variationCallingAlgorithm")));
    verificationPlatform = getString(fieldMap.get(fields.get("verificationPlatform")));
    verificationStatus = getString(fieldMap.get(fields.get("verificationStatus")));
    xrefEnsemblVarId = getString(fieldMap.get(fields.get("xrefEnsemblVarId")));
    mutation = getString(fieldMap.get(fields.get("mutation")));
    observations = buildObservations(getObservations(fieldMap));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getObservations(Map<String, Object> fieldMap) {
    val observationKey = FIELDS_MAPPING.get(Kind.OCCURRENCE).get("observation");
    if (!fieldMap.containsKey(observationKey)) {
      return emptyList();
    }

    return (List<Map<String, Object>>) fieldMap.get(observationKey);
  }

  private List<Observation> buildObservations(List<Map<String, Object>> list) {
    if (list == null) return null;

    List<Observation> observations = Lists.newArrayList();
    for (Map<String, Object> map : list) {
      observations.add(new Observation(map));
    }
    return observations;
  }

}
