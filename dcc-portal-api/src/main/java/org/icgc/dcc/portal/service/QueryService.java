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

import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.numericRangeFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.icgc.dcc.common.core.util.FormatUtils._;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.IS;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.model.IndexModel.MISSING;
import static org.icgc.dcc.portal.model.IndexModel.NOT;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;
import static org.icgc.dcc.portal.util.LocationUtils.parseLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
public class QueryService {

  // Gene set fields are "virtual", in the sense that they are not 1-to-1 with the index structure
  private static List<String> geneSetFields = ImmutableList.<String> of("geneSetId", "pathwayId", "goTermId",
      "curatedSetId");

  private static Map<String, String> geneSetFieldTypeMap = ImmutableMap.<String, String> of(
      "pathwayId", "pathway",
      "goTermId", "go_term",
      "curatedSetId", "curated_set"
      );

  private static List<String> geneSetExistFields = ImmutableList.<String> of("hasPathway", "hasGoterm", "hasCurated");

  private static List<String> locationFields = Lists.newArrayList("location", "transcript.gene.location",
      "gene.location", "ssm.consequence.gene.location", "ssm.location", "donor.ssm.location", "gene.ssm.location");

  public static FilterBuilder getFilters(ObjectNode filters, Kind kind) {
    if (filters.fieldNames().hasNext()) return buildFilters(filters, kind);
    return matchAllFilter();
  }

  public static FilterBuilder getFilters(ObjectNode filters, Kind kind, ImmutableMap<Kind, String> nestedMapping,
      ImmutableMap<Kind, String> prefixMapping) {
    if (filters.fieldNames().hasNext()) {
      return buildFilters(filters, kind, nestedMapping, prefixMapping);
    }
    return matchAllFilter();
  }

  public static String[] getFields(Query query, Kind kind) {
    val typeMap = FIELDS_MAPPING.get(kind);
    if (query.hasFields()) {
      val fs = Lists.<String> newArrayList();
      for (String field : query.getFields()) {
        if (typeMap.containsKey(field)) {
          fs.add(typeMap.get(field));
        }
      }
      return fs.toArray(new String[fs.size()]);
    } else {
      val fs = typeMap.values();
      return fs.toArray(new String[fs.size()]);
    }
  }

  public static List<TermsFacetBuilder> getFacets(Query query, Kind kind, ImmutableList<String> facets,
      ObjectNode filters, ImmutableMap<Kind, String> nestedMapping, ImmutableMap<Kind, String> prefixMapping) {
    val fs = Lists.<TermsFacetBuilder> newArrayList();
    if (query.hasInclude("facets")) {
      for (String facet : facets) {
        val tf = FacetBuilders.termsFacet(facet).field(FIELDS_MAPPING.get(kind).get(facet)).size(MAX_FACET_TERM_COUNT);

        if (filters.fieldNames().hasNext()) {
          val facetFilters = filters.deepCopy();
          if (facetFilters.has(kind.getId())) {
            facetFilters.with(kind.getId()).remove(facet);
          }

          if (nestedMapping != null) {
            tf.facetFilter(getFilters(facetFilters, kind, nestedMapping, prefixMapping));
          } else {
            tf.facetFilter(getFilters(facetFilters, kind));
          }
        }
        fs.add(tf);
      }
    }
    return fs;
  }

  public static FilterBuilder buildFilters(ObjectNode filters, Kind kind,
      ImmutableMap<Kind, String> nestedMapping, ImmutableMap<Kind, String> prefixMapping) {
    val qb = FilterBuilders.boolFilter();
    boolean matchAll = true;
    for (Kind k : Kind.values()) {
      if (hasFilter(filters, k)) {
        matchAll = false;
        val termFilters = buildTypeFilters(filters, k, prefixMapping);
        if (k.equals(kind) || !nestedMapping.containsKey(k)) qb.must(termFilters);
        else {
          qb.must(nestedFilter(nestedMapping.get(k), termFilters));
        }
      }
    }
    return matchAll ? matchAllFilter() : qb;
  }

  public static FilterBuilder buildFilters(ObjectNode filters, Kind kind) {
    if (hasFilter(filters, kind)) {
      return buildTypeFilters(filters, kind, null);
    } else {
      return matchAllFilter();
    }
  }

  public static BoolFilterBuilder buildGeneFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.GENE, prefixMapping);
  }

  public static BoolFilterBuilder buildDonorFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.DONOR, prefixMapping);
  }

  public static BoolFilterBuilder buildProjectFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.PROJECT, prefixMapping);
  }

  public static BoolFilterBuilder buildMutationFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.MUTATION, prefixMapping);
  }

  public static BoolFilterBuilder buildConsequenceFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.CONSEQUENCE, prefixMapping);
  }

  public static BoolFilterBuilder buildTranscriptFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.TRANSCRIPT, prefixMapping);
  }

  public static BoolFilterBuilder buildGeneSetFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.GENE_SET, prefixMapping);
  }

  public static BoolFilterBuilder buildEmbOccurrenceFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.EMB_OCCURRENCE, prefixMapping);
  }

  public static BoolFilterBuilder buildObservationFilters(ObjectNode filters, ImmutableMap<Kind, String> prefixMapping) {
    return buildTypeFilters(filters, Kind.OBSERVATION, prefixMapping);
  }

  // Test
  // TODO: Must_not has to be above nested sets
  public static void main(String args[]) throws JsonProcessingException, IOException {

    ObjectNode objNode =
        (ObjectNode) MAPPER
            .readTree(
            // "{'gene':{'id':{'is': ['abc']}, 'goterm':{'is':['go1', 'go2'], 'not':['go3']}, 'pathway':{'is':['pathway1']}, 'hasPathway':true   }}"
            "{'gene':{'goterm':{'is':['go1', 'go2'], 'not':['go3']}, hasPathway:false }}"
            );

    ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap.of(
        Kind.DONOR, "donor",
        Kind.MUTATION, "donor.ssm",
        Kind.CONSEQUENCE, "donor.ssm.consequence",
        Kind.OBSERVATION, "donor.ssm.observation"));

    val filter = QueryService.buildTypeFilters(objNode, Kind.GENE, NESTED_MAPPING);
    log.info("!!!!!!!!!!\n{}", filter);
  }

  public static BoolFilterBuilder buildTypeFilters(ObjectNode filters, Kind kind,
      ImmutableMap<Kind, String> prefixMapping) {

    log.info("Building type filters {} {}", prefixMapping.get(kind.getId()), filters);

    val termFilters = FilterBuilders.boolFilter();

    val genePath = filters.path(Kind.GENE_SET.getId());
    ArrayList<NestedFilterBuilder> geneSetInclusionFilters = Lists.<NestedFilterBuilder> newArrayList();
    ArrayList<NestedFilterBuilder> geneSetExclusionFilters = Lists.<NestedFilterBuilder> newArrayList();

    ArrayList<NestedFilterBuilder> geneSetExistFilters = Lists.<NestedFilterBuilder> newArrayList();
    ArrayList<NestedFilterBuilder> geneSetNotExistFilters = Lists.<NestedFilterBuilder> newArrayList();

    for (val geneSetField : geneSetFields) {

      // FIXME: move out
      if (!genePath.path(geneSetField).isMissingNode() && kind.getId().equals("geneSet")) {

        for (String bool : Lists.newArrayList(IS, NOT)) {
          val boolNode = genePath.path(geneSetField).path(bool);
          if (!boolNode.isMissingNode() && boolNode.isArray()) {
            List<String> termList = Lists.newArrayList();
            for (val item : boolNode) {
              termList.add(item.textValue());
            }

            val nestedPrefix = prefixMapping.get(Kind.GENE_SET);

            NestedFilterBuilder nestedFilter;
            // val idFilter = termsFilter("id", termList);
            val idFilter = termsFilter(_("%s.%s", nestedPrefix, "id"), termList);

            // If specified by the virtual fields, we need to add a type limition
            if (geneSetFieldTypeMap.containsKey(geneSetField)) {
              // val typeFilter = termsFilter("type", geneSetFieldTypeMap.get(geneSetField));
              val typeFilter = termsFilter(_("%s.%s", nestedPrefix, "type"), geneSetFieldTypeMap.get(geneSetField));
              nestedFilter =
                  FilterBuilders.nestedFilter(prefixMapping.get(Kind.GENE_SET),
                      FilterBuilders.boolFilter().must(idFilter, typeFilter));
            } else {
              nestedFilter =
                  FilterBuilders.nestedFilter(prefixMapping.get(Kind.GENE_SET),
                      FilterBuilders.boolFilter().must(idFilter));
            }

            if (bool.equals(IS)) {
              geneSetInclusionFilters.add(nestedFilter);
            } else {
              geneSetExclusionFilters.add(nestedFilter);
            }

          }
        }
      }
    }

    if (geneSetInclusionFilters.size() > 0) {
      val mustGeneSetFilter = FilterBuilders.boolFilter();
      for (val geneSetInclusionFilter : geneSetInclusionFilters) {
        mustGeneSetFilter.must(geneSetInclusionFilter);
      }
      termFilters.must(mustGeneSetFilter);
    }
    if (geneSetExclusionFilters.size() > 0) {
      val mustGeneSetFilter = FilterBuilders.boolFilter();
      for (val geneSetExclusionFilter : geneSetExclusionFilters) {
        mustGeneSetFilter.mustNot(geneSetExclusionFilter);
      }
      termFilters.must(mustGeneSetFilter);
    }

    for (val geneSetExistField : geneSetExistFields) {

      if (!genePath.path(geneSetExistField).isMissingNode()) {
        val exist = genePath.path(geneSetExistField).asBoolean();

        val filter =
            nestedFilter(prefixMapping.get(Kind.GENE_SET),
                FilterBuilders.boolFilter().must(termFilter("type", geneSetExistField)));
        if (exist) {
          geneSetExistFilters.add(filter);
        } else {
          geneSetNotExistFilters.add(filter);
        }
      }
    }

    if (geneSetExistFilters.size() > 0) {
      val mustGeneSetFilter = FilterBuilders.boolFilter();
      for (val geneSetExistFilter : geneSetExistFilters) {
        mustGeneSetFilter.must(geneSetExistFilter);
      }
      termFilters.must(mustGeneSetFilter);
    }

    if (geneSetNotExistFilters.size() > 0) {
      val mustGeneSetFilter = FilterBuilders.boolFilter();
      for (val geneSetNotExistFilter : geneSetNotExistFilters) {
        mustGeneSetFilter.mustNot(geneSetNotExistFilter);
      }
      termFilters.must(mustGeneSetFilter);
    }

    // log.info("After building : {}", termFilters);

    val fields = filters.path(kind.getId()).fields();
    while (fields.hasNext()) {
      val facetField = fields.next();

      // Skip the virtual fields
      if (geneSetFields.contains(facetField)) continue;
      if (geneSetExistFields.contains(facetField)) continue;

      // Check that facet field is in Field Mapping
      val typeMapping = FIELDS_MAPPING.get(kind);
      if (typeMapping.containsKey(facetField.getKey())) {
        String fieldName = typeMapping.get(facetField.getKey());
        if (prefixMapping != null && prefixMapping.containsKey(kind)) {
          fieldName = String.format("%s.%s", prefixMapping.get(kind), fieldName);
        }

        // Check for IS / IS NOT
        JsonNode boolNode = facetField.getValue();
        for (String bool : Lists.newArrayList(IS, NOT)) {
          // Just a place holder that will be overridden with a more specific kind of filter
          FilterBuilder fb;
          if (boolNode.has(bool)) {
            if (boolNode.get(bool).isArray()) {
              val items = Lists.<String> newArrayList();
              // Converts from JsonNodes to List<String> for termsFilter
              for (val item : boolNode.get(bool)) {
                items.add(item.textValue());
              }
              if (locationFields.contains(fieldName)) {
                fb = locationFilters(kind, items, typeMapping, prefixMapping);
              } else {
                if (items.remove(MISSING)) {
                  val bf = FilterBuilders.boolFilter();
                  val mf = missingFilter(fieldName).existence(true).nullValue(false);
                  bf.should(mf);
                  if (!items.isEmpty()) {
                    bf.should(termsFilter(fieldName, items));
                  }
                  fb = bf;
                } else {
                  fb = termsFilter(fieldName, items);
                }
              }
            } else {
              String value = boolNode.get(bool).textValue();
              if (locationFields.contains(fieldName)) {
                fb = locationFilter(kind, value, typeMapping, prefixMapping);
              } else if (value.equals(MISSING)) {
                fb = missingFilter(fieldName);
              } else {
                fb = termFilter(fieldName, value);
              }
            }

            // TODO: termFilters must exist / fb
            // {gene: {type:{is:[], exists:true}}}

            if (bool.equals(IS)) {
              termFilters.must(fb);
            } else if (bool.equals(NOT)) {
              termFilters.mustNot(fb);
            }
          }
        }

      }
    }
    return termFilters;
  }

  public static FilterBuilder locationFilters(Kind kind, List<String> locations,
      ImmutableMap<String, String> typeMapping, ImmutableMap<Kind, String> prefixMapping) {
    val locationFilters = FilterBuilders.boolFilter();
    for (String location : locations) {
      locationFilters.must(locationFilter(kind, location, typeMapping, prefixMapping));
    }
    return locationFilters;
  }

  public static FilterBuilder locationFilter(Kind kind, String value, ImmutableMap<String, String> typeMapping,
      ImmutableMap<Kind, String> prefixMapping) {
    val locationFilter = FilterBuilders.boolFilter();

    val location = parseLocation(value);

    // Nested fields
    String prefix = "";
    if (prefixMapping != null && prefixMapping.containsKey(kind)) {
      prefix = String.format("%s.", prefixMapping.get(kind));
    }

    // Constrain chromosome
    locationFilter.must(FilterBuilders.termFilter(prefix + typeMapping.get("chromosome"), location.getChromosome()));

    if (location.hasStart()) {
      locationFilter.must(numericRangeFilter(prefix + typeMapping.get("end")).gte(location.getStart()));
    }

    if (location.hasEnd()) {
      locationFilter.must(numericRangeFilter(prefix + typeMapping.get("start")).lte(location.getEnd()));
    }

    return locationFilter;
  }

  // NOTE: This changes the filter structure so the Filter Building logic doesn't have to change.
  // Moves donor: {projectId,primarySite} -> project: {id,primarySite} because mutation index structure doesn't nest
  // project under donor
  // Making the change here instead of globally because donor facets don't work probably with project filters and this
  // keeps the QueryService code from branching
  public static ObjectNode remapD2P(ObjectNode filters) {
    if (filters.has("donor")) {
      val donor = (ObjectNode) filters.get("donor");
      val project = new ObjectMapper().createObjectNode();
      if (donor.has("primarySite")) {
        project.put("primarySite", donor.remove("primarySite"));
      }
      if (donor.has("projectId")) {
        project.put("id", donor.remove("projectId"));
      }
      if (project.fieldNames().hasNext()) {
        filters.put("project", project);
      }
      if (donor.fieldNames().hasNext()) {
        filters.replace("donor", donor);
      } else {
        filters.remove("donor");
      }
    }
    return filters;
  }

  // NOTE: This changes the filter structure so the Filter Building logic doesn't have to change.
  // Moves gene: {pathwayId} -> pathway: {id} because pathways are nested
  public static ObjectNode remapG2P(ObjectNode filters) {
    log.info("Before {}", filters);
    if (filters.has("gene")) {
      val gene = (ObjectNode) filters.get("gene");

      val geneSet = new ObjectMapper().createObjectNode();
      val geneSetList = ImmutableList.<String> of("geneSetId", "pathwayId", "goTermId", "curatedSetId");

      for (val geneSetIdentifier : geneSetList) {
        if (gene.has(geneSetIdentifier)) {
          geneSet.put(geneSetIdentifier, gene.remove(geneSetIdentifier));
        }
      }

      if (geneSet.fieldNames().hasNext()) {
        filters.put("geneSet", geneSet);
      }
      if (gene.fieldNames().hasNext()) {
        filters.replace("gene", gene);
      } else {
        filters.remove("gene");
      }
    }

    log.info("After filters {}", filters);
    return filters;
  }

  // NOTE: This changes the filter structure
  // Moves mutation: {consequenceType} -> consequence: {type} because consequence type needs custom query
  public static ObjectNode remapM2C(ObjectNode filters) {
    // Needed only if both mutation and gene filters found, otherwise consequence can be on its own.
    if (filters.has("mutation")) {
      val mutation = (ObjectNode) filters.get("mutation");
      val consequence = new ObjectMapper().createObjectNode();
      if (mutation.has("consequenceType")) {
        consequence.put("type", mutation.remove("consequenceType"));
      }
      if (mutation.has("functionalImpact")) {
        consequence.put("functionalImpact", mutation.remove("functionalImpact"));
      }
      if (consequence.fieldNames().hasNext()) {
        filters.put("consequence", consequence);
      }
      if (mutation.fieldNames().hasNext()) {
        filters.replace("mutation", mutation);
      } else {
        filters.remove("mutation");
      }
    }

    return filters;
  }

  // NOTE: This changes the filter structure
  // Moves mutation: {platform, verificationStatus} -> observation: {platform, verificationStatus} because they need
  // custom query
  public static ObjectNode remapM2O(ObjectNode filters) {
    // Needed only if both mutation and donor filters found, otherwise they can be on their own.
    if (filters.has("mutation")) {
      val mutation = (ObjectNode) filters.get("mutation");
      val observation = new ObjectMapper().createObjectNode();
      if (mutation.has("platform")) {
        observation.put("platform", mutation.remove("platform"));
      }
      if (mutation.has("verificationStatus")) {
        observation.put("verificationStatus", mutation.remove("verificationStatus"));
      }
      if (mutation.has("sequencingStrategy")) {
        observation.put("sequencingStrategy", mutation.remove("sequencingStrategy"));
      }

      if (observation.fieldNames().hasNext()) {
        filters.put("observation", observation);
      }
      if (mutation.fieldNames().hasNext()) {
        filters.replace("mutation", mutation);
      } else {
        filters.remove("mutation");
      }
    }
    return filters;
  }

  static public final Boolean hasFilter(ObjectNode filters, Kind kind) {
    return filters.has(kind.getId()) && filters.path(kind.getId()).fieldNames().hasNext();
  }

  static public final Boolean hasDonor(ObjectNode filters) {
    return hasFilter(filters, Kind.DONOR);
  }

  static public final Boolean hasProject(ObjectNode filters) {
    return hasFilter(filters, Kind.PROJECT);
  }

  static public final Boolean hasOccurrence(ObjectNode filters) {
    return hasFilter(filters, Kind.EMB_OCCURRENCE);
  }

  static public final Boolean hasGene(ObjectNode filters) {
    return hasFilter(filters, Kind.GENE);
  }

  static public final Boolean hasMutation(ObjectNode filters) {
    return hasFilter(filters, Kind.MUTATION);
  }

  static public final Boolean hasConsequence(ObjectNode filters) {
    return hasFilter(filters, Kind.CONSEQUENCE);
  }

  static public final Boolean hasObservation(ObjectNode filters) {
    return hasFilter(filters, Kind.OBSERVATION);
  }

  static public final Boolean hasGeneSet(ObjectNode filters) {
    return hasFilter(filters, Kind.GENE_SET) ||
        (filters.has("pathway") && filters.path("pathway").fieldNames().hasNext());
  }

  static public final Boolean hasTranscript(ObjectNode filters) {
    return hasFilter(filters, Kind.TRANSCRIPT);
  }
}
