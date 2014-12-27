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

import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
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
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.hasPathway;
import static org.icgc.dcc.portal.service.QueryService.remapG2P;
import static org.icgc.dcc.portal.service.QueryService.remapM2C;
import static org.icgc.dcc.portal.service.QueryService.remapM2O;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.addIncludes;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class DonorRepository implements Repository {

  private static final String SCORE =
      "g = doc['gene._gene_id']; x = doc['gene._summary._ssm_count']; g.value == '' || g.empty || x.empty || x.value < 1 ? 0 : 1";

  private static final Type CENTRIC_TYPE = Type.DONOR_CENTRIC;
  private static final Type TYPE = Type.DONOR;
  private static final Kind KIND = Kind.DONOR;

  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.PROJECT, "project")
      .put(Kind.GENE, "gene")
      .put(Kind.MUTATION, "gene.ssm")
      .put(Kind.PATHWAY, "gene.pathways")
      .put(Kind.CONSEQUENCE, "gene.ssm.consequence")
      .put(Kind.OBSERVATION, "gene.ssm.observation")
      .build());

  static final ImmutableMap<Kind, String> PREFIX_MAPPING = NESTED_MAPPING;

  private static final ImmutableList<String> FACETS = ImmutableList.of("projectId", "primarySite", "gender",
      "tumourStageAtDiagnosis", "vitalStatus", "diseaseStatusLastFollowup", "relapseType", "ageAtDiagnosisGroup",
      "availableDataTypes", "analysisTypes", "projectName");

  private static final int SCAN_BATCH_SIZE = 1000;

  private final static TimeValue KEEP_ALIVE = new TimeValue(10000);

  private final Client client;
  private final String index;

  @Autowired
  DonorRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  private List<TermsFacetBuilder> getFacets(Query query, ObjectNode filters) {
    val fs = Lists.<TermsFacetBuilder> newArrayList();

    if (query.hasInclude("facets")) {
      for (String facet : FACETS) {
        val tf = FacetBuilders.termsFacet(facet).field(FIELDS_MAPPING.get(KIND).get(facet)).size(MAX_FACET_TERM_COUNT);

        if (filters.fieldNames().hasNext()) {
          val facetFilters = filters.deepCopy();
          if (facetFilters.has(KIND.getId())) {
            facetFilters.with(KIND.getId()).remove(facet);
          }

          tf.facetFilter(getFilters(facetFilters));
        }
        fs.add(tf);
      }
    }
    return fs;
  }

  private FilterBuilder getFilters(ObjectNode filters) {
    if (filters.fieldNames().hasNext()) return buildFilters(filters);
    return matchAllFilter();
  }

  private FilterBuilder buildFilters(ObjectNode filters) {
    val qb = FilterBuilders.boolFilter();
    val musts = Lists.<FilterBuilder> newArrayList();

    boolean matchAll = true;
    boolean hasDonor = hasDonor(filters);
    boolean hasGene = hasGene(filters);
    boolean hasPathway = hasPathway(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasDonor || hasGene || hasPathway || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      if (hasDonor) {
        musts.add(buildDonorFilters(filters, PREFIX_MAPPING));
      }
      if (hasGene || hasPathway || hasMutation || hasConsequence || hasObservation) {
        val gb = FilterBuilders.boolFilter();
        val gMusts = buildGeneNestedFilters(filters, hasGene, hasPathway, hasMutation, hasConsequence, hasObservation);
        gb.must(gMusts.toArray(new FilterBuilder[gMusts.size()]));
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.GENE), gb));
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }
    return matchAll ? matchAllFilter() : qb;
  }

  private List<FilterBuilder> buildGeneNestedFilters(ObjectNode filters, boolean hasGene, boolean hasPathway,
      boolean hasMutation, boolean hasConsequence, boolean hasObservation) {
    val gMusts = Lists.<FilterBuilder> newArrayList();
    if (hasGene) gMusts.add(buildGeneFilters(filters, PREFIX_MAPPING));
    if (hasPathway) {
      gMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.PATHWAY), buildPathwayFilters(filters, PREFIX_MAPPING)));
    }
    if (hasMutation || hasConsequence || hasObservation) {
      val nb = FilterBuilders.boolFilter();
      val nMusts = Lists.<FilterBuilder> newArrayList();
      if (hasMutation) nMusts.add(buildMutationFilters(filters, PREFIX_MAPPING));
      if (hasConsequence) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.CONSEQUENCE),
          buildConsequenceFilters(filters, PREFIX_MAPPING)));

      if (hasObservation) {
        nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION),
            buildObservationFilters(filters, PREFIX_MAPPING)));
      }
      nb.must(nMusts.toArray(new FilterBuilder[nMusts.size()]));
      gMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.MUTATION), nb));
    }
    return gMusts;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {
    val search = buildFindAllRequest(query, CENTRIC_TYPE);
    search.setQuery(buildQuery(query));

    log.debug("{}", search);
    val response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  @Override
  public SearchResponse findAll(Query query) {
    val search = buildFindAllRequest(query, TYPE);

    log.debug("{}", search);
    val response = search.execute().actionGet();
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

    val filters = remapFilters(query.getFilters());
    search.setPostFilter(getFilters(filters));
    search.addFields(getFields(query, KIND));
    addIncludes(search, query, KIND);

    val facets = getFacets(query, filters);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    String sort = cleanSort(query.getSort());

    search.addSort(fieldSort(sort).order(query.getOrder()));
    if (!sort.equals("_score")) search.addSort("_score", SortOrder.DESC);

    return search;
  }

  private String cleanSort(String sort) {
    // Need to remove 'project.' from the sort field for some reason
    return FIELDS_MAPPING.get(KIND).get(sort).replace("project.", "");
  }

  protected FilterBuilder buildScoreFilters(Query query) {
    ObjectNode filters = remapFilters(query.getFilters());

    val qb = FilterBuilders.boolFilter();
    boolean matchAll = true;

    boolean hasGene = hasGene(filters);
    boolean hasPathway = hasPathway(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasGene || hasPathway || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      val gMusts = buildGeneNestedFilters(filters, hasGene, hasPathway, hasMutation, hasConsequence, hasObservation);
      qb.must(gMusts.toArray(new FilterBuilder[gMusts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
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
      ObjectNode filters = remapFilters(query.getFilters());
      search.setPostFilter(getFilters(filters));
      search.setQuery(buildQuery(query));
    }

    return search;
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    return nestedQuery(
        "gene",
        functionScoreQuery(
            filteredQuery(matchAllQuery(), buildScoreFilters(query)),
            ScoreFunctionBuilders.scriptFunction(SCORE))).scoreMode("total");
  }

  public ObjectNode remapFilters(ObjectNode filters) {
    return remapM2O(remapG2P(remapM2C(filters)));
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE.getId(), id);
    search.setFields(getFields(query, KIND));
    addIncludes(search, query, KIND);

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val map = createResponseMap(response, query);
    log.debug("{}", map);

    return map;
  }

  public Set<String> findIds(Query query) {
    // TODO: Now assume 5000 ids at least
    Set<String> donorIds = newHashSetWithExpectedSize(5000);
    ObjectNode filters = remapFilters(query.getFilters());

    SearchRequestBuilder search = client
        .prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(SCAN)
        .setSize(SCAN_BATCH_SIZE)
        .setScroll(KEEP_ALIVE)
        .setPostFilter(getFilters(filters))
        .setQuery(matchAllQuery())
        .setNoFields();

    SearchResponse response = search.execute().actionGet();
    while (true) {
      response = client.prepareSearchScroll(response.getScrollId())
          .setScroll(KEEP_ALIVE)
          .execute().actionGet();

      for (SearchHit hit : response.getHits()) {
        donorIds.add(hit.getId());
      }

      // Break condition: No hits are returned
      if (response.getHits().hits().length == 0) {
        response = client.prepareSearchScroll(response.getScrollId())
            .setScroll(new TimeValue(0)).execute().actionGet();
        break;
      }
    }

    return donorIds;
  }
}
