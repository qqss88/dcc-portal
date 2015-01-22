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
package org.icgc.dcc.portal.repository;

import static org.elasticsearch.index.query.FilterBuilders.boolFilter;

import java.util.UUID;

import lombok.NonNull;
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
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TODO
 */
@Slf4j
@Component
public class UnionAnalyzer {// implements Repository {

  private final Client client;
  private final String index;

  private final static String FIELD_NAME = "_id";
  private final static MatchAllQueryBuilder MATCH_ALL = QueryBuilders.matchAllQuery();

  // TODO: temp
  @Autowired
  UnionAnalyzer(
      final Client client,
      final IndexModel indexModel) {

    this.index = indexModel.getIndex();
    this.client = client;
  }

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

  private static long getCountFrom(@NonNull final SearchResponse response) {

    return response.getHits().totalHits();
  }

  public long getUnionCount(
      final UnionUnit unionDefinition,
      final BaseEntityList.Type entityType) {

    val response = filter(
        getIndexTypeNameFrom(entityType),
        SearchType.COUNT,
        toBoolFilterFrom(unionDefinition, entityType));

    val count = getCountFrom(response);
    log.info("Total hits: {}", count);

    return count;
  }

  public long unionAll(
      final Iterable<UnionUnit> definitions,
      final BaseEntityList.Type entityType,
      // temp
      final UUID listId,
      final TermsLookupService termLookupService) {

    val response = filter(
        getIndexTypeNameFrom(entityType),
        SearchType.QUERY_THEN_FETCH,
        toBoolFilterFrom(definitions, entityType));

    val entityIds = SearchResponses.getHitIds(response);
    log.info("Union result is: '{}'", entityIds);

    val count = getCountFrom(response);

    termLookupService.createTermsLookup(getLookupTypeFrom(entityType), listId, entityIds);
    return count;
  }

  public SearchResponse filter(
      final String indexTypeName,
      final SearchType searchType,
      final BoolFilterBuilder boolFilter) {

    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);

    val search = client
        .prepareSearch(this.index)
        .setTypes(indexTypeName)
        .setSearchType(searchType)
        .setQuery(query)
        .setNoFields();

    log.info("ElasticSearch query is: '{}'", search);

    val response = search.execute().actionGet();

    log.info("ElasticSearch result is: '{}'", response);

    // val hits = response.getHits();
    return response;
  }

}