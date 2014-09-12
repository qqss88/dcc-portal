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

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.google.common.collect.Maps;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Mutation")
public class Mutation {

  @ApiModelProperty(value = "Mutation ID", required = true)
  String id;
  @ApiModelProperty(value = "Type", required = true)
  String type;
  @ApiModelProperty(value = "Chromosome", required = true)
  String chromosome;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "Mutation", required = true)
  String mutation;
  @ApiModelProperty(value = "Assembly Version", required = true)
  String assemblyVersion;
  @ApiModelProperty(value = "Reference Genome Allele", required = true)
  String referenceGenomeAllele;
  @ApiModelProperty(value = "Tested Donor Count", required = true)
  Long testedDonorCount;
  @ApiModelProperty(value = "Total number of Donors affected by Mutation", required = true)
  Long affectedDonorCountTotal;
  @ApiModelProperty(value = "Filtered number of Donors affected by Mutation", required = true)
  Long affectedDonorCountFiltered;
  @ApiModelProperty(value = "Number of Cancer Projects with a Donor affected by Mutation", required = true)
  Long affectedProjectCount;
  @ApiModelProperty(value = "List of Cancer Projects with a Donor affected by Mutation", required = true)
  List<String> affectedProjectIds;
  @ApiModelProperty(value = "Platform", required = true)
  List<String> platform;
  @ApiModelProperty(value = "Consequence Type", required = true)
  List<String> consequenceType;
  @ApiModelProperty(value = "Verification Status", required = true)
  List<String> verificationStatus;
  @ApiModelProperty(value = "Occurrences")
  List<EmbOccurrence> occurrences;
  @ApiModelProperty(value = "Transcripts")
  List<Transcript> transcripts;
  @ApiModelProperty(value = "Consequences")
  List<Consequence> consequences;
  @ApiModelProperty(value = "Functional Impact Prediction Summary")
  List<String> functionalImpact;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Mutation(Map<String, Object> fieldMap) {
    ImmutableMap<String, String> fields = FIELDS_MAPPING.get(Kind.MUTATION);
    id = (String) fieldMap.get(fields.get("id"));
    type = (String) fieldMap.get(fields.get("type"));
    chromosome = (String) fieldMap.get(fields.get("chromosome"));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    mutation = (String) fieldMap.get(fields.get("mutation"));
    assemblyVersion = (String) fieldMap.get(fields.get("assemblyVersion"));
    referenceGenomeAllele = (String) fieldMap.get(fields.get("referenceGenomeAllele"));
    testedDonorCount = getLong(fieldMap.get(fields.get("testedDonorCount")));
    affectedDonorCountTotal = getLong(fieldMap.get(fields.get("affectedDonorCountTotal")));
    affectedDonorCountFiltered = getLong(fieldMap.get(fields.get("affectedDonorCountFiltered")));
    affectedProjectCount = getLong(fieldMap.get(fields.get("affectedProjectCount")));
    affectedProjectIds = (List<String>) fieldMap.get(fields.get("affectedProjectIds"));
    platform = (List<String>) fieldMap.get(fields.get("platform"));
    consequenceType = (List<String>) fieldMap.get(fields.get("consequenceType"));
    verificationStatus = (List<String>) fieldMap.get(fields.get("verificationStatus"));
    occurrences = buildOccurrences((List<Map<String, Object>>) fieldMap.get("ssm_occurrence"));
    transcripts = buildTranscripts((List<Map<String, Object>>) fieldMap.get("transcript"));
    consequences = buildConsequences((List<Map<String, Object>>) fieldMap.get("consequences"));
    functionalImpact = (List<String>) fieldMap.get(fields.get("functionalImpact"));
  }

  private List<EmbOccurrence> buildOccurrences(List<Map<String, Object>> occurrences) {
    if (occurrences == null) return null;
    val lst = Lists.<EmbOccurrence> newArrayList();
    for (val item : occurrences) {
      lst.add(new EmbOccurrence(item));
    }
    return lst;
  }

  private List<Transcript> buildTranscripts(List<Map<String, Object>> transcripts) {
    if (transcripts == null) return null;
    val lst = Lists.<Transcript> newArrayList();
    for (val item : transcripts) {
      lst.add(new Transcript(item));
    }
    return lst;
  }

  @SuppressWarnings("unchecked")
  private List<Consequence> buildConsequences(List<Map<String, Object>> transcripts) {
    if (transcripts == null) return null;
    val lst = Lists.<Consequence> newArrayList();
    val consequencesMap = Maps.<String, Consequence> newHashMap();

    for (val item : transcripts) {
      val gene = (Map<String, Object>) item.get("gene");
      val consequence = new Consequence((Map<String, Object>) item.get("consequence"));
      val functionalImpact = (String) item.get("functional_impact_prediction_summary");

      // Key uses params needed to make it unique
      String key = consequence.getGeneAffectedId() + consequence.getAaMutation() + consequence.getType();
      if (consequencesMap.containsKey(key)) {
        Consequence c = consequencesMap.get(key);
        c.addTranscript(item);
        c.addFunctionalImpact(functionalImpact);
      } else {
        consequence.addTranscript(item);
        // Need symbol for UI, raw field get - no translation.
        consequence.setGeneAffectedSymbol((String) gene.get("symbol"));
        consequence.setGeneStrand(getLong(gene.get("strand")));
        consequence.addFunctionalImpact(functionalImpact);
        consequencesMap.put(key, consequence);
      }
    }

    // Just need the consequences we made.
    for (val c : consequencesMap.values()) {
      lst.add(c);
    }

    return lst;
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    else if (field instanceof Integer) return (long) (Integer) field;
    else if (field instanceof Float) return ((Float) field).longValue();
    else
      return null;
  }
}
