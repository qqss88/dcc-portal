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

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.model.IndexModel.INPUT_GENE_LIST_ID;
import static org.icgc.dcc.portal.model.IndexModel.IS;
import static org.icgc.dcc.portal.model.IndexModel.Kind.GENE;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@NoArgsConstructor(access = PRIVATE)
public final class Filters {

  public static ObjectNode emptyFilter() {
    return MAPPER.createObjectNode();
  }

  public static ObjectNode uploadedGeneListFilter(@NonNull String geneListId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("uploadedGeneList", is(geneListId));

    return geneFilter;
  }

  public static ObjectNode pathwayFilter() {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("hasPathway", true);

    return geneFilter;
  }

  public static ObjectNode geneSetFilter(@NonNull String geneSetId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("geneSetId", is(geneSetId));

    return geneFilter;
  }

  public static ObjectNode goTermFilter(@NonNull String goTermId) {
    val geneFilter = geneFilter();
    geneFilter.with(GENE.getId()).put("goTermId", is(goTermId));

    return geneFilter;
  }

  public static ObjectNode geneFilter() {
    return entityFilter(GENE.getId());
  }

  public static ObjectNode enrichmentAnalysisFilter(@NonNull UUID analysisId) {
    val analysisFilter = geneFilter();
    analysisFilter.with(GENE.getId()).put(INPUT_GENE_LIST_ID, is(analysisId.toString()));

    return analysisFilter;
  }

  public static ObjectNode andFilter(ObjectNode... filters) {
    JsonNode left = filters[0];
    for (int i = 1; i < filters.length; i++) {
      val right = filters[i];

      left = andFilter(left, right);
    }

    return (ObjectNode) left;
  }

  private static JsonNode andFilter(JsonNode left, JsonNode right) {
    val and = MAPPER.createObjectNode();

    if (right.getNodeType() == left.getNodeType()) {

      //
      // Same types
      //

      // Arbitrary
      val field = left;

      if (field.isObject()) {
        val fieldNames = ImmutableSet.<String> builder().addAll(left.fieldNames()).addAll(right.fieldNames()).build();
        for (val fieldName : fieldNames) {
          val leftField = left.path(fieldName);
          val rightField = right.path(fieldName);
          val andField = andFilter(leftField, rightField);

          and.put(fieldName, andField);
        }
      } else if (field.isArray()) {
        val values = Sets.<Object> newLinkedHashSet();
        for (val element : Iterables.concat(left, right)) {
          if (element.isNumber()) {
            values.add(element.asInt());
          } else if (element.isBoolean()) {
            values.add(element.asBoolean());
          } else if (element.isTextual()) {
            values.add(element.asText());
          } else if (element.isPojo()) {
            val pojo = (POJONode) element;
            val value = pojo.getPojo();
            values.add(value);
          } else {
            values.add(element);
          }
        }

        val result = MAPPER.createArrayNode();
        for (val value : values) {
          result.addPOJO(value);
        }

        return result;
      } else if (field.isValueNode()) {
        val result = MAPPER.createArrayNode();
        result.add(left);
        result.add(right);

        return result;
      } else if (field.isMissingNode()) {
        // Can't happen
      }
    } else {

      //
      // Different types
      //

      if (left.isMissingNode()) {
        return right;
      } else if (right.isMissingNode()) {
        return left;
      }
    }

    return and;
  }

  public static ObjectNode entityFilter(@NonNull String entityName) {
    val entityFilter = MAPPER.createObjectNode();
    entityFilter.with(entityName);

    return entityFilter;
  }

  private static ObjectNode is(@NonNull String value) {
    val is = MAPPER.createObjectNode();
    is.withArray(IS).add(value);

    return is;
  }

}
