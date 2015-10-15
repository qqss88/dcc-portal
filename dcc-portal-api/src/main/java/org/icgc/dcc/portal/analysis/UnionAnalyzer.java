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

import static java.lang.Math.min;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.icgc.dcc.portal.model.IndexModel.REPOSITORY_INDEX_NAME;
import static org.icgc.dcc.portal.model.IndexModel.Type.DONOR_TEXT;
import static org.icgc.dcc.portal.model.IndexModel.Type.REPOSITORY_FILE_DONOR_TEXT;
import static org.icgc.dcc.portal.service.TermsLookupService.TERMS_LOOKUP_PATH;
import static org.icgc.dcc.portal.util.JsonUtils.LIST_TYPE_REFERENCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;

import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsLookupFilterBuilder;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.BaseEntitySet;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySetDefinition;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.model.UnionUnitWithCount;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides various set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class UnionAnalyzer {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Dependencies.
   */
  @NonNull
  private final Client client;
  @Value("#{indexName}")
  private final String indexName;
  @NonNull
  private final PortalProperties properties;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  @NonNull
  private final UnionAnalysisRepository unionAnalysisRepository;
  @NonNull
  private final EntityListRepository entityListRepository;
  @NonNull
  private final TermsLookupService termLookupService;
  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final RepositoryFileRepository repositoryFileRepository;

  /**
   * Configuration.
   */
  @Min(1)
  private int maxNumberOfHits;
  @Min(1)
  private int maxMultiplier;
  @Min(1)
  private int maxUnionCount;
  @Min(1)
  private int maxPreviewNumberOfHits;

  @PostConstruct
  private void init() {
    val setOpSettings = properties.getSetOperation();
    maxNumberOfHits = setOpSettings.getMaxNumberOfHits();
    maxMultiplier = setOpSettings.getMaxMultiplier();

    maxUnionCount = maxNumberOfHits * maxMultiplier;

    maxPreviewNumberOfHits = min(setOpSettings.getMaxPreviewNumberOfHits(), maxUnionCount);
  }

  private final static String FIELD_NAME = "_id";
  private final static MatchAllQueryBuilder MATCH_ALL = QueryBuilders.matchAllQuery();

  private static TermsLookupFilterBuilder buildTermsFilter(final TermLookupType type, final UUID id) {
    return TermsLookupService.createTermsLookupFilter(FIELD_NAME, type, id);
  }

  private static BoolFilterBuilder toBoolFilterFrom(final UnionUnit unionDefinition,
      final BaseEntitySet.Type entityType) {
    val lookupType = entityType.toLookupType();
    val boolFilter = boolFilter();

    // Adding Musts
    val intersectionUnits = unionDefinition.getIntersection();
    for (val mustId : intersectionUnits) {
      boolFilter.must(buildTermsFilter(lookupType, mustId));
    }

    // Adding MustNots
    val exclusionUnits = unionDefinition.getExclusions();
    for (val notId : exclusionUnits) {
      boolFilter.mustNot(buildTermsFilter(lookupType, notId));
    }
    return boolFilter;
  }

  private static BoolFilterBuilder toBoolFilterFrom(final Iterable<UnionUnit> definitions,
      final BaseEntitySet.Type entityType) {
    val boolFilter = boolFilter();

    for (val def : definitions) {
      boolFilter.should(toBoolFilterFrom(def, entityType));
    }
    return boolFilter;
  }

  private long getCountFrom(@NonNull final SearchResponse response, final long max) {
    val result = SearchResponses.getTotalHitCount(response);

    return min(max, result);
  }

  @Async
  public void calculateUnionUnitCounts(@NonNull final UUID id, @NonNull final UnionAnalysisRequest request) {
    UnionAnalysisResult analysis = null;

    try {
      analysis = unionAnalysisRepository.find(id);
      val dataVersion = analysis.getVersion();

      // Set status to 'in progress' for browser polling
      unionAnalysisRepository.update(analysis.updateStateToInProgress(), dataVersion);

      val entityType = request.getType();
      val definitions = request.toUnionSets();

      val result = new ArrayList<UnionUnitWithCount>(definitions.size());

      if (entityType == BaseEntitySet.Type.DONOR) {
        for (val def : definitions) {
          val count = getDonorCount(def);
          result.add(UnionUnitWithCount.copyOf(def, count));
        }
      } else {
        for (val def : definitions) {
          val count = getUnionCount(def, entityType);
          result.add(UnionUnitWithCount.copyOf(def, count));
        }
      }

      log.debug("Result of Union Analysis is: '{}'", result);

      // Done - update status to finished
      unionAnalysisRepository.update(analysis.updateStateToFinished(result), dataVersion);
    } catch (Exception e) {
      log.error("Error while calculating UnionUnitCounts for {}: {}", id, e);

      if (null != analysis) {
        unionAnalysisRepository.update(analysis.updateStateToError(), analysis.getVersion());
      }
    }
  }

  private long getUnionCount(
      final UnionUnit unionDefinition,
      final BaseEntitySet.Type entityType) {

    val response = runEsQuery(
        entityType.getIndexTypeName(),
        SearchType.COUNT,
        toBoolFilterFrom(unionDefinition, entityType),
        maxUnionCount);

    val count = getCountFrom(response, maxUnionCount);
    log.debug("Total hits: {}", count);

    return count;
  }

  public List<String> previewSetUnion(@NonNull final DerivedEntitySetDefinition definition) {
    val definitions = definition.getUnion();
    val entityType = definition.getType();

    val response = unionAll(definitions, entityType, maxPreviewNumberOfHits);

    return SearchResponses.getHitIds(response);
  }

  @Async
  public void combineListsAsync(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    combineLists(newEntityId, entitySetDefinition);
  }

  public void combineLists(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    EntitySet newEntity = null;

    try {
      newEntity = entityListRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();

      // Set status to 'in progress' for browser polling
      entityListRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val definitions = entitySetDefinition.getUnion();
      val entityType = entitySetDefinition.getType();

      SearchResponse response;
      long totalHits;
      Iterable<String> entityIds;
      if (entityType == BaseEntitySet.Type.DONOR) {
        response = getDonorUnion(definitions);
        entityIds = SearchResponses.getHitIdsSet(response);
        totalHits = Iterables.size(entityIds);
      } else {
        response = unionAll(definitions, entityType, maxUnionCount);
        totalHits = SearchResponses.getTotalHitCount(response);
        entityIds = SearchResponses.getHitIds(response);
      }
      log.debug("Union result is: '{}'", entityIds);

      if (totalHits > maxUnionCount) {
        // If the total hit count exceeds the allowed maximum, flag this list and quit.
        log.info(
            "Because the total hit count ({}) exceeds the allowed maximum ({}), this set operation is aborted.",
            totalHits, maxUnionCount);

        entityListRepository.update(newEntity.updateStateToError(), dataVersion);
        return;
      }

      val lookupType = entityType.toLookupType();
      termLookupService.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());

      // Done - update status to finished
      entityListRepository.update(newEntity.updateStateToFinished(totalHits), dataVersion);
    } catch (Exception e) {
      log.error("Error while combining lists for {}. See exception below.", newEntityId);
      log.error("Error while combining lists: '{}'", e);

      if (null != newEntity) {
        entityListRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  private SearchResponse unionAll(final Iterable<UnionUnit> definitions, final BaseEntitySet.Type entityType,
      final int max) {

    val response = runEsQuery(
        entityType.getIndexTypeName(),
        SearchType.QUERY_THEN_FETCH,
        toBoolFilterFrom(definitions, entityType),
        max);

    return response;
  }

  private Type getRepositoryByEntityType(final BaseEntitySet.Type entityType) {
    if (entityType == BaseEntitySet.Type.DONOR) {
      return Type.DONOR_CENTRIC;
    } else if (entityType == BaseEntitySet.Type.GENE) {
      return Type.GENE_CENTRIC;
    } else if (entityType == BaseEntitySet.Type.MUTATION) {
      return Type.MUTATION_CENTRIC;
    }

    log.error("No mapping for enum value '{}' of BaseEntityList.Type.", entityType);
    throw new IllegalStateException("No mapping for enum value: " + entityType);
  }

  private SearchResponse executeFilterQuery(@NonNull final EntitySetDefinition definition, final int max) {

    log.debug("List def is: " + definition);

    val query = Query.builder()
        .fields(ImmutableList.of("id"))
        .filters(definition.getFilters())
        .sort(definition.getSortBy())
        .order(definition.getSortOrder().getName())
        // .size(max)
        // .limit(max)
        .build();

    val type = getRepositoryByEntityType(definition.getType());
    val pql = converter.convert(query, type);
    val request = queryEngine.execute(pql, type);
    return request.getRequestBuilder()
        .setSize(max)
        .execute().actionGet();
  }

  @Async
  public void materializeListAsync(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySetDefinition) {
    materializeList(newEntityId, entitySetDefinition);
  }

  public void materializeList(@NonNull final UUID newEntityId, @NonNull final EntitySetDefinition entitySetDefinition) {
    EntitySet newEntity = null;

    try {
      newEntity = entityListRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();

      // Set status to 'in progress' for browser polling
      entityListRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val max = entitySetDefinition.getLimit(maxNumberOfHits);
      val response = executeFilterQuery(entitySetDefinition, max);

      val entityIds = SearchResponses.getHitIds(response);
      log.debug("The result of running a FilterParam query is: '{}'", entityIds);

      val lookupType = entitySetDefinition.getType().toLookupType();
      termLookupService.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());

      val count = getCountFrom(response, max);
      // Done - update status to finished
      entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
    } catch (Exception e) {
      log.error("Error while materializing list for {}: {}", newEntityId, e);

      if (null != newEntity) {
        entityListRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  @SneakyThrows
  public void materializeRepositoryList(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entityListRepository.find(newEntityId);
    val dataVersion = newEntity.getVersion();

    val query = Query.builder()
        .filters(entitySet.getFilters())
        .fields(Arrays.asList("donorId"))
        .sort("id")
        .order("desc")
        .size(maxNumberOfHits)
        .defaultLimit(maxNumberOfHits)
        .build();
    val maxSetSize = entitySet.getLimit(maxNumberOfHits);
    val entityIds = repositoryFileRepository.findAllDonorIds(query, maxSetSize);

    val lookupType = entitySet.getType().toLookupType();
    termLookupService.createTermsLookup(lookupType, newEntityId, entityIds, entitySet.isTransient());

    val count = entityIds.size();
    // Done - update status to finished
    entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
  }

  public void materializeFileSet(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entityListRepository.find(newEntityId);
    val dataVersion = newEntity.getVersion();

    val query = Query.builder()
        .filters(entitySet.getFilters())
        .sort("id")
        .order("desc")
        .size(maxNumberOfHits)
        .defaultLimit(maxNumberOfHits)
        .build();

    val entityIds = repositoryFileRepository.findAllFileIds(query);
    val lookupType = entitySet.getType().toLookupType();

    val repoList = (ArrayNode) entitySet.getFilters().path("file").path("repoName").path("is");
    termLookupService.createTermsLookup(lookupType, newEntityId, entityIds, repoList.get(0).asText());

    val count = entityIds.size();
    // Done - update status to finished
    entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
  }

  private SearchResponse runEsQuery(
      final String indexTypeName,
      @NonNull final SearchType searchType,
      @NonNull final BoolFilterBuilder boolFilter,
      final int max) {

    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);

    val search = client
        .prepareSearch(indexName)
        .setTypes(indexTypeName)
        .setSearchType(searchType)
        .setQuery(query)
        .setSize(max)
        .setNoFields();

    log.debug("ElasticSearch query is: '{}'", search);

    // val watch = Stopwatch.createStarted();

    val response = search.execute().actionGet();

    // watch.stop();
    // log.info("runEsQuery took {} nanoseconds for searchType - '{}'", watch.elapsed(TimeUnit.NANOSECONDS),
    // searchType);

    log.debug("ElasticSearch result is: '{}'", response);

    return response;
  }

  public List<String> retriveListItems(@NonNull final EntitySet entityList) {
    val lookupTypeName = entityList.getType().toLookupType().getName();
    val query = client.prepareGet(TermsLookupService.TERMS_LOOKUP_INDEX_NAME,
        lookupTypeName, entityList.getId().toString());

    val response = query.execute().actionGet();
    val rawValues = response.getSource().get(TERMS_LOOKUP_PATH);
    log.debug("Raw values of {} are: '{}'", lookupTypeName, rawValues);

    return MAPPER.convertValue(rawValues, LIST_TYPE_REFERENCE);
  }

  public Map<String, String> retrieveGeneIdsAndSymbolsByListId(final UUID listId) {

    return geneRepository.findGeneSymbolsByGeneListId(listId);
  }

  private static BoolFilterBuilder toDonorBoolFilter(final UnionUnit unionDefinition) {

    val boolFilter = boolFilter();

    // Adding Musts
    val intersectionUnits = unionDefinition.getIntersection();
    for (val mustId : intersectionUnits) {
      val mustTerms =
          TermsLookupService.createTermsLookupFilter("_id", TermsLookupService.TermLookupType.DONOR_IDS, mustId);
      boolFilter.must(mustTerms);
    }

    // Adding MustNots
    val exclusionUnits = unionDefinition.getExclusions();
    for (val notId : exclusionUnits) {
      val mustNotTerms =
          TermsLookupService.createTermsLookupFilter("_id", TermsLookupService.TermLookupType.DONOR_IDS, notId);
      boolFilter.mustNot(mustNotTerms);
    }
    return boolFilter;
  }

  private SearchResponse getDonorUnion(final Iterable<UnionUnit> definitions) {
    val boolFilter = toBoolFilterFrom(definitions, BaseEntitySet.Type.DONOR);
    val response = donorSearchRequest(boolFilter);

    return response;
  }

  private long getDonorCount(final UnionUnit unionDefinition) {
    val boolFilter = toDonorBoolFilter(unionDefinition);
    val response = donorSearchRequest(boolFilter);

    return SearchResponses.getHitIdsSet(response).size();
  }

  private SearchResponse donorSearchRequest(final BoolFilterBuilder boolFilter) {
    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);

    val search = client
        .prepareSearch(REPOSITORY_INDEX_NAME, indexName)
        .setTypes(DONOR_TEXT.getId(), REPOSITORY_FILE_DONOR_TEXT.getId())
        .setQuery(query)
        .setSize(maxUnionCount)
        .setNoFields()
        .setSearchType(SearchType.DEFAULT);

    log.debug("ElasticSearch query is: '{}'", search);
    val response = search.execute().actionGet();
    log.debug("ElasticSearch result is: '{}'", response);

    return response;
  }
}