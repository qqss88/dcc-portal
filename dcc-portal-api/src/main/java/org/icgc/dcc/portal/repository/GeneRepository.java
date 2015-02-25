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
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.search.facet.FacetBuilders.termsFacet;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.icgc.dcc.common.core.model.FieldNames.GENE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.GENE_SYMBOL;
import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.service.QueryService.buildConsequenceFilters;
import static org.icgc.dcc.portal.service.QueryService.buildDonorFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneSetFilters;
import static org.icgc.dcc.portal.service.QueryService.buildMutationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildObservationFilters;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasGeneSet;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.remapG2P;
import static org.icgc.dcc.portal.service.QueryService.remapM2C;
import static org.icgc.dcc.portal.service.QueryService.remapM2O;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.addIncludes;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.Filters.andFilter;
import static org.icgc.dcc.portal.util.Filters.geneSetFilter;
import static org.icgc.dcc.portal.util.Filters.inputGeneListFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.AndQuery;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class GeneRepository implements Repository {

  private static final String SCORE = "x = doc['donor._summary._ssm_count']; x.empty || x.value < 1 ? 0 : 1";

  public static final List<String> GENE_ID_SEARCH_FIELDS = ImmutableList.<String> of(
      GENE_ID, GENE_SYMBOL, GENE_UNIPROT_IDS);

  private static final Type CENTRIC_TYPE = Type.GENE_CENTRIC;
  private static final Type TYPE = Type.GENE;
  private static final Kind KIND = Kind.GENE;

  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap.of(
      Kind.DONOR, "donor",
      Kind.MUTATION, "donor.ssm",
      Kind.CONSEQUENCE, "donor.ssm.consequence",
      Kind.OBSERVATION, "donor.ssm.observation"));
  protected static final ImmutableMap<Kind, String> PREFIX_MAPPING = NESTED_MAPPING;

  private static final ImmutableList<String> FACETS = ImmutableList.of("type");

  private final Client client;
  private final String index;

  @Autowired
  GeneRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  private List<TermsFacetBuilder> getFacets(Query query, ObjectNode filters) {
    val fs = Lists.<TermsFacetBuilder> newArrayList();

    if (query.hasInclude("facets")) {
      for (String facet : FACETS) {
        val tf =
            FacetBuilders.termsFacet(facet).field(FIELDS_MAPPING.get(KIND).get(facet))
                .size(MAX_FACET_TERM_COUNT);

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
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);
    boolean hasGeneSet = hasGeneSet(filters);

    if (hasGene || hasGeneSet || hasDonor || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      if (hasGene) {
        musts.add(buildGeneFilters(filters, PREFIX_MAPPING));
      }
      if (hasGeneSet) {
        musts.add(buildGeneSetFilters(filters, PREFIX_MAPPING));
      }

      if (hasDonor || hasMutation || hasConsequence || hasObservation) {
        val db = FilterBuilders.boolFilter();
        val dMusts = buildDonorNestedFilters(filters, hasDonor, hasMutation, hasConsequence, hasObservation);
        db.must(dMusts.toArray(new FilterBuilder[dMusts.size()]));
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.DONOR), db));
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
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

  private Map<String, String> findGeneSymbolsByFilters(@NonNull ObjectNode filters) {
    val maxGenes = 70000;
    val symbolFieldName = "symbol";

    val search = client.prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(maxGenes)
        .setPostFilter(getFilters(filters))
        .addField(symbolFieldName);

    val response = search.execute().actionGet();

    val map = Maps.<String, String> newLinkedHashMap();
    for (val hit : response.getHits()) {
      String id = hit.getId();
      String symbol = hit.getFields().get(symbolFieldName).getValue();

      map.put(id, symbol);
    }

    return map;
  }

  public Map<String, String> findGeneSymbolsByGeneListIdAndGeneSetId(@NonNull UUID inputGeneListId,
      @NonNull String geneSetId) {
    val filters = remapFilters(andFilter(geneSetFilter(geneSetId), inputGeneListFilter(inputGeneListId)));
    return findGeneSymbolsByFilters(filters);
  }

  public Map<String, String> findGeneSymbolsByGeneListId(@NonNull UUID inputGeneListId) {
    val filters = remapFilters(inputGeneListFilter(inputGeneListId));
    return findGeneSymbolsByFilters(filters);
  }

  public SearchResponse findGeneSetCounts(AndQuery query) {
    val search = client.prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(COUNT);

    val boolFilter = new BoolFilterBuilder();
    for (val filters : query.getAndFilters()) {
      val remappedFilters = remapFilters(filters);

      boolFilter.must(getFilters(remappedFilters));
    }

    for (val universe : Universe.values()) {
      val universeFacetName = universe.getGeneSetFacetName();

      search.addFacet(
          termsFacet(universeFacetName)
              .field(universeFacetName)
              .size(50000) // This has to be as big as the largest universe
              .facetFilter(boolFilter));
    }

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

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

    val filters = remapFilters(query.getFilters());
    search.setPostFilter(getFilters(filters));
    search.addFields(getFields(query, KIND));
    addIncludes(search, query, KIND);

    val facets = getFacets(query, filters);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    if (query.getSort() != null) {
      String sort = FIELDS_MAPPING.get(KIND).get(query.getSort());
      search.addSort(fieldSort(sort).order(query.getOrder()));
      if (!sort.equals("_score")) search.addSort("_score", SortOrder.DESC);
    }

    return search;
  }

  public FilterBuilder buildScoreFilters(ObjectNode filters) {
    ObjectNode remappedFilters = remapFilters(filters);

    val qb = FilterBuilders.boolFilter();
    boolean matchAll = true;

    boolean hasDonor = hasDonor(remappedFilters);
    boolean hasMutation = hasMutation(remappedFilters);
    boolean hasConsequence = hasConsequence(remappedFilters);
    boolean hasObservation = hasObservation(remappedFilters);

    if (hasDonor || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      val dMusts = buildDonorNestedFilters(remappedFilters, hasDonor, hasMutation, hasConsequence, hasObservation);
      qb.must(dMusts.toArray(new FilterBuilder[dMusts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
  }

  private List<FilterBuilder> buildDonorNestedFilters(ObjectNode filters,
      boolean hasDonor, boolean hasMutation, boolean hasConsequence, boolean hasObservation) {
    val dMusts = Lists.<FilterBuilder> newArrayList();
    if (hasDonor) dMusts.add(buildDonorFilters(filters, PREFIX_MAPPING));
    if (hasMutation || hasConsequence || hasObservation) {
      val nb = FilterBuilders.boolFilter();
      val nMusts = Lists.<FilterBuilder> newArrayList();
      if (hasMutation) nMusts.add(buildMutationFilters(filters, PREFIX_MAPPING));
      if (hasConsequence) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.CONSEQUENCE),
          buildConsequenceFilters(filters, PREFIX_MAPPING)));

      if (hasObservation) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION),
          buildObservationFilters(filters, PREFIX_MAPPING)));

      nb.must(nMusts.toArray(new FilterBuilder[nMusts.size()]));
      dMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.MUTATION), nb));
    }
    return dMusts;
  }

  @Override
  public long count(Query query) {
    val search = buildCountRequest(query, CENTRIC_TYPE);

    log.debug("{}", search);

    return search.execute().actionGet().getHits().getTotalHits();
  }

  public long countIntersection(AndQuery query) {
    val search = client.prepareSearch(index).setTypes(CENTRIC_TYPE.getId()).setSearchType(COUNT);

    if (query.hasFilters()) {
      // Require all filter components to be true
      val boolFilter = new BoolFilterBuilder();
      for (val filters : query.getAndFilters()) {
        val remappedFilters = remapFilters(filters);

        boolFilter.must(getFilters(remappedFilters));
      }

      search.setPostFilter(boolFilter);
    }

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
      val filters = remapFilters(query.getFilters());
      search.setPostFilter(getFilters(filters));

      // Remove score from below to significantly speed up results
      val countQuery = nestedQuery(
          "donor",
          filteredQuery(matchAllQuery(), buildScoreFilters(query.getFilters())));

      search.setQuery(countQuery);
    }

    return search;
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    return nestedQuery(
        "donor",
        functionScoreQuery(filteredQuery(matchAllQuery(), buildScoreFilters(query.getFilters())),
            ScoreFunctionBuilders.scriptFunction(SCORE))).scoreMode("total");
  }

  public ObjectNode remapFilters(ObjectNode filters) {
    return remapG2P(remapM2O(remapM2C(filters)));
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, CENTRIC_TYPE.getId(), id);
    search.setFields(getFields(query, KIND));
    addIncludes(search, query, KIND);

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val map = createResponseMap(response, query);
    log.debug("{}", map);

    return map;
  }

  /*
   * Lookup up genes by ensembl gene_id or gene symbol or uniprot
   * 
   * @param input a list of string identifiers of either ensembl id or gene symbol or uniprot
   * 
   * @returns a map of matched identifiers
   */
  // public Map<String, Multimap<String, String>> validateIdentifiers(List<String> input) {
  public SearchResponse validateIdentifiers(List<String> input) {

    val boolQuery = QueryBuilders.boolQuery();

    val search = client.prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(5000);
    for (val searchField : GENE_ID_SEARCH_FIELDS) {
      boolQuery.should(QueryBuilders.termsQuery(searchField, input.toArray()));
      search.addHighlightedField(searchField);
      search.addField(searchField);
    }
    search.setQuery(boolQuery);

    log.debug("Search is {}", search);
    val response = search.execute().actionGet();

    return response;
  }

  /**
   * Find transcripts for a specific gene that have mutations
   * @param geneId
   * @return unique list of transcript ids
   */
  public List<String> getAffectedTranscripts(String geneId) {
    val transcriptField = "donor.ssm.consequence.transcript_affected";
    val result = Lists.<String> newArrayList();
    val search =
        client
            .prepareSearch(index)
            .setTypes(CENTRIC_TYPE.getId())
            .setSearchType(QUERY_THEN_FETCH)
            .setSize(0)
            .addFacet(
                FacetBuilders.termsFacet("affectedTranscript")
                    .nested("donor.ssm.consequence")
                    .size(IndexModel.MAX_FACET_TERM_COUNT)
                    .field(transcriptField)
                    .facetFilter(FilterBuilders.termFilter("donor.ssm.consequence._gene_id", geneId)));

    val response = search.execute().actionGet();
    val facet = (InternalStringTermsFacet) response.getFacets().facet("affectedTranscript");

    for (val entry : facet.getEntries()) {
      result.add(entry.getTerm().toString());
    }
    return result;
  }
}
