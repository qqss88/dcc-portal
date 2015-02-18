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
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;

public class MutationCentricTypeModel extends AbstractTypeModel {

  public MutationCentricTypeModel() {
    super(defineFields());
  }

  private static List<FieldModel> defineFields() {
    return new ImmutableList.Builder<FieldModel>()
        .add(string("assembly_version", "assemblyVersion"))
        .add(string("chromosome", "chromosome"))
        .add(long_("chromosome_start", "start"))
        .add(long_("chromosome_end", "end"))
        .add(string("_mutation_id", "id"))
        .add(string("mutation", "mutation"))
        .add(string("mutation_type", "type"))
        .add(string("reference_genome_allele", "referenceGenomeAllele"))
        .add(defineSsmOccurrence())
        .add(defineTranscript())
        .add(string("platform", "platform"))
        .add(arrayOfStrings("verification_status", "verificationStatus"))
        .add(arrayOfStrings("sequencing_strategy", "sequencingStrategy"))
        .add(arrayOfStrings("consequence_type", "consequenceType"))
        .add(arrayOfStrings("functional_impact_prediction_summary", "functionalImpact"))
        .add(defineSummary())
        .build();
  }

  private static ObjectFieldModel defineSummary() {
    return object("_summary",
        long_("_affected_donor_count", "affectedDonorCountTotal"),
        string("_tested_donor_count", "testedDonorCount"),
        string("_affected_project_count", "affectedProjectCount"),
        arrayOfStrings("_affected_project_ids", "affectedProjectIds"));
  }

  private static ArrayFieldModel defineTranscript() {
    return nestedArrayOfObjects("transcript",
        object(
            string("id", "transcriptId"),
            string("functional_impact_prediction_summary", "functionalImpactNested"),
            object("consequence", string("consequence_type", "consequenceTypeNested"))));
  }

  private static ArrayFieldModel defineSsmOccurrence() {
    return nestedArrayOfObjects("ssm_occurrence",
        object(
        nestedArrayOfObjects("observation", object(
            string("platform", "platformNested"),
            string("verification_status", "verificationStatusNested"),
            string("sequencing_strategy", "sequencingStrategyNested")))
        ));
  }

}
