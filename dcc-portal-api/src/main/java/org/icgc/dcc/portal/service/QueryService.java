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

package org.icgc.dcc.portal.service;

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

@Slf4j
@Deprecated
public class QueryService {

  public static String[] getFields(Query query, Kind kind) {
    val typeFieldsMap = FIELDS_MAPPING.get(kind);
    val result = Lists.<String> newArrayList();

    if (query.hasFields()) {
      for (val field : query.getFields()) {
        if (typeFieldsMap.containsKey(field)) {
          result.add(typeFieldsMap.get(field));
        }
      }
    } else {
      result.addAll(typeFieldsMap.values().asList());
    }
    clearInvalidFields(result, kind);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Remove fields that are objects in ES. They must be retrieved from source
   */
  private static void clearInvalidFields(List<String> fields, Kind kind) {
    val typeFieldsMap = FIELDS_MAPPING.get(kind);

    switch (kind) {
    case GENE:
      fields.remove(typeFieldsMap.get("externalDbIds"));
      fields.remove(typeFieldsMap.get("pathways"));
      break;
    case PROJECT:
      fields.remove(typeFieldsMap.get("experimentalAnalysisPerformedDonorCounts"));
      fields.remove(typeFieldsMap.get("experimentalAnalysisPerformedSampleCounts"));
      break;
    case OCCURRENCE:
      fields.remove(typeFieldsMap.get("observation"));
      break;
    case GENE_SET:
      fields.remove(typeFieldsMap.get("hierarchy"));
      fields.remove(typeFieldsMap.get("inferredTree"));
      fields.remove(typeFieldsMap.get("synonyms"));
      fields.remove(typeFieldsMap.get("altIds"));
      break;
    }
  }

  static public final Boolean hasFilter(ObjectNode filters, Kind kind) {
    return filters.has(kind.getId()) && filters.path(kind.getId()).fieldNames().hasNext();
  }

  // Default to donors with molecular information for donor-centric type
  public static FilterBuilder defaultDonorFilter() {
    return FilterBuilders.termFilter("_summary._complete", true);
  }

  // Defaults to projects with at least 1 donor with molecular information
  public static FilterBuilder defaultProjectFilter() {
    return FilterBuilders.termFilter("_summary._complete", true);
  }

  // Convert user specified state to elastic search complete field values
  private static List<Boolean> convertEntityState(List<String> uiStates) {
    val list = Lists.<Boolean> newArrayList();
    for (val state : uiStates) {
      if (state.equalsIgnoreCase("pending")) {
        list.add(false);
      } else if (state.equalsIgnoreCase("live")) {
        list.add(true);
      } else if (state.equalsIgnoreCase("*")) {
        list.add(false);
        list.add(true);
      }
    }

    // Default if no value or only invalid values
    if (list.isEmpty()) {
      list.add(true);
    }

    return list;
  }

}
