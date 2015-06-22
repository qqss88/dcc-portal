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
import static org.icgc.dcc.portal.model.Query.NO_FIELDS;
import static org.icgc.dcc.portal.service.TermsLookupService.TERMS_LOOKUP_PATH;
import static org.icgc.dcc.portal.util.JsonUtils.LIST_TYPE_REFERENCE;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Provides various set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UnionAnalyzer {

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
    val lookupType = entityType.toLookupTypeFrom();
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

      for (val def : definitions) {
        val count = getUnionCount(def, entityType);
        result.add(UnionUnitWithCount.copyOf(def, count));
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

      val response = unionAll(definitions, entityType, maxUnionCount);

      val totalHits = SearchResponses.getTotalHitCount(response);
      if (totalHits > maxUnionCount) {
        // If the total hit count exceeds the allowed maximum, flag this list and quit.
        log.info(
            "Because the total hit count ({}) exceeds the allowed maximum ({}), this set operation is aborted.",
            totalHits, maxUnionCount);

        entityListRepository.update(newEntity.updateStateToError(), dataVersion);
        return;
      }

      val entityIds = SearchResponses.getHitIds(response);
      log.debug("Union result is: '{}'", entityIds);

      // val watch = Stopwatch.createStarted();

      val lookupType = entityType.toLookupTypeFrom();

      if (entitySetDefinition.isTransient()) {
        val additionalAttribute = new HashMap<String, Object>() {

          {
            put("transient", true);
          }
        };

        termLookupService.createTermsLookup(lookupType, newEntityId, entityIds,
            additionalAttribute);
      } else {
        termLookupService.createTermsLookup(lookupType, newEntityId, entityIds);
      }

      // watch.stop();
      // log.info("createTermsLookup took {} nanoseconds for creating a derived list for entity type - {}",
      // watch.elapsed(TimeUnit.NANOSECONDS), entityType);

      val count = getCountFrom(response, maxUnionCount);
      // Done - update status to finished
      entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
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
        .fields(NO_FIELDS)
        .filters(definition.getFilters())
        .sort(definition.getSortBy())
        .order(definition.getSortOrder().getName())
        .size(max)
        .limit(max)
        .build();

    val type = getRepositoryByEntityType(definition.getType());
    val pql = converter.convert(query, type);
    val request = queryEngine.execute(pql, type);
    return request.getRequestBuilder().execute().actionGet();

  }

  @Async
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

      val lookupType = entitySetDefinition.getType().toLookupTypeFrom();

      // val watch = Stopwatch.createStarted();

      termLookupService.createTermsLookup(lookupType, newEntityId, entityIds);

      // watch.stop();
      // log.info("createTermsLookup took {} nanoseconds for creating a new list for entity type - {}",
      // watch.elapsed(TimeUnit.NANOSECONDS), entityType);

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
    val lookupTypeName = entityList.getType().toLookupTypeFrom().getName();
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
}
