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

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static java.util.Collections.singletonMap;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.TermsLookupService.createTermsLookupFilter;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.isRepositoryDonor;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacetBuilder;
import org.icgc.dcc.portal.model.EntitySetTermFacet;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.PhenotypeResult;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Statistics;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class DonorRepository implements Repository {

  private static final Type TYPE = Type.DONOR;
  private static final Kind KIND = Kind.DONOR;

  public static final Map<String, String> DONOR_ID_SEARCH_FIELDS =
      ImmutableMap.<String, String> of("id.search", "_donor_id",
          "submittedId.search", "submittedId",
          "specimenIds.search", "specimenIds",
          "sampleIds.search", "sampleIds");

  public static final Map<String, String> FILE_DONOR_ID_SEARCH_FIELDS =
      ImmutableMap.<String, String> of(
          "tcga_participant_barcode.search", "tcga_participant_barcode",
          "tcga_sample_barcode.search", "tcga_sample_barcode",
          "tcga_aliquot_barcode.search", "tcga_aliquot_barcode",
          "submitted_specimen_id.search", "submitted_specimen_id",
          "submitted_sample_id.search", "submitted_sample_id");

  private static final class PhenotypeFacetNames {

    private static final String AGE_AT_DIAGNOSIS = "ageAtDiagnosis";
    private static final String AVERAGE_AGE_AT_DIAGNOSIS = "Average" + AGE_AT_DIAGNOSIS;
    private static final String AGE_AT_DIAGNOSIS_GROUP = "ageAtDiagnosisGroup";
    private static final String GENDER = "gender";
    private static final String VITAL_STATUS = "vitalStatus";
  }

  private static final Optional<SimpleImmutableEntry<String, String>> EMPTY_PAIR = Optional.empty();

  private static boolean wantsStatistics(Optional<SimpleImmutableEntry<String, String>> value) {
    return value.isPresent();
  }

  /**
   * A lookup table of facet name (the key) and its corresponding stats facet name pair (the value) for phenotype
   * analyses. Set the value to EMPTY_PAIR to indicate no stats is needed for the main facet. *NOTE: Categorical terms
   * such as gender or vitalStatus can't have stats facets and, if you do that, your elasticsearch query will fail.
   */
  private static final ImmutableMap<String, Optional<SimpleImmutableEntry<String, String>>> FACETS_FOR_PHENOTYPE =
      ImmutableMap.of(
          PhenotypeFacetNames.GENDER, EMPTY_PAIR,
          PhenotypeFacetNames.VITAL_STATUS, EMPTY_PAIR,
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS_GROUP, createPair(PhenotypeFacetNames.AVERAGE_AGE_AT_DIAGNOSIS,
              PhenotypeFacetNames.AGE_AT_DIAGNOSIS));

  private static final ImmutableMap<String, String> DONORS_FIELDS_MAPPING_FOR_PHENOTYPE =
      ImmutableMap.of(
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS, "donor_age_at_diagnosis",
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS_GROUP, "_summary._age_at_diagnosis_group",
          PhenotypeFacetNames.GENDER, "donor_sex",
          PhenotypeFacetNames.VITAL_STATUS, "donor_vital_status");

  @Getter(lazy = true, value = PRIVATE)
  private final Map<String, Map<String, Integer>> baselineTermsFacetsOfPhenotype = loadBaselineTermsFacetsOfPhenotype();

  private static final int SCAN_BATCH_SIZE = 1000;

  private final static TimeValue KEEP_ALIVE = new TimeValue(10000);

  private final Client client;
  private final String index;
  private final String fileIndex = "icgc-repository";
  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  @Autowired
  DonorRepository(Client client, IndexModel indexModel, QueryEngine queryEngine) {
    this.index = indexModel.getIndex();
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {
    val pql = converter.convert(query, DONOR_CENTRIC);
    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    val response = request.getRequestBuilder().execute().actionGet();
    return response;
  }

  public List<PhenotypeResult> getPhenotypeAnalysisResult(@NonNull final Collection<UUID> entitySetIds) {
    // Here we eliminate duplicates and impose ordering (needed for reading the response items).
    val setIds = ImmutableSet.copyOf(entitySetIds).asList();

    val multiResponse = performPhenotypeAnalysisMultiSearch(setIds);
    val responseItems = multiResponse.getResponses();
    val responseItemCount = responseItems.length;
    checkState(responseItemCount == setIds.size(),
        "The number of queries does not match the number of responses in a multi-search.");

    val facetKeyValuePairs = FACETS_FOR_PHENOTYPE.entrySet();

    // Building a map with facet name as the key and a list builder as the value.
    val results = facetKeyValuePairs.stream().collect(
        Collectors.toMap(entry -> entry.getKey(), notUsed -> new ImmutableList.Builder<EntitySetTermFacet>()));

    // Here we enumerate the response collection by entity-set UUIDs (the same grouping as in the multi-search).
    for (int i = 0; i < responseItemCount; i++) {
      val facets = responseItems[i].getResponse().getFacets();

      if (null == facets) continue;

      val facetMap = facets.facetsAsMap();
      val entitySetId = setIds.get(i);

      // We go through the main Results map for each facet and build the inner list by populating it with instances of
      // EntitySetTermFacet.
      for (val facetKv : facetKeyValuePairs) {
        val facetName = facetKv.getKey();
        val facet = facetMap.get(facetName);

        if (!(facet instanceof TermsFacet)) continue;

        results.get(facetName).add(
            buildEntitySetTermFacet(entitySetId, (TermsFacet) facet, facetMap, facetKv.getValue()));
      }

    }

    val finalResult = transform(results.entrySet(),
        entry -> new PhenotypeResult(entry.getKey(), entry.getValue().build()));

    return ImmutableList.copyOf(finalResult);
  }

  private EntitySetTermFacet buildEntitySetTermFacet(final UUID entitySetId, final TermsFacet termsFacet,
      final Map<String, Facet> facetMap, final Optional<SimpleImmutableEntry<String, String>> statsFacetConfigMap) {
    val termFacetList = buildTermFacetList(termsFacet, getBaselineTermsFacetsOfPhenotype());

    val mean = wantsStatistics(statsFacetConfigMap) ? getMeanFromTermsStatsFacet(
        facetMap.get(statsFacetConfigMap.get().getKey())) : null;
    val summary = new Statistics(termsFacet.getTotalCount(), termsFacet.getMissingCount(), mean);

    return new EntitySetTermFacet(entitySetId, termFacetList, summary);
  }

  private Map<String, Map<String, Integer>> loadBaselineTermsFacetsOfPhenotype() {
    val search = getPhenotypeAnalysisSearchBuilder();
    val response = search.execute().actionGet();

    log.debug("ES query is: '{}'", search);
    log.debug("ES response is: '{}'", response);

    val facets = response.getFacets();
    checkNotNull(facets, "Query response does not contain any facets.");

    val results = ImmutableMap.<String, Map<String, Integer>> builder();

    for (val facet : facets.facets()) {
      val entries = ((TermsFacet) facet).getEntries();
      val terms = transform(entries, entry -> entry.getTerm().string());

      // Map all term values to zero
      results.put(facet.getName(), toMap(terms, constant(0)));
    }

    return results.build();
  }

  private static Optional<SimpleImmutableEntry<String, String>> createPair(final String first, final String second) {
    checkArgument(isNotBlank(first));
    checkArgument(isNotBlank(second));

    return Optional.of(new SimpleImmutableEntry<String, String>(first, second));
  }

  private static List<Term> buildTermFacetList(@NonNull final TermsFacet termsFacet,
      @NonNull Map<String, Map<String, Integer>> baseline) {
    val results = ImmutableMap.<String, Integer> builder();

    // First we populate with the terms facets from the search response
    termsFacet.getEntries().stream().forEach(entry -> {
      results.put(entry.getTerm().toString(), entry.getCount());
    });

    val facetName = termsFacet.getName();
    // Then augment the result in case of missing terms in the response.
    if (baseline.containsKey(facetName)) {
      val difference = Maps.difference(results.build(), baseline.get(facetName)).entriesOnlyOnRight();

      results.putAll(difference);
    }

    val termFacetList = transform(results.build().entrySet(),
        entry -> new Term(entry.getKey(), (long) entry.getValue()));

    return ImmutableList.copyOf(termFacetList);
  }

  private static Double getMeanFromTermsStatsFacet(final Facet facet) {
    if (!(facet instanceof TermsStatsFacet)) {
      return null;
    }

    val stats = ((TermsStatsFacet) facet).getEntries();

    return (stats.size() > 0) ? stats.get(0).getMean() : 0;
  }

  private MultiSearchResponse performPhenotypeAnalysisMultiSearch(@NonNull final List<UUID> setIds) {
    val multiSearch = client.prepareMultiSearch();
    val matchAll = matchAllQuery();

    for (val setId : setIds) {
      val search = getPhenotypeAnalysisSearchBuilder();
      search.setQuery(filteredQuery(matchAll, getDonorSetIdFilterBuilder(setId)));

      // Adding terms_stats facets
      FACETS_FOR_PHENOTYPE.values().stream()
          .filter(v -> wantsStatistics(v))
          .map(Optional::get)
          .forEach(statsFacetNameFieldPair -> {
            final String actualFieldName = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE.get(statsFacetNameFieldPair.getValue());

            search.addFacet(buildTermsStatsFacetBuilder(statsFacetNameFieldPair.getKey(), actualFieldName));
          });

      log.info("Sub-search for DonorSet ID [{}] is: '{}'", setId, search);
      multiSearch.add(search);
    }

    val multiResponse = multiSearch.execute().actionGet();
    log.info("MultiResponse is: '{}'.", multiResponse);

    return multiResponse;
  }

  private static TermsStatsFacetBuilder buildTermsStatsFacetBuilder(final String facetName, final String facetField) {
    return FacetBuilders.termsStatsFacet(facetName)
        .keyField("_type")
        .valueField(facetField)
        .size(MAX_FACET_TERM_COUNT);
  }

  private SearchRequestBuilder getPhenotypeAnalysisSearchBuilder() {
    val type = Type.DONOR_CENTRIC;
    val fieldMap = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE;

    val searchBuilder = client.prepareSearch(index)
        .setTypes(type.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .addFields(fieldMap.values().stream().toArray(String[]::new));

    for (val name : FACETS_FOR_PHENOTYPE.keySet()) {
      val facetbuilder = FacetBuilders.termsFacet(name)
          .field(fieldMap.get(name))
          .size(MAX_FACET_TERM_COUNT);

      searchBuilder.addFacet(facetbuilder);
    }

    return searchBuilder;
  }

  private static FilterBuilder getDonorSetIdFilterBuilder(final UUID donorId) {
    if (null == donorId) {
      return matchAllFilter();
    }

    val mustFilterFieldName = "_id";
    // Note: We should not reference TermsLookupService here but for now we'll wait for the PQL module and see how we
    // can move the createTermsLookupFilter() routine out of TermsLookupService.
    val termsLookupFilter = createTermsLookupFilter(mustFilterFieldName, TermLookupType.DONOR_IDS, donorId);

    return boolFilter().must(termsLookupFilter);
  }

  @Override
  public SearchResponse findAll(Query query) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public long count(Query query) {
    log.info("Converting {}", query.getFilters());

    val pql = converter.convertCount(query, DONOR_CENTRIC);

    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    return request.getRequestBuilder().setSearchType(COUNT).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      log.info("Converting {}", query.getFilters());
      val pql = converter.convertCount(query, DONOR_CENTRIC);
      val request = queryEngine.execute(pql, DONOR_CENTRIC);
      search.add(request.getRequestBuilder());
    }
    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        log.info("Nested converting {}", innerQuery);

        val pql = converter.convertCount(innerQuery, DONOR_CENTRIC);
        val request = queryEngine.execute(pql, DONOR_CENTRIC);

        search.add(request.getRequestBuilder());
      }
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE.getId(), id)
        .setFields(getFields(query, KIND));
    setFetchSourceOfGetRequest(search, query, KIND);

    val response = search.execute().actionGet();

    if (response.isExists()) {
      val result = createResponseMap(response, query, KIND);
      log.debug("Found donor: '{}'.", result);

      return result;
    }

    if (!isRepositoryDonor(client, "donor_id", id)) {
      // We know this is guaranteed to throw a 404, since the 'id' was not found in the first query.
      checkResponseState(id, response, KIND);
    }

    return singletonMap(FIELDS_MAPPING.get(KIND).get("id"), id);
  }

  /**
   * Retrieves donor-specimen-sample information. Note this searches donor and not donor-centric as centric does not
   * have sample information
   */
  public SearchResponse getDonorSamplesByProject(String projectId) {

    // Only download donor with complete (has submitted molecular data)
    val donorFilters = FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter("_project_id", projectId))
        .must(FilterBuilders.termFilter("_summary._complete", true));

    val search = client.prepareSearch(index)
        .setTypes(TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(6000)
        .setFetchSource("specimen", null)
        .addFields("_donor_id", "donor_id", "project._project_id")
        .setPostFilter(donorFilters);
    return search.execute().actionGet();
  }

  public Set<String> findIds(Query query) {

    // TODO: Now assume 5000 ids at least
    Set<String> donorIds = newHashSetWithExpectedSize(5000);

    val pql = converter.convert(query, DONOR_CENTRIC);
    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    val requestBuilder = request.getRequestBuilder()
        .setSearchType(SCAN)
        .setSize(SCAN_BATCH_SIZE)
        .setScroll(KEEP_ALIVE)
        .setNoFields();

    SearchResponse response = requestBuilder.execute().actionGet();
    while (true) {
      response = client.prepareSearchScroll(response.getScrollId())
          .setScroll(KEEP_ALIVE)
          .execute().actionGet();

      for (SearchHit hit : response.getHits()) {
        donorIds.add(hit.getId());
      }

      val finished = !hasHits(response);
      if (finished) {
        break;
      }
    }

    return donorIds;
  }

  public SearchResponse validateIdentifiersDonorText(List<String> input) {
    val boolQuery = QueryBuilders.boolQuery();

    val search = client.prepareSearch(index)
        .setTypes("donor-text")
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(5000);

    for (val searchField : DONOR_ID_SEARCH_FIELDS.keySet()) {
      boolQuery.should(QueryBuilders.termsQuery(searchField, input.toArray()));
      search.addHighlightedField(searchField);
      search.addField(searchField);
    }
    search.setQuery(boolQuery);
    log.info("Search is {}", search);

    val response = search.execute().actionGet();
    return response;
  }

  public SearchResponse validateIdentifiersFileDoner(List<String> input) {
    val boolQuery = QueryBuilders.boolQuery();

    val search = client.prepareSearch(fileIndex)
        .setTypes("file-donor-text")
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(5000);

    for (val searchField : FILE_DONOR_ID_SEARCH_FIELDS.keySet()) {
      boolQuery.should(QueryBuilders.termsQuery(searchField, input.toArray()));
      search.addHighlightedField(searchField);
      search.addField(searchField);
    }
    search.setQuery(boolQuery);
    log.info("Search is {}", search);

    val response = search.execute().actionGet();
    return response;
  }

}
