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
package meta;

import static meta.ArrayFieldModel.arrayOfObjects;
import static meta.ArrayFieldModel.arrayOfStrings;
import static meta.ArrayFieldModel.nestedArrayOfObjects;
import static meta.BooleanFieldModel.bool;
import static meta.LongFieldModel._long;
import static meta.ObjectFieldModel.object;
import static meta.StringFieldModel.string;

import java.util.List;

import lombok.Value;
import lombok.val;

import com.google.common.collect.ImmutableList;

@Value
public class DonorCentricTypeModel {

  private final List<FieldModel> fields;

  public DonorCentricTypeModel() {
    fields = initializeFields();
  }

  private List<FieldModel> initializeFields() {
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(string("_donor_id"));
    fields.add(string("_project_id"));
    fields.add(initSummary());
    fields.add(_long("donor_age_at_diagnosis"));
    fields.add(_long("donor_age_at_enrollment"));
    fields.add(_long("donor_age_at_last_followup"));
    fields.add(string("donor_diagnosis_icd10"));
    fields.add(string("donor_id"));
    fields.add(_long("donor_interval_of_last_followup"));
    fields.add(_long("donor_relapse_interval"));
    fields.add(string("donor_relapse_type"));
    fields.add(string("donor_sex"));
    fields.add(_long("donor_survival_time"));
    fields.add(string("donor_tumour_stage_at_diagnosis"));
    fields.add(string("donor_tumour_stage_at_diagnosis_supplemental"));
    fields.add(string("donor_tumour_staging_system_at_diagnosis"));
    fields.add(string("donor_vital_status"));
    fields.add(initGene());
    fields.add(initProject());

    return fields.build();
  }

  private ObjectFieldModel initSummary() {
    return object("_summary",
        _long("_affected_gene_count"),
        string("_age_at_diagnosis_group"),
        arrayOfStrings("_available_data_type"),
        bool("_cngv_exists"),
        bool("_cnsm_exists"),
        bool("_exp_array_exists"),
        bool("_exp_seq_exists"),
        bool("_jcn_exists"),
        bool("_meth_array_exists"),
        bool("_meth_seq_exists"),
        bool("_mirna_seq_exists"),
        bool("_pexp_exists"),
        bool("_sgv_exists"),
        bool("_ssm_count"),
        bool("_stgv_exists"),
        bool("_stsm_exists"),
        arrayOfStrings("_studies"),
        arrayOfStrings("experimental_analysis_performed"),
        initExperimentalAnalysisPerformedSampleCount(),
        arrayOfStrings("repository"));
  }

  private ObjectFieldModel initExperimentalAnalysisPerformedSampleCount() {
    return object("experimental_analysis_performed_sample_count",
        _long("AMPLICON"),
        _long("Bisulfite-Seq"),
        _long("RNA-Seq"),
        _long("WGA"),
        _long("WGS"),
        _long("WXS"),
        _long("miRNA-Seq"),
        _long("non-NGS"));
  }

  private ArrayFieldModel initGene() {
    val element = object(
        string("_gene_id"),
        object("_summary", _long("_ssm_count")),
        string("biotype"),
        string("chromosome"),
        _long("end"),
        _long("start"),
        string("symbol"),
        arrayOfStrings("pathway"),
        object("go_term", arrayOfStrings("biological_process"), arrayOfStrings("cellular_component")),
        arrayOfObjects("ssm", initSmm()));

    return nestedArrayOfObjects("gene", element);
  }

  private ObjectFieldModel initSmm() {
    return object(
        string("_mutation_id"),
        string("_type"),
        string("chromosome"),
        _long("chromosome_end"),
        _long("chromosome_start"),
        arrayOfObjects("consequence",
            object(string("consequence_type"), string("functional_impact_prediction_summary"))),
        string("mutation_type"),
        arrayOfObjects("observation", initObservation()));
  }

  private ObjectFieldModel initObservation() {
    return object(
        string("_matched_sample_id"),
        string("_sample_id"),
        string("_specimen_id"),
        string("alignment_algorithm"),
        string("analysis_id"),
        string("analyzed_sample_id"),
        string("base_calling_algorithm"),
        string("biological_validation_platform"),
        string("biological_validation_status"),
        string("experimental_protocol"),
        string("marking"),
        string("matched_sample_id"),
        _long("mutant_allele_read_count"),
        string("observation_id"),
        string("other_analysis_algorithm"),
        string("platform"),
        _long("probability"), // FIXME: double?
        _long("quality_score"), // FIXME: double?
        string("raw_data_accession"),
        string("raw_data_repository"),
        _long("seq_coverage"), // FIXME: double?
        string("sequencing_strategy"),
        _long("total_read_count"), // FIXME: double?
        string("variation_calling_algorithm"),
        string("verification_platform"),
        string("verification_status"));
  }

  private ObjectFieldModel initProject() {
    return object("project",
        string("_project_id"),
        string("primary_site"),
        string("project_name"));
  }

}
