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
package org.icgc.dcc.portal.analysis;

import static org.elasticsearch.index.query.FilterBuilders.boolFilter;

import java.util.ArrayList;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsLookupFilterBuilder;
import org.icgc.dcc.portal.model.BaseEntityList;
import org.icgc.dcc.portal.model.DerivedEntityListDefinition;
import org.icgc.dcc.portal.model.EntityListDefinition;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.model.UnionUnitWithCount;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.icgc.dcc.portal.repository.Repository;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * TODO
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class UnionAnalyzer {

  @NonNull
  private final Client client;

  @NonNull
  private final IndexModel index;

  @NonNull
  private final UnionAnalysisRepository unionAnalysisDao;

  @NonNull
  private final EntityListRepository entityListDao;

  @NonNull
  private final TermsLookupService termLookupService;

  @NonNull
  private final GeneRepository geneRepository;

  @NonNull
  private final DonorRepository donorRepository;

  @NonNull
  private final MutationRepository mutationRepository;

  private final static String FIELD_NAME = "_id";
  private final static MatchAllQueryBuilder MATCH_ALL = QueryBuilders.matchAllQuery();

  private final static int MAX_LIST_ELEMENT_COUNT = 5000;

  private static TermsLookupFilterBuilder buildTermsFilter(
      final TermLookupType type,
      final UUID id) {

    return TermsLookupService.createTermsLookupFilter(FIELD_NAME, type, id);
  }

  private static String getIndexTypeNameFrom(final BaseEntityList.Type type) {

    return type.getName() + "-centric";
  }

  private static TermLookupType getLookupTypeFrom(final BaseEntityList.Type type) {

    TermLookupType result = TermLookupType.GENE_IDS;

    switch (type) {

    case DONOR: {
      result = TermLookupType.DONOR_IDS;
      break;
    }
    case MUTATION: {
      result = TermLookupType.MUTATION_IDS;
      break;
    }
    }
    return result;
  }

  private static BoolFilterBuilder toBoolFilterFrom(
      final UnionUnit unionDefinition,
      final BaseEntityList.Type entityType) {

    val lookupType = getLookupTypeFrom(entityType);
    val boolFilter = boolFilter();

    // adding Musts
    val intersectionUnits = unionDefinition.getIntersection();
    for (val mustId : intersectionUnits) {

      boolFilter.must(buildTermsFilter(lookupType, mustId));
    }

    // adding MustNots
    val exclusionUnits = unionDefinition.getExclusions();
    for (val notId : exclusionUnits) {

      boolFilter.mustNot(buildTermsFilter(lookupType, notId));
    }
    return boolFilter;
  }

  private static BoolFilterBuilder toBoolFilterFrom(
      final Iterable<UnionUnit> definitions,
      final BaseEntityList.Type entityType) {

    val boolFilter = boolFilter();

    for (val def : definitions) {

      boolFilter.should(toBoolFilterFrom(def, entityType));
    }
    return boolFilter;
  }

  private static long getCountFrom(
      @NonNull final SearchResponse response) {

    return response.getHits().totalHits();
  }

  private String getIndexName() {

    return this.index.getIndex();
  }

  @Async
  public void determineUnionUnitCounts(
      @NonNull final UUID id,
      @NonNull final UnionAnalysisRequest request) {

    val analysis = unionAnalysisDao.find(id);

    // set status to 'in progress' for browser polling
    unionAnalysisDao.update(analysis.inProgress());

    val entityType = request.getType();
    val definitions = request.toUnionSets();

    val result = new ArrayList<UnionUnitWithCount>(definitions.size());
    for (val def : definitions) {

      val count = getUnionCount(def, entityType);
      result.add(UnionUnitWithCount.copyOf(def, count));
    }
    log.info("Result of Union Analysis is: " + result);

    // done - update status to finished
    unionAnalysisDao.update(analysis.finished(result));
  }

  private long getUnionCount(
      final UnionUnit unionDefinition,
      final BaseEntityList.Type entityType) {

    val response = runEsQuery(
        getIndexTypeNameFrom(entityType),
        SearchType.COUNT,
        toBoolFilterFrom(unionDefinition, entityType));

    val count = getCountFrom(response);
    log.info("Total hits: {}", count);

    return count;
  }

  @Async
  public void combineLists(
      @NonNull final UUID newListId,
      @NonNull final DerivedEntityListDefinition listDefinition) {

    val newList = entityListDao.find(newListId);

    // set status to 'in progress' for browser polling
    entityListDao.update(newList.inProgress());

    val definitions = listDefinition.getUnion();
    val entityType = listDefinition.getType();

    val response = unionAll(definitions, entityType);

    val entityIds = SearchResponses.getHitIds(response);
    log.info("Union result is: '{}'", entityIds);

    termLookupService.createTermsLookup(getLookupTypeFrom(entityType), newList.getId(), entityIds);

    val count = getCountFrom(response);
    // done - update status to finished
    entityListDao.update(newList.finished(count));
  }

  private SearchResponse unionAll(
      final Iterable<UnionUnit> definitions,
      final BaseEntityList.Type entityType) {

    val response = runEsQuery(
        getIndexTypeNameFrom(entityType),
        SearchType.QUERY_THEN_FETCH,
        toBoolFilterFrom(definitions, entityType));

    return response;
  }

  private Repository getRepositoryByEntityType(final BaseEntityList.Type entityType) {

    Repository repo = geneRepository;

    switch (entityType) {
    case DONOR: {
      repo = donorRepository;
      break;
    }
    case MUTATION: {
      repo = mutationRepository;
      break;
    }
    }
    return repo;
  }

  private SearchResponse executeFilterQuery(
      @NonNull final EntityListDefinition definition) {

    val maxCount = 1000;

    log.info("List def is: " + definition);

    val limitedGeneQuery = Query.builder()
        // .fields(idField())
        .filters(definition.getFilters())
        .sort(definition.getSortBy())
        .order(definition.getSortOrder().getName())

        .size(maxCount)
        .limit(maxCount)
        .build();

    val repo = getRepositoryByEntityType(definition.getType());
    return repo.findAllCentric(limitedGeneQuery);
  }

  @Async
  public void createList(
      @NonNull final UUID newListId,
      @NonNull final EntityListDefinition listDefinition) {

    val newList = entityListDao.find(newListId);

    // set status to 'in progress' for browser polling
    entityListDao.update(newList.inProgress());

    val response = executeFilterQuery(listDefinition);

    val entityIds = SearchResponses.getHitIds(response);
    log.info("Union result is: '{}'", entityIds);

    val entityType = listDefinition.getType();
    termLookupService.createTermsLookup(getLookupTypeFrom(entityType), newList.getId(), entityIds);

    val count = getCountFrom(response);
    // done - update status to finished
    entityListDao.update(newList.finished(count));
  }

  private SearchResponse runEsQuery(
      final String indexTypeName,
      final SearchType searchType,
      final BoolFilterBuilder boolFilter) {

    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);

    val search = client
        .prepareSearch(getIndexName())
        .setTypes(indexTypeName)
        .setSearchType(searchType)
        .setQuery(query)
        .setSize(MAX_LIST_ELEMENT_COUNT)
        .setNoFields();

    log.info("ElasticSearch query is: '{}'", search);

    val response = search.execute().actionGet();

    log.info("ElasticSearch result is: '{}'", response);

    return response;
  }
}
