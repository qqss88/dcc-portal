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

import static java.util.Collections.emptyList;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.nestedArrayOfObjects;
import static org.dcc.portal.pql.meta.field.DoubleFieldModel.double_;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.nestedObject;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;
import java.util.Map;

import lombok.val;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;
import org.elasticsearch.common.collect.ImmutableSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ObservationCentricTypeModel extends TypeModel {

  private final static String TYPE_PREFIX = "observation";
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of("ssm.consequence", "ssm.observation");
  private static final List<String> PUBLIC_FIELDS = ImmutableList.of(
      "observation",
      "donorId",
      "analysisId",
      "analyzedSampleId",
      "baseCallingAlgorithm",
      "chromosome",
      "controlGenotype",
      "end",
      "experimentalProtocol",
      "expressedAllele",
      "matchedSampleId",
      "mutation",
      "mutationId",
      "platform",
      "donor.primarySite",
      "probability",
      "projectId",
      "qualityScore",
      "rawDataAccession",
      "rawDataRepository",
      "readCount",
      "refsnpAllele",
      "sampleId",
      "seqCoverage",
      "sequencingStrategy",
      "specimenId",
      "start",
      "submittedMatchedSampleId",
      "variationCallingAlgorithm",
      "verificationStatus");

  public ObservationCentricTypeModel() {
    super(defineFields(), defineInternalAliases(), PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return Type.OBSERVATION_CENTRIC;
  }

  @Override
  public List<String> getFacets() {
    return emptyList();
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  private static List<FieldModel> defineFields() {
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(defineSsm());
    fields.add(defineProject());
    fields.add(defineDonor());

    // This is a fake field to which is resolved by GeneSetFilterVisitor
    fields.add(string(GENE_GO_TERM_ID, GENE_GO_TERM_ID));
    fields.add(string(GENE_SET_ID, GENE_SET_ID));

    // This is a fake field to which is resolved by LocationFilterVisitor
    fields.add(string(GENE_LOCATION, GENE_LOCATION));
    fields.add(string(MUTATION_LOCATION, MUTATION_LOCATION));

    fields.add(string(SCORE, SCORE));
    fields.add(string(DONOR_ENTITY_SET_ID, DONOR_ENTITY_SET_ID));
    fields.add(string(GENE_ENTITY_SET_ID, GENE_ENTITY_SET_ID));
    fields.add(string(MUTATION_ENTITY_SET_ID, MUTATION_ENTITY_SET_ID));

    return fields.build();
  }

  private static ObjectFieldModel defineSsm() {
    return ObjectFieldModel.nestedObject("ssm",
        defineSsmConsequence(),
        string("_mutation_id", ImmutableSet.of("mutation.id", "mutationId")),
        string("mutation_type", "mutation.type"),
        defineSsmObservation(),
        string("chromosome", ImmutableSet.of("mutation.chromosome", "chromosome")),
        long_("chromosome_start", ImmutableSet.of("mutation.start", "start")),
        long_("chromosome_end", ImmutableSet.of("mutation.end", "end")),

        string("_donor_id", "donorId"),
        string("analysis_id", "analysisId"),
        string("analyzed_sample_id", "analyzedSampleId"),
        string("base_calling_algorithm", "baseCallingAlgorithm"),
        string("control_genotype", "controlGenotype"),
        string("experimental_protocol", "experimentalProtocol"),
        string("expressed_allele", "expressedAllele"),
        string("_matched_sample_id", "matchedSampleId"),
        string("mutation", "mutation"),
        string("platform", "platform"),
        long_("probability", "probability"),
        string("_project_id", "projectId"),
        double_("quality_score", "qualityScore"),
        string("raw_data_accession", "rawDataAccession"),
        string("raw_data_repository", "rawDataRepository"),
        long_("read_count", "readCount"),
        string("refsnp_allele", "refsnpAllele"),
        string("_sample_id", "sampleId"),
        string("seq_coverage", "seqCoverage"),
        string("sequencing_strategy", "sequencingStrategy"),
        string("_specimen_id", "specimenId"),
        string("matched_sample_id", "submittedMatchedSampleId"),
        string("variation_calling_algorithm", "variationCallingAlgorithm"),
        string("verification_status", "verificationStatus"));
  }

  private static ArrayFieldModel defineSsmObservation() {
    return nestedArrayOfObjects("observation", "observation", object(
        string("platform", "mutation.platform"),
        string("sequencing_strategy", "mutation.sequencingStrategy"),
        string("verification_status", "mutation.verificationStatus")));
  }

  private static ArrayFieldModel defineSsmConsequence() {
    return nestedArrayOfObjects("consequence", "consequences", object(
        object("gene",
            string("_gene_id", "gene.id"),
            string("biotype", "gene.type"),
            arrayOfStrings("pathway", "gene.pathwayId"),
            string("chromosome", "gene.chromosome"),
            long_("start", "gene.start"),
            long_("end", "gene.end"),
            string("curated_set", "gene.curatedSetId"),
            object("go_term", "gene.GoTerm",
                arrayOfStrings("biological_process"),
                arrayOfStrings("cellular_component"),
                arrayOfStrings("molecular_function"))

        ),
        string("consequence_type", "mutation.consequenceType"),
        string("functional_impact_prediction_summary", "mutation.functionalImpact")));
  }

  private static ObjectFieldModel defineProject() {
    return nestedObject("project",
        string("_project_id", "donor.projectId"),
        string("primary_site", "donor.primarySite"));
  }

  private static ObjectFieldModel defineDonor() {
    return ObjectFieldModel.nestedObject("donor",
        string("_donor_id", "donor.id"),
        string("donor_sex", "donor.gender"),
        string("donor_tumour_stage_at_diagnosis", "donor.tumourStageAtDiagnosis"),
        string("donor_vital_status", "donor.vitalStatus"),
        string("disease_status_last_followup", "donor.diseaseStatusLastFollowup"),
        string("donor_relapse_type", "donor.relapseType"),
        string("_summary._state", "donor.state"),
        arrayOfStrings("_summary._studies", "donor.studies"),
        defineDonorSummary());
  }

  private static ObjectFieldModel defineDonorSummary() {
    return object("_summary",
        string("_age_at_diagnosis_group", "donor.ageAtDiagnosisGroup"),
        string("_available_data_type", "donor.availableDataTypes"),
        string("experimental_analysis_performed", "donor.analysisTypes"));
  }

  private static Map<String, String> defineInternalAliases() {
    return new ImmutableMap.Builder<String, String>()
        .put(BIOLOGICAL_PROCESS, "ssm.consequence.gene.go_term.biological_process")
        .put(CELLULAR_COMPONENT, "ssm.consequence.gene.go_term.cellular_component")
        .put(MOLECULAR_FUNCTION, "ssm.consequence.gene.go_term.molecular_function")
        .put(DONOR_ENTITY_SET_ID, "donor._donor_id")
        .put(GENE_ENTITY_SET_ID, "ssm.consequence.gene._gene_id")
        .put(MUTATION_ENTITY_SET_ID, "ssm._mutation_id")
        .put(LOOKUP_TYPE, "observation-ids")
        .build();
  }

}
