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

import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.nestedArrayOfObjects;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.nestedArrayOfStrings;
import static org.dcc.portal.pql.meta.field.BooleanFieldModel.bool;
import static org.dcc.portal.pql.meta.field.DoubleFieldModel.double_;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;

import lombok.val;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;

public class DonorCentricTypeModel extends AbstractTypeModel {

  public DonorCentricTypeModel() {
    super(initFields());
  }

  private static List<FieldModel> initFields() {
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(string("_donor_id"));
    fields.add(string("_project_id"));
    fields.add(initSummary());
    fields.add(long_("donor_age_at_diagnosis"));
    fields.add(long_("donor_age_at_enrollment"));
    fields.add(long_("donor_age_at_last_followup"));
    fields.add(string("donor_diagnosis_icd10"));
    fields.add(string("donor_id"));
    fields.add(long_("donor_interval_of_last_followup"));
    fields.add(long_("donor_relapse_interval"));
    fields.add(string("donor_relapse_type"));
    fields.add(string("donor_sex"));
    fields.add(long_("donor_survival_time"));
    fields.add(string("donor_tumour_stage_at_diagnosis"));
    fields.add(string("donor_tumour_stage_at_diagnosis_supplemental"));
    fields.add(string("donor_tumour_staging_system_at_diagnosis"));
    fields.add(string("donor_vital_status"));
    fields.add(initGene());
    fields.add(initProject());

    return fields.build();
  }

  private static ObjectFieldModel initSummary() {
    return object("_summary",
        long_("_affected_gene_count"),
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

  private static ObjectFieldModel initExperimentalAnalysisPerformedSampleCount() {
    return object("experimental_analysis_performed_sample_count",
        long_("AMPLICON"),
        long_("Bisulfite-Seq"),
        long_("RNA-Seq"),
        long_("WGA"),
        long_("WGS"),
        long_("WXS"),
        long_("miRNA-Seq"),
        long_("non-NGS"));
  }

  private static ArrayFieldModel initGene() {
    val element = object(
        string("_gene_id"),
        object("_summary", long_("_ssm_count")),
        string("biotype"),
        string("chromosome"),
        long_("end"),
        long_("start"),
        string("symbol"),
        nestedArrayOfStrings("pathways"),
        object("go_term", arrayOfStrings("biological_process"), arrayOfStrings("cellular_component")),
        nestedArrayOfObjects("ssm", initSmm()));

    return nestedArrayOfObjects("gene", element);
  }

  private static ObjectFieldModel initSmm() {
    return object(
        string("_mutation_id"),
        string("_type"),
        string("chromosome"),
        long_("chromosome_end"),
        long_("chromosome_start"),
        nestedArrayOfObjects("consequence",
            object(string("consequence_type"), string("functional_impact_prediction_summary"))),
        string("mutation_type"),
        nestedArrayOfObjects("observation", initObservation()));
  }

  private static ObjectFieldModel initObservation() {
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
        long_("mutant_allele_read_count"),
        string("observation_id"),
        string("other_analysis_algorithm"),
        string("platform"),
        double_("probability"),
        double_("quality_score"),
        string("raw_data_accession"),
        string("raw_data_repository"),
        double_("seq_coverage"),
        string("sequencing_strategy"),
        long_("total_read_count"),
        string("variation_calling_algorithm"),
        string("verification_platform"),
        string("verification_status"));
  }

  private static ObjectFieldModel initProject() {
    return object("project",
        string("_project_id"),
        string("primary_site"),
        string("project_name"));
  }

}
