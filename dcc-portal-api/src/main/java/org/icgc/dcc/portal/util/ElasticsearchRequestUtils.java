/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.util;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;

import com.google.common.collect.Lists;

@NoArgsConstructor(access = PRIVATE)
public class ElasticsearchRequestUtils {

  public static final String[] EMPTY_SOURCE_FIELDS = null;

  public static String[] resolveSourceFields(Query query, Kind kind) {
    val sourceFields = getSource(query, kind);
    if (sourceFields.isEmpty()) {
      return EMPTY_SOURCE_FIELDS;
    }

    return sourceFields.toArray(new String[sourceFields.size()]);
  }

  private static List<String> getSource(Query query, Kind kind) {
    switch (kind) {
    case MUTATION:
      return prepareMutationIncludes(query);
    case DONOR:
      return prepareDonorIncludes(query);
    case GENE:
      return prepareGeneIncludes(query, kind);
    case PATHWAY:
      return preparePathwayIncludes(query);
    case PROJECT:
      return prepareProjectIncludes(query, kind);
    case OCCURRENCE:
      return prepareOccurrenceIncludes(query, kind);
    case GENE_SET:
      return prepareGeneSetIncludes(query, kind);
    default:
      return emptyList();
    }
  }

  private static List<String> prepareGeneSetIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "hierarchy", "inferredTree", "synonyms", "altIds");
  }

  private static List<String> prepareOccurrenceIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "observation");
  }

  private static List<String> prepareMutationIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("transcripts") || query.hasInclude("consequences")) {
      sourceFields.add("transcript");
    }

    if (query.hasInclude("occurrences")) {
      sourceFields.add("ssm_occurrence");
    }

    return sourceFields;
  }

  private static List<String> prepareDonorIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("specimen")) {
      sourceFields.add("specimen");
    }

    return sourceFields;
  }

  private static List<String> prepareGeneIncludes(Query query, Kind kind) {
    val sourceFields = Lists.<String> newArrayList();

    // external_db_ids and pathways are objects. Fields support only leaf nodes, that's why they must be included in
    // source
    sourceFields.addAll(resolveFields(query, kind, "externalDbIds", "pathways", "sets"));

    if (query.hasInclude("transcripts")) {
      sourceFields.add("transcripts");
    }

    if (query.hasInclude("projects")) {
      sourceFields.add("project");
    }

    if (query.hasInclude("pathways")) {
      sourceFields.add("pathways");
    }

    return sourceFields;
  }

  private static List<String> resolveFields(Query query, Kind kind, String... fields) {
    val result = Lists.<String> newArrayList();
    val typeFieldsMap = FIELDS_MAPPING.get(kind);
    val queryFields = query.getFields();
    for (val field : fields) {
      if (!query.hasFields() || queryFields.contains(field)) {
        result.add(typeFieldsMap.get(field));
      }
    }

    return result;
  }

  private static List<String> preparePathwayIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("projects")) {
      sourceFields.add("projects");
    }

    return sourceFields;
  }

  private static List<String> prepareProjectIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "experimentalAnalysisPerformedDonorCounts",
        "experimentalAnalysisPerformedSampleCounts");
  }

}