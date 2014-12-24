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

package org.icgc.dcc.portal.repository;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.service.QueryService.buildConsequenceFilters;
import static org.icgc.dcc.portal.service.QueryService.buildDonorFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneFilters;
import static org.icgc.dcc.portal.service.QueryService.buildMutationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildObservationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildPathwayFilters;
import static org.icgc.dcc.portal.service.QueryService.buildProjectFilters;
import static org.icgc.dcc.portal.service.QueryService.buildTranscriptFilters;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.hasPathway;
import static org.icgc.dcc.portal.service.QueryService.hasProject;
import static org.icgc.dcc.portal.service.QueryService.hasTranscript;
import static org.icgc.dcc.portal.service.QueryService.remapD2P;
import static org.icgc.dcc.portal.service.QueryService.remapG2P;
import static org.icgc.dcc.portal.service.QueryService.remapM2O;
import static org.icgc.dcc.portal.util.ElasticsearchUtils.addResponseIncludes;
import static org.icgc.dcc.portal.util.ElasticsearchUtils.processIncludes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class MutationRepository implements Repository {

  private static final Type CENTRIC_TYPE = Type.MUTATION_CENTRIC;
  private static final Type TYPE = Type.MUTATION;
  private static final Kind KIND = Kind.MUTATION;

  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.EMB_OCCURRENCE, "ssm_occurrence")
      .put(Kind.PROJECT, "ssm_occurrence")
      .put(Kind.DONOR, "ssm_occurrence")
      .put(Kind.TRANSCRIPT, "transcript")
      .put(Kind.GENE, "transcript")
      .put(Kind.PATHWAY, "transcript.gene.pathways")
      .put(Kind.OBSERVATION, "ssm_occurrence.observation")
      .build());

  static final ImmutableMap<Kind, String> PREFIX_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.EMB_OCCURRENCE, "ssm_occurrence")
      .put(Kind.PROJECT, "ssm_occurrence.project")
      .put(Kind.DONOR, "ssm_occurrence.donor")
      .put(Kind.TRANSCRIPT, "transcript")
      .put(Kind.CONSEQUENCE, "transcript.consequence")
      .put(Kind.GENE, "transcript.gene")
      .put(Kind.PATHWAY, "transcript.gene.pathways")
      .put(Kind.OBSERVATION, "ssm_occurrence.observation")
      .build());

  private static final ImmutableList<String> FACETS = ImmutableList.of("type", "consequenceTypeNested",
      "consequenceType", "platform", "verificationStatus", "platformNested", "verificationStatusNested",
      "functionalImpact", "functionalImpactNested", "sequencingStrategy", "sequencingStrategyNested");

  private final Client client;
  private final String index;

  @Autowired
  MutationRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  private List<TermsFacetBuilder> getFacets(Query query, ObjectNode filters) {
    val fs = Lists.<TermsFacetBuilder> newArrayList();
    if (query.hasInclude("facets")) {
      for (String facet : FACETS) {
        val tf = FacetBuilders.termsFacet(facet).field(FIELDS_MAPPING.get(KIND).get(facet)).size(MAX_FACET_TERM_COUNT);

        // For some reason these always have to be nested, unlike consequenceTypeNested which only needs it for filters
        if (facet.equals("platformNested") || facet.equals("verificationStatusNested")
            || facet.equals("sequencingStrategyNested")) {
          // needs to be nested
          tf.nested("ssm_occurrence.observation");
          tf.nested("ssm_occurrence");
        }

        if (filters.fieldNames().hasNext()) {
          val facetFilters = filters.deepCopy();
          if (facetFilters.has(KIND.getId())) {
            facetFilters.with(KIND.getId()).remove(facet);
          }

          // consequenceTypeNested is an internal facet that is needed to properly handle gene filters
          // Will later be intersected with consequenceType
          if (facet.equals("consequenceTypeNested") || facet.equals("functionalImpactNested")) {
            // Consequence type needs to be nested
            tf.nested("transcript");
            // consequenceTypeNested facet can only be filtered by gene
            if (facet.equals("consequenceTypeNested")) {
              facetFilters.retain("gene", "transcript");
            }
            if (facet.equals("functionalImpactNested")) {
              facetFilters.retain("gene", "consequence");
            }
          }

          // platformNested and verificationStatusNested are internal facets that are needed to properly handle donor
          // filters
          // Will later be intersected with platform and verificationStatus respectively
          else if (facet.equals("platformNested") || facet.equals("verificationStatusNested")
              || facet.equals("sequencingStrategyNested")) {
            // Can only be filtered by donor or project
            // facetFilters.retain("donor", "project");

            facetFilters.retain("observation");

            // Nested attributes at the same level should not filtered by itself, but
            // should be filtered by siblings
            if (facetFilters.get("observation") != null) {
              ObjectNode observationFilter = (ObjectNode) facetFilters.get("observation");
              if (facet.equals("platformNested")) {
                observationFilter.remove("platform");
              } else if (facet.equals("verificationStatusNested")) {
                observationFilter.remove("verificationStatus");
              } else if (facet.equals("sequencingStrategyNested")) {
                observationFilter.remove("sequencingStrategy");
              }
            }

          }

          tf.facetFilter(getFilters(facetFilters, facet));
        }
        fs.add(tf);
      }
    }
    return fs;
  }

  // Needed to check for consequenceTypeNested
  private FilterBuilder getFilters(ObjectNode filters, String facetName) {
    if (filters.fieldNames().hasNext()) {
      return buildFilters(filters, facetName);
    }
    return matchAllFilter();
  }

  private FilterBuilder buildFilters(ObjectNode filters, String facetName) {
    val qb = FilterBuilders.boolFilter();
    val musts = Lists.<FilterBuilder> newArrayList();
    boolean matchAll = true;

    boolean hasDonor = hasDonor(filters);
    boolean hasProject = hasProject(filters);
    boolean hasGene = hasGene(filters);
    boolean hasPathway = hasPathway(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasTranscript = hasTranscript(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasProject || hasGene || hasPathway || hasDonor || hasMutation || hasConsequence || hasTranscript
        || hasObservation) {
      matchAll = false;
      if (hasMutation) {
        musts.add(buildMutationFilters(filters, PREFIX_MAPPING));
      }
      if (hasGene || hasPathway || hasConsequence || hasTranscript) {
        val tb = FilterBuilders.boolFilter();
        val tMusts = Lists.<FilterBuilder> newArrayList();
        if (hasTranscript) tMusts.add(buildTranscriptFilters(filters, PREFIX_MAPPING));
        if (hasConsequence) tMusts.add(buildConsequenceFilters(filters, PREFIX_MAPPING));
        if (hasGene) tMusts.add(buildGeneFilters(filters, PREFIX_MAPPING));
        if (hasPathway) tMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.PATHWAY),
            buildPathwayFilters(filters, PREFIX_MAPPING)));
        tb.must(tMusts.toArray(new FilterBuilder[tMusts.size()]));

        // Facet is nested so filter cannot be nested
        if (facetName.equals("consequenceTypeNested") || facetName.equals("functionalImpactNested")) {
          musts.add(tb);
        } else {
          musts.add(nestedFilter(NESTED_MAPPING.get(Kind.TRANSCRIPT), tb));
        }
      }
      if (hasObservation || hasProject || hasDonor) {
        val ob = FilterBuilders.boolFilter();

        // Facet is nested so filter cannot be nested
        if (facetName.equals("platformNested") || facetName.equals("verificationStatusNested")
            || facetName.equals("sequencingStrategyNested")) {
          val observationMusts = Lists.<FilterBuilder> newArrayList();
          if (hasObservation) observationMusts.add(buildObservationFilters(filters, PREFIX_MAPPING));
          ob.must(observationMusts.toArray(new FilterBuilder[observationMusts.size()]));
          musts.add(ob);
        } else {
          val oMusts = buildOccurrenceNestedFilters(filters, hasDonor, hasProject, hasObservation);
          ob.must(oMusts.toArray(new FilterBuilder[oMusts.size()]));
          musts.add(nestedFilter(NESTED_MAPPING.get(Kind.EMB_OCCURRENCE), ob));
        }
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {
    val search = buildFindAllRequest(query, CENTRIC_TYPE);

    search.setQuery(buildQuery(query));

    log.info("{}", search);
    SearchResponse response = search.execute().actionGet();
    // log.info("{}", response);

    return response;
  }

  @Override
  public SearchResponse findAll(Query query) {
    val search = buildFindAllRequest(query, TYPE);

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    val search = client
        .prepareSearch(index)
        .setTypes(type.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize());

    ObjectNode filters = remapFilters(query.getFilters());
    search.setPostFilter(getFilters(filters, ""));
    search.addFields(getFields(query, KIND));
    processIncludes(search, query);

    val facets = getFacets(query, filters);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    String sort = FIELDS_MAPPING.get(KIND).get(query.getSort());
    search.addSort(fieldSort(sort).order(query.getOrder()));
    if (!sort.equals("_score")) search.addSort("_score", SortOrder.DESC);

    return search;
  }

  public ObjectNode remapFilters(ObjectNode filters) {
    return remapM2T(remapM2O(remapM2C(remapG2P(remapD2P(filters)))));
  }

  protected FilterBuilder buildScoreFilters(Query query) {
    ObjectNode filters =
        query.hasScoreFilters() ? remapFilters(query.getScoreFilters()) : remapFilters(query.getFilters());

    val qb = FilterBuilders.boolFilter();

    boolean matchAll = true;

    boolean hasDonor = hasDonor(filters);
    boolean hasProject = hasProject(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasProject || hasDonor || hasObservation) {
      matchAll = false;
      val oMusts = buildOccurrenceNestedFilters(filters, hasDonor, hasProject, hasObservation);
      qb.must(oMusts.toArray(new FilterBuilder[oMusts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
  }

  private List<FilterBuilder> buildOccurrenceNestedFilters(
      ObjectNode filters, boolean hasDonor, boolean hasProject, boolean hasObservation) {
    val oMusts = Lists.<FilterBuilder> newArrayList();
    if (hasProject) oMusts.add(buildProjectFilters(filters, PREFIX_MAPPING));
    // if (hasObservation) oMusts.add(buildObservationFilters(filters, PREFIX_MAPPING));
    if (hasObservation) {
      oMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION), buildObservationFilters(filters, PREFIX_MAPPING)));
    }
    if (hasDonor) oMusts.add(buildDonorFilters(filters, PREFIX_MAPPING));
    return oMusts;
  }

  @Override
  public long count(Query query) {
    val search = buildCountRequest(query, CENTRIC_TYPE);

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();
    for (val id : queries.keySet()) {
      search.add(buildCountRequest(queries.get(id), CENTRIC_TYPE));
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();
    for (val id1 : queries.keySet()) {
      val nestedQuery = queries.get(id1);
      for (val id2 : nestedQuery.keySet()) {
        search.add(buildCountRequest(nestedQuery.get(id2), CENTRIC_TYPE));
      }
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  public SearchRequestBuilder buildCountRequest(Query query, Type type) {
    val search = client.prepareSearch(index).setTypes(type.getId()).setSearchType(COUNT);

    if (query.hasFilters()) {
      ObjectNode filters = query.hasScoreFilters() ? query.getScoreFilters() : query.getFilters();
      filters = remapFilters(filters);
      search.setPostFilter(buildFilters(filters, ""));

      search.setQuery(buildQuery(query));
    }
    return search;
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    return nestedQuery("ssm_occurrence",
        constantScoreQuery(filteredQuery(matchAllQuery(), buildScoreFilters(query))).boost(1.0f))
        .scoreMode("total");
  }

  public Map<String, Object> findOne(String id, Query query) {
    val fieldMapping = FIELDS_MAPPING.get(KIND);
    val search = client.prepareGet(index, CENTRIC_TYPE.getId(), id);
    processFields(search, query, fieldMapping);
    processIncludes(search, query);
    val response = search.execute().actionGet();

    if (!response.isExists()) {
      String type = KIND.getId().substring(0, 1).toUpperCase() + KIND.getId().substring(1);
      log.info("{} {} not found.", type, id);
      String msg = String.format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(msg).build());
    }

    val map = createResponseMap(response, query, fieldMapping);
    log.debug("{}", map);

    return map;
  }

  private static void processFields(GetRequestBuilder search, Query query,
      ImmutableMap<String, String> validFieldsMapping) {
    List<String> requestFields = Lists.<String> newArrayList();
    if (query.hasFields()) {

      for (String field : query.getFields()) {
        if (validFieldsMapping.containsKey(field)) {
          requestFields.add(validFieldsMapping.get(field));
        }
      }
    } else
      requestFields.addAll(validFieldsMapping.values().asList());

    search.setFields(requestFields.toArray(new String[requestFields.size()]));
  }

  private static Map<String, Object> createResponseMap(GetResponse response, Query query,
      ImmutableMap<String, String> fieldMapping) {
    val map = Maps.<String, Object> newHashMap();
    val fieldsList = Lists.newArrayList(
        fieldMapping.get("platform"),
        fieldMapping.get("consequenceType"),
        fieldMapping.get("verificationStatus"),
        fieldMapping.get("sequencingStrategy"),
        fieldMapping.get("affectedProjectIds"),
        fieldMapping.get("functionalImpact"));

    for (val field : response.getFields().values()) {
      if (fieldsList.contains(field.getName())) {
        map.put(field.getName(), field.getValues());
      } else {
        map.put(field.getName(), field.getValue());
      }
    }

    addResponseIncludes(query, response, map);

    return map;
  }

  public SearchResponse protein(Query query) {
    ImmutableMap<String, String> fields = FIELDS_MAPPING.get(KIND);

    val search =
        client.prepareSearch(index).setTypes(CENTRIC_TYPE.getId()).setSearchType(QUERY_THEN_FETCH).setFrom(1)
            .setSize(10000);

    search.setPostFilter(getFilters(query.getFilters(), null));

    search.addFields(new String[] {
        fields.get("id"),
        fields.get("mutation"),
        fields.get("affectedDonorCountTotal"),
        fields.get("functionalImpact"),
        fields.get("transcriptId"),
        "transcript.consequence.aa_mutation",
        "transcript.functional_impact_prediction_summary"
    });

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  // NOTE: This changes the filter structure
  // Moves mutation: {consequenceType} -> consequence: {type} because consequence type needs custom query
  private static ObjectNode remapM2C(ObjectNode filters) {
    // Needed only if both mutation and gene filters found, otherwise consequence can be on its own.
    if (filters.has("mutation")) {
      val mutation = (ObjectNode) filters.get("mutation");
      val consequence = new ObjectMapper().createObjectNode();
      if (mutation.has("consequenceType")) {
        consequence.put("type", mutation.remove("consequenceType"));
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
  // Moves mutation: functionalImpact -> transcript: functionalImpact
  private static ObjectNode remapM2T(ObjectNode filters) {
    if (filters.has("mutation")) {
      val mutation = (ObjectNode) filters.get("mutation");
      val transcript = new ObjectMapper().createObjectNode();
      if (mutation.has("functionalImpact")) {
        transcript.put("functionalImpact", mutation.remove("functionalImpact"));
      }
      if (transcript.fieldNames().hasNext()) {
        filters.put("transcript", transcript);
      }
      if (mutation.fieldNames().hasNext()) {
        filters.replace("mutation", mutation);
      } else {
        filters.remove("mutation");
      }
    }

    return filters;
  }
}
