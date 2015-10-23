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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.math.LongMath.divide;
import static java.math.RoundingMode.CEILING;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.limit;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.sortBuilder;
import static org.dcc.portal.pql.meta.RepositoryFileTypeModel.AVAILABLE_FACETS;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.dcc.portal.pql.meta.TypeModel.ENTITY_SET_ID;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.avg;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.IS;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.model.IndexModel.MISSING;
import static org.icgc.dcc.portal.model.IndexModel.REPOSITORY_INDEX_NAME;
import static org.icgc.dcc.portal.model.SearchFieldMapper.searchFieldMapper;
import static org.icgc.dcc.portal.model.TermFacet.repoTermFacet;
import static org.icgc.dcc.portal.service.TermsLookupService.createTermsLookupFilter;
import static org.icgc.dcc.portal.service.TermsLookupService.TermLookupType.DONOR_IDS;
import static org.icgc.dcc.portal.service.TermsLookupService.TermLookupType.FILE_IDS;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.SearchResponses.getHitIds;
import static org.icgc.dcc.portal.util.SearchResponses.getTotalHitCount;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.EsFields;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.MatchAllFilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.SearchFieldMapper;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RepositoryFileRepository {

  /**
   * Constants
   */
  private static final Set<String> FILE_DONOR_FIELDS = newHashSet(
      "specimen_id", "sample_id", "submitted_specimen_id", "submitted_sample_id",
      "id", "submitted_donor_id",
      "tcga_participant_barcode", "tcga_sample_barcode", "tcga_aliquot_barcode");

  private static final SearchFieldMapper FILE_DONOR_TEXT_SEARCH_FIELDS = searchFieldMapper()
      .partialMatchFields(FILE_DONOR_FIELDS)
      .lowercaseMatchFields(FILE_DONOR_FIELDS)
      .build();

  private static final SelectNode MANIFEST_DOWNLOAD_INFO_SELECT = select(ImmutableList.of(
      Fields.FILE_UUID,
      Fields.FILE_ID,
      Fields.STUDY,
      Fields.DATA_BUNDLE_ID,
      Fields.FILE_COPIES,
      Fields.DONORS));
  private static final SortNode MANIFEST_DOWNLOAD_INFO_SORT = sortBuilder()
      .sortAsc(Fields.REPO_TYPE).build();

  private static final TypeModel TYPE_MODEL = IndexModel.getRepositoryFileTypeModel();
  private static final String PREFIX = TYPE_MODEL.prefix();
  private static final String DONOR_ID_RAW_FIELD_NAME = toRawFieldName(Fields.DONOR_ID);
  private static final MatchAllQueryBuilder MATCH_ALL_QUERY = matchAllQuery();
  private static final MatchAllFilterBuilder MATCH_ALL_FILTER = matchAllFilter();
  private static final Jql2PqlConverter PQL_CONVERTER = Jql2PqlConverter.getInstance();
  private static final Kind KIND = Kind.REPOSITORY_FILE;
  private static final Map<String, String> JQL_FIELD_NAME_MAPPING = FIELDS_MAPPING.get(KIND);
  private static final String FILE_INDEX_TYPE = REPOSITORY_FILE.getId();
  private static final String FILE_DONOR_TEXT_INDEX_TYPE = Type.REPOSITORY_FILE_DONOR_TEXT.getId();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  // Instance variables
  private final String index = REPOSITORY_INDEX_NAME;

  /**
   * Dependencies.
   */
  private final Client client;
  private final QueryEngine queryEngine;

  @Autowired
  public RepositoryFileRepository(Client client) {
    this.client = client;
    this.queryEngine = new QueryEngine(client, index);
  }

  public static boolean isNestedField(String fieldAlias) {
    return TYPE_MODEL.isAliasDefined(fieldAlias) && TYPE_MODEL.isNested(fieldAlias);
  }

  public static String toRawFieldName(@NonNull String alias) {
    return TYPE_MODEL.getField(alias);
  }

  /**
   * FIXME: This is a temporary solution. We really should use the PQL infrastructure to build. <br>
   * Negation is not supported <br>
   * _missing is not supported for data_types.datatype and data_type.dataformat <br>
   */
  private static FilterBuilder buildRepoFilters(final ObjectNode filters) {
    val fields = filters.path(PREFIX).fields();

    if (!fields.hasNext()) {
      // If there is no filter defined under "file", return a match-all filter.
      return MATCH_ALL_FILTER;
    }

    val result = boolFilter();

    // Used for creating the terms lookup filter when ENTITY_SET_ID and donorId are in the JQL.
    FilterBuilder entitySetIdFilter = null;
    BoolFilterBuilder donorIdFilter = null;

    while (fields.hasNext()) {
      val facetField = fields.next();
      val fieldAlias = facetField.getKey();

      checkArgument(JQL_FIELD_NAME_MAPPING.containsKey(fieldAlias),
          "'%s' is not a valid field in this query.", fieldAlias);

      val filterValues = transform(newArrayList(facetField.getValue().get(IS)),
          item -> item.textValue());

      if (fieldAlias.equals(ENTITY_SET_ID)) {
        // The assumption here is there should be only one "entitySetId" filter in JQL.
        entitySetIdFilter = buildEntitySetIdFilter(filterValues);
      } else {
        val filter = buildDataFilter(fieldAlias, filterValues);

        if (fieldAlias.equals(Fields.DONOR_ID)) {
          // The assumption here is there should be only one "donorId" filter in JQL.
          donorIdFilter = filter;
        } else {
          result.must(filter);
        }
      }
    }

    // Creates the terms lookup filter when both ENTITY_SET_ID and donorId are in the JQL.
    if (null != donorIdFilter && null != entitySetIdFilter) {
      result.must(boolFilter()
          .should(donorIdFilter)
          .should(entitySetIdFilter));
    } else if (null != donorIdFilter) {
      result.must(donorIdFilter);
    } else if (null != entitySetIdFilter) {
      result.must(entitySetIdFilter);
    }

    return result;
  }

  private static NestedFilterBuilder nestedEntitySetIdFilter(String uuid) {
    return nestedFilter(EsFields.DONORS,
        createTermsLookupFilter(DONOR_ID_RAW_FIELD_NAME, DONOR_IDS, UUID.fromString(uuid)));
  }

  private static FilterBuilder buildEntitySetIdFilter(List<String> uuids) {
    if (isEmpty(uuids)) {
      return null;
    }

    if (1 == uuids.size()) {
      return nestedEntitySetIdFilter(uuids.get(0));
    }

    val result = boolFilter();

    for (val uuid : uuids) {
      result.should(nestedEntitySetIdFilter(uuid));
    }

    return result;
  }

  private static BoolFilterBuilder buildDataFilter(String fieldAlias, List<String> filterValues) {
    val rawFieldName = toRawFieldName(fieldAlias);
    val result = boolFilter();
    val terms = termsFilter(rawFieldName, filterValues);

    // Special processing for "no data" terms
    if (filterValues.remove(MISSING)) {
      val missing = missingFilter(rawFieldName).existence(true).nullValue(true);
      result.should(missing).should(terms);
    } else {
      result.must(terms);
    }

    return isNestedField(fieldAlias) ? boolFilter().must(
        nestedFilter(TYPE_MODEL.getNestedPath(fieldAlias), result)) : result;
  }

  @NonNull
  private static NestedBuilder nestedAgg(String aggName, String path, AbstractAggregationBuilder... subAggs) {
    val result = nested(aggName).path(path);

    for (val subAgg : subAggs) {
      result.subAggregation(subAgg);
    }

    return result;
  }

  @NonNull
  private static ReverseNestedBuilder reverseNestedAgg(String aggName, String path,
      AbstractAggregationBuilder... subAggs) {
    val result = reverseNested(aggName).path(path);

    for (val subAgg : subAggs) {
      result.subAggregation(subAgg);
    }

    return result;
  }

  @UtilityClass
  private class CustomAggregationFields {

    public final String DONOR = "donor";
    public final String FILE_SIZE = "fileSize";
    public final String REPO_SIZE = "repositorySizes";
    public final String REPO_NAME = "repositoryNamesFiltered";
    public final String REPO_DONOR_COUNT = "repositoryDonorCount";

  }

  private static List<AggregationBuilder<?>> aggs(final ObjectNode filters) {
    val result = Lists.<AggregationBuilder<?>> newArrayList();

    // Regular UI facets
    for (val facet : AVAILABLE_FACETS) {
      val rawFieldName = toRawFieldName(facet);
      val filterAgg = filter(facet).filter(aggFilter(filters, facet));

      // FIXME - tentative implementation!!!
      val subAgg =
          isNestedField(facet) ? (facet.equals(Fields.FILE_FORMAT) ? addNestedSubAggregationsForFileFormat(filterAgg,
              facet, rawFieldName, TYPE_MODEL.getNestedPath(facet)) : addNestedSubAggregations(filterAgg, facet,
                  rawFieldName, TYPE_MODEL.getNestedPath(facet))) : addSubAggregations(filterAgg, facet, rawFieldName);

      result.add(global(facet).subAggregation(subAgg));
    }

    val repoFilters = buildRepoFilters(filters.deepCopy());
    val repoNameFieldName = toRawFieldName(Fields.REPO_NAME);

    /*
     * Facets that aren't visible in the UI. Special filtered cases (do not exclude self filtering):
     * repositoryNamesFiltered & repositorySize
     */
    // repositoryNamesFiltered - file count
    val repoNameAggName = CustomAggregationFields.REPO_NAME;
    val filterAgg = filter(repoNameAggName).filter(repoFilters);
    val repoNameSubAgg = addNestedSubAggregations(filterAgg, repoNameAggName, repoNameFieldName, EsFields.FILE_COPIES);

    result.add(global(repoNameAggName).subAggregation(repoNameSubAgg));

    // repositorySize
    val repoSizeAggName = CustomAggregationFields.REPO_SIZE;
    val repoSizeTermsSubAgg = terms(repoSizeAggName).field(repoNameFieldName)
        .size(MAX_FACET_TERM_COUNT)
        .subAggregation(reverseNestedAgg(CustomAggregationFields.DONOR, EsFields.DONORS,
            terms(CustomAggregationFields.DONOR).size(100000).field(DONOR_ID_RAW_FIELD_NAME)))
        .subAggregation(
            sum(CustomAggregationFields.FILE_SIZE).field(toRawFieldName(Fields.FILE_SIZE)));
    val repoSizeSubAgg = filter(repoSizeAggName)
        .filter(repoFilters)
        .subAggregation(nestedAgg(repoSizeAggName, EsFields.FILE_COPIES, repoSizeTermsSubAgg));

    result.add(global(repoSizeAggName).subAggregation(repoSizeSubAgg));

    return result;
  }

  private static FilterBuilder aggFilter(final ObjectNode filters, String facetAlias) {
    if (!filters.fieldNames().hasNext()) {
      return MATCH_ALL_FILTER;
    }

    val facetFilters = filters.deepCopy();

    if (facetFilters.has(PREFIX)) {
      // Remove the facet itself from the "file" filter.
      facetFilters.with(PREFIX).remove(facetAlias);
    }

    return buildRepoFilters(facetFilters);
  }

  @NonNull
  private static FilterAggregationBuilder addNestedSubAggregations(FilterAggregationBuilder builder,
      String facetName, String fieldName, String path) {
    val termsAgg = terms(facetName).field(fieldName)
        .size(MAX_FACET_TERM_COUNT);
    val missingAgg = missing(MISSING).field(fieldName);
    val nestedAgg = nestedAgg(facetName, path, termsAgg, missingAgg);

    return builder.subAggregation(nestedAgg);
  }

  // FIXME - tentative implementation
  @NonNull
  private static FilterAggregationBuilder addNestedSubAggregationsForFileFormat(FilterAggregationBuilder builder,
      String facetName, String fieldName, String path) {
    val termsAgg = terms(facetName).field(fieldName)
        .size(MAX_FACET_TERM_COUNT);
    val reverseNestedAgg = termsAgg.subAggregation(reverseNested(facetName));
    val missingAgg = missing(MISSING).field(fieldName);
    val nestedAgg = nestedAgg(facetName, path, reverseNestedAgg, missingAgg);

    return builder.subAggregation(nestedAgg);
  }

  @NonNull
  private static FilterAggregationBuilder addSubAggregations(FilterAggregationBuilder builder,
      String facetName, String fieldName) {
    val termsAgg = terms(facetName).field(fieldName)
        .size(MAX_FACET_TERM_COUNT);
    val missingAgg = missing(MISSING).field(fieldName);

    return builder.subAggregation(termsAgg).subAggregation(missingAgg);
  }

  public static Map<String, TermFacet> convertAggregationsToFacets(@NonNull Aggregations aggs) {
    val result = Maps.<String, TermFacet> newHashMap();

    for (val agg : aggs) {
      val name = agg.getName();

      if (name.equals(CustomAggregationFields.REPO_SIZE)) {
        val buckets = getBucketsOfRepoAggregations(agg);

        result.put(CustomAggregationFields.REPO_SIZE, convertRepoSizeAggregation(buckets));
        result.put("repositoryDonors", convertRepoDonorAggregation(buckets));

      } else if (name.equals(Fields.FILE_FORMAT)) {
        // FIXME - tentative implementation
        result.put(name, convertFileFormatAggregation(agg));
      } else {
        result.put(name, convertNormalAggregation(agg));
      }
    }

    log.debug("Result of convertAggregationsToFacets is: '{}'.", result);
    return result;
  }

  private static List<Bucket> getBucketsOfRepoAggregations(Aggregation aggregation) {
    val name = aggregation.getName();
    val globalAgg = (Global) aggregation;
    val filterAgg = (Filter) globalAgg.getAggregations().get(name);
    val termAgg = (Terms) getSubAggResultFromNested(filterAgg.getAggregations(), name).get(name);
    // val termAgg = (Terms) filterAgg.getAggregations().get(name);

    return termAgg.getBuckets();
  }

  // Special aggregation to get unique donor count for each repository
  private static TermFacet convertRepoDonorAggregation(List<Bucket> buckets) {
    val terms = buckets.stream().map(bucket -> {
      final int size = bucketSize(
          getSubAggResultFromReverseNested(bucket.getAggregations(), CustomAggregationFields.DONOR),
          CustomAggregationFields.DONOR);

      return new Term(bucket.getKey(), Long.valueOf(size));
    }).collect(toImmutableList());

    // Total does not have any meaning in this context because a donor can cross repositories
    val total = -1L;
    return repoTermFacet(total, 0, terms);
  }

  // Special aggregation to get file size for each repository
  private static TermFacet convertRepoSizeAggregation(List<Bucket> buckets) {
    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : buckets) {
      val childCount = sumValue(bucket.getAggregations(), CustomAggregationFields.FILE_SIZE);
      // val childCount = sumValue(
      // getSubAggResultFromNested(bucket.getAggregations(), CustomAggregationFields.FILE_SIZE),
      // CustomAggregationFields.FILE_SIZE);

      termsBuilder.add(new Term(bucket.getKey(), (long) childCount));
      total += childCount;
    }

    return repoTermFacet(total, 0, termsBuilder.build());
  }

  private static TermFacet convertNormalAggregation(Aggregation agg) {
    val name = agg.getName();
    log.debug("Normal Facet {}", name);

    val globalAggs = ((Global) agg).getAggregations();
    val filterAgg = ((Filter) globalAggs.get(name)).getAggregations();

    val isNested = isNestedField(name) || name.equals(CustomAggregationFields.REPO_NAME);
    val termsAgg = isNested ? getSubAggResultFromNested(filterAgg, name) : filterAgg;
    val aggResult = (Terms) termsAgg.get(name);

    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : aggResult.getBuckets()) {
      val bucketKey = bucket.getKey();
      val count = bucket.getDocCount();
      log.debug("convertNormalAggregation bucketKey: {}, count: {}", bucketKey, count);

      total += count;
      termsBuilder.add(new Term(bucketKey, count));
    }

    val missingAgg = (Missing) termsAgg.get(MISSING);
    val missingCount = missingAgg.getDocCount();
    log.debug("convertNormalAggregation Missing count is: {}", missingCount);

    if (missingCount > 0) {
      termsBuilder.add(new Term(MISSING, missingCount));
    }

    return repoTermFacet(total, missingCount, termsBuilder.build());
  }

  // FIXME - tentative implementation
  private static TermFacet convertFileFormatAggregation(Aggregation agg) {
    val name = agg.getName();
    log.debug("Normal Facet {}", name);

    val globalAggs = ((Global) agg).getAggregations();
    val filterAgg = ((Filter) globalAggs.get(name)).getAggregations();

    val isNested = isNestedField(name) || name.equals(CustomAggregationFields.REPO_NAME);
    val termsAgg = isNested ? getSubAggResultFromNested(filterAgg, name) : filterAgg;
    val aggResult = (Terms) termsAgg.get(name);

    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : aggResult.getBuckets()) {
      val bucketKey = bucket.getKey();
      val count = ((SingleBucketAggregation) bucket.getAggregations().get(name)).getDocCount();
      log.debug("convertNormalAggregation bucketKey: {}, count: {}", bucketKey, count);

      total += count;
      termsBuilder.add(new Term(bucketKey, count));
    }

    val missingAgg = (Missing) termsAgg.get(MISSING);
    val missingCount = missingAgg.getDocCount();
    log.debug("convertNormalAggregation Missing count is: {}", missingCount);

    if (missingCount > 0) {
      termsBuilder.add(new Term(MISSING, missingCount));
    }

    return repoTermFacet(total, missingCount, termsBuilder.build());
  }

  @NonNull
  private SearchResponse searchRepositoryFiles(String indexType, String logMessage,
      Consumer<SearchRequestBuilder> customizer) {
    val request = client.prepareSearch(index).setTypes(indexType);
    customizer.accept(request);

    log.info(logMessage + "; ES query is: '{}'", request);
    return request.execute().actionGet();
  }

  private SearchResponse searchFileCentric(String logMessage, Consumer<SearchRequestBuilder> customizer) {
    return searchRepositoryFiles(FILE_INDEX_TYPE, logMessage, customizer);
  }

  private SearchResponse searchFileDonorText(String logMessage, Consumer<SearchRequestBuilder> customizer) {
    return searchRepositoryFiles(FILE_DONOR_TEXT_INDEX_TYPE, logMessage, customizer);
  }

  @NonNull
  private SearchResponse pqlSearchFileCentric(String logMessage, StatementNode pqlAst,
      Consumer<SearchRequestBuilder> customizer) {
    val request = queryEngine.execute(pqlAst, REPOSITORY_FILE).getRequestBuilder();
    customizer.accept(request);

    log.info(logMessage + "; ES query is: '{}'", request);
    return request.execute().actionGet();
  }

  private SearchResponse prepareDataTableExport(Consumer<SearchRequestBuilder> queryCustomizer, String[] fields) {
    val size = 5000;

    return searchFileCentric("Preparing data table export", request -> {
      request.setSearchType(SCAN)
          .setSize(size)
          .setScroll(KEEP_ALIVE)
          .addFields(fields);

      queryCustomizer.accept(request);
    });
  }

  public SearchResponse fetchSearchScrollData(@NonNull String scrollId) {
    return client.prepareSearchScroll(scrollId)
        .setScroll(KEEP_ALIVE)
        .execute()
        .actionGet();
  }

  @NonNull
  public SearchResponse prepareDataExport(Query query, final String[] fields) {
    val filters = buildRepoFilters(query.getFilters());

    return prepareDataTableExport(request -> request.setPostFilter(filters).setQuery(MATCH_ALL_QUERY), fields);
  }

  // FIXME: Support terms lookup on files as part of the filter builder so we don't need an extra method.
  @NonNull
  public SearchResponse prepareDataExportFromSet(String setId, final String[] fields) {
    val query = buildFileSetIdQuery(setId);

    return prepareDataTableExport(request -> request.setQuery(query), fields);
  }

  private static FilteredQueryBuilder buildFileSetIdQuery(String setId) {
    val lookupFilter = createTermsLookupFilter(toRawFieldName(Fields.ID), FILE_IDS, UUID.fromString(setId));
    return new FilteredQueryBuilder(MATCH_ALL_QUERY, lookupFilter);
  }

  public GetResponse findOne(@NonNull String id) {
    val search = client.prepareGet(index, FILE_INDEX_TYPE, id);
    val response = search.execute().actionGet();
    // This check is important as it validates if there is any document at all in the GET response.
    checkResponseState(id, response, KIND);

    return response;
  }

  public SearchResponse findAll(@NonNull Query query) {
    val queryFilter = query.getFilters();
    val filters = buildRepoFilters(queryFilter);

    val response = searchFileCentric("findAll()", request -> {
      request.setSearchType(QUERY_THEN_FETCH)
          .setFrom(query.getFrom())
          .setSize(query.getSize())
          .addSort(JQL_FIELD_NAME_MAPPING.get(query.getSort()), query.getOrder())
          // .setQuery(boolFilter().must(filters));
          .setQuery(new FilteredQueryBuilder(MATCH_ALL_QUERY, filters));
      // .setPostFilter(filters);

      aggs(queryFilter).stream().forEach(
          agg -> request.addAggregation(agg));
    });

    log.debug("findAll() - ES response is: '{}'.", response);
    return response;
  }

  public Set<String> findAllDonorIds(@NonNull Query query, final int setLimit) {
    val pqlAst = parse(PQL_CONVERTER.convert(query, REPOSITORY_FILE));
    val size = query.getSize();
    int pageNumber = 0;

    SearchResponse response = fetchDonorIds(pqlAst, pageNumber, size);

    val result = Sets.<String> newHashSet();
    val pageCount = divide(getTotalHitCount(response), size, CEILING);

    // Number of files > max limit, so we must page files in order to ensure we get all donors.
    while (pageNumber <= pageCount) {
      for (val hit : response.getHits()) {
        val donorIds = hit.field(DONOR_ID_RAW_FIELD_NAME).getValues();
        result.addAll(transform(donorIds, id -> id.toString()));

        if (result.size() >= setLimit) {
          return result;
        }
      }

      response = fetchDonorIds(pqlAst, ++pageNumber, size);
    }

    return result;
  }

  private SearchResponse fetchDonorIds(@NonNull StatementNode pqlAst, int pageNumber, int size) {
    pqlAst.setLimit(limit(pageNumber * size, size));

    val response = pqlSearchFileCentric("fetchDonorIds", pqlAst, request -> {
    });

    log.debug("findAllDonorIds() - ES response is: '{}'.", response);
    return response;
  }

  public List<String> findAllFileIds(Query query) {
    val queryFilter = query.getFilters();
    val filters = buildRepoFilters(queryFilter);

    val response = searchFileCentric("Files Ids from Query", (request) -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(JQL_FIELD_NAME_MAPPING.get(query.getSort()), query.getOrder())
        .setPostFilter(filters));

    return getHitIds(response);
  }

  @NonNull
  public SearchResponse findDownloadInfoFromSet(String setId) {
    val pqlAst = parse("select(*)");
    pqlAst.setSelect(MANIFEST_DOWNLOAD_INFO_SELECT);

    val response = pqlSearchFileCentric("Donor Info From Set Id", pqlAst, request -> request
        .setFrom(0)
        .setSize(20000)
        .setQuery(buildFileSetIdQuery(setId)));

    log.debug("ES response is: {}", response);
    return response;
  }

  public SearchResponse findDownloadInfo(@NonNull final String pql) {
    val pqlAst = parse(pql);
    pqlAst.setSelect(MANIFEST_DOWNLOAD_INFO_SELECT);
    pqlAst.setSort(MANIFEST_DOWNLOAD_INFO_SORT);

    log.debug("PQL for download is: '{}'.", pqlAst.toString());

    // Get the total count first.
    val count = getTotalHitCount(findDownloadInfo(pqlAst, COUNT, 0));
    log.debug("A total of {} files will be returned from this query.", count);

    return findDownloadInfo(pqlAst, QUERY_THEN_FETCH, Ints.saturatedCast(count));
  }

  @NonNull
  private SearchResponse findDownloadInfo(StatementNode pqlAst, SearchType searchType, int size) {
    val response = pqlSearchFileCentric("findDownloadInfo", pqlAst, request -> request
        .setSearchType(searchType)
        .setSize(size));

    log.debug("ES response is: {}", response);
    return response;
  }

  @NonNull
  public static String[] toStringArray(Iterable<String> strings) {
    return StreamSupport.stream(strings.spliterator(), false)
        .toArray(String[]::new);
  }

  /**
   * @param fields - A list of field names that form the search query.
   * @param queryString - User input - could be any value out of one of the fields.
   * @return
   */
  @NonNull
  public SearchResponse findRepoDonor(Iterable<String> fields, String queryString) {
    val maxNumberOfDocs = 5;
    val fieldNames = FILE_DONOR_TEXT_SEARCH_FIELDS.map(fields);

    val result = searchFileDonorText("findRepoDonor", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(maxNumberOfDocs)
        .setQuery(multiMatchQuery(queryString, toStringArray(fieldNames))));

    log.debug("ES search result is: '{}'.", result);
    return result;
  }

  private static TermsBuilder averageFileSizePerFileCopyAgg(@NonNull String aggName) {
    return terms(aggName).size(100000).field(toRawFieldName(Fields.FILE_ID))
        .subAggregation(nestedAgg(aggName, EsFields.FILE_COPIES,
            avg(aggName).field(toRawFieldName(Fields.FILE_SIZE))));
  }

  @UtilityClass
  private class SummaryFields {

    private final String FILE = "file";
    private final String DONOR = "donor";
    private final String PROJECT = "project";
    private final String PRIMARY_SITE = "primarySite";

  }

  /**
   * Get total file size, total donor count and total number of files based on query
   */
  @NonNull
  public Map<String, Long> getSummary(Query query) {
    val donorSubAggs = nestedAgg(SummaryFields.DONOR, EsFields.DONORS,
        terms(SummaryFields.DONOR).size(100000).field(DONOR_ID_RAW_FIELD_NAME))
            .subAggregation(
                terms(SummaryFields.PROJECT).size(1000).field(toRawFieldName(Fields.PROJECT_CODE)))
            .subAggregation(
                terms(SummaryFields.PRIMARY_SITE).size(1000).field(toRawFieldName(Fields.PRIMARY_SITE)));

    val fileSizeSubAgg = averageFileSizePerFileCopyAgg(SummaryFields.FILE);

    val filters = buildRepoFilters(query.getFilters());

    val response = searchFileCentric("Summary aggregation", request -> request
        .setSearchType(COUNT)
        .setFrom(0)
        .setSize(0)
        .setQuery(new FilteredQueryBuilder(MATCH_ALL_QUERY, filters))
        .addAggregation(fileSizeSubAgg)
        .addAggregation(donorSubAggs));

    log.debug("getSummary aggregation result is: '{}'.", response);

    val aggResult = response.getAggregations();
    val buckets = ((Terms) aggResult.get(SummaryFields.FILE)).getBuckets();
    val totalFileSize = sumFileCopySize(buckets, SummaryFields.FILE);
    val donorAggResult = getSubAggResultFromNested(aggResult, SummaryFields.DONOR);

    return ImmutableMap.<String, Long> of(
        "fileCount", getTotalHitCount(response),
        "totalFileSize", (long) totalFileSize,
        "donorCount", (long) bucketSize(donorAggResult, SummaryFields.DONOR),
        "projectCount", (long) bucketSize(donorAggResult, SummaryFields.PROJECT),
        "primarySiteCount", (long) bucketSize(donorAggResult, SummaryFields.PRIMARY_SITE));
  }

  /**
   * Returns the unique donor count across repositories Note we are counting the bucket size of a term aggregation. It
   * appears that using cardinality aggregation yields imprecise result.
   */
  @NonNull
  public long getDonorCount(Query query) {
    val aggName = "donorCount";
    val filters = buildRepoFilters(query.getFilters());
    // TODO: Look into a better solution than using terms agg and buckets.
    val nestedAgg = nestedAgg(aggName, EsFields.DONORS,
        terms(aggName).size(100000).field(DONOR_ID_RAW_FIELD_NAME));
    val filterAgg = filter(aggName).filter(filters).subAggregation(nestedAgg);

    val response = searchFileCentric("Donor Count aggregation", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .setPostFilter(filters)
        .addAggregation(global(aggName).subAggregation(filterAgg)));

    log.debug("getDonorCount aggregation result is: '{}'.", response);

    return bucketSize(getSubAggResultFromNested(
        getAggregationResult(response, aggName), aggName), aggName);
  }

  @NonNull
  private static Aggregations getAggregationResult(SearchResponse response, String name) {
    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) global.getAggregations().get(name);

    return filter.getAggregations();
  }

  @NonNull
  private static Aggregations getNestedAggregationResult(SearchResponse response, String name) {
    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) getSubAggResultFromNested(global.getAggregations(), name).get(name);

    return filter.getAggregations();
  }

  private static Aggregations getSubAggResultFromNested(Aggregations nestedAggs, String aggKey) {
    return ((Nested) nestedAggs.get(aggKey)).getAggregations();
  }

  private static Aggregations getSubAggResultFromReverseNested(Aggregations nestedAggs, String aggKey) {
    return ((ReverseNested) nestedAggs.get(aggKey)).getAggregations();
  }

  @NonNull
  private static int bucketSize(Aggregations aggResult, String name) {
    return ((Terms) aggResult.get(name)).getBuckets().size();
  }

  @NonNull
  private static double sumValue(Aggregations aggResult, String name) {
    return ((Sum) aggResult.get(name)).getValue();
  }

  @NonNull
  private static double averageValue(Aggregations aggResult, String name) {
    return ((Avg) aggResult.get(name)).getValue();
  }

  private static double sumFileCopySize(List<Bucket> buckets, String aggregationKey) {
    return buckets.stream().mapToDouble(
        bucket -> averageValue(
            getSubAggResultFromNested(bucket.getAggregations(), aggregationKey), aggregationKey))
        .sum();
  }

  public Map<String, String> getRepositoryMap() {
    val repoName = toRawFieldName(Fields.REPO_NAME);
    val repoCode = toRawFieldName(Fields.REPO_CODE);
    val repoNameSubAgg = terms(repoName).field(repoName);
    val repoCodeSubAgg = nestedAgg(repoCode, EsFields.FILE_COPIES,
        terms(repoCode).size(100).field(repoCode).subAggregation(repoNameSubAgg));
    val response = searchFileCentric("repoCodeNameMapping", request -> request
        .setSearchType(COUNT)
        .addAggregation(repoCodeSubAgg));
    val terms = (Terms) getSubAggResultFromNested(response.getAggregations(), repoCode).get(repoCode);

    return terms.getBuckets().stream().collect(toMap(bucket -> bucket.getKey(),
        bucket -> {
          final List<Bucket> repoNameBuckets = ((Terms) bucket.getAggregations().get(repoName)).getBuckets();

          return isEmpty(repoNameBuckets) ? "" : repoNameBuckets.get(0).getKey();
        }));
  }

  @UtilityClass
  private class StatsAggregationKeys {

    private final String DONOR = "donor";
    private final String SIZE = "size";
    private final String FORMAT = "format";
    private final String DONOR_PRIMARY_SITE = "donorPrimarySite";

  }

  @NonNull
  public Map<String, Map<String, Map<String, Object>>> getRepoStats(String repoName) {
    val aggsFilter = nestedFilter(EsFields.FILE_COPIES, termFilter(toRawFieldName(Fields.REPO_CODE), repoName));
    val response = getStats(aggsFilter, repoName);

    return extractStats(response.getAggregations(), repoName);
  }

  @NonNull
  public Map<String, Map<String, Map<String, Object>>> getStudyStats(String study) {
    val aggsFilter = termFilter(toRawFieldName(Fields.STUDY), study);
    val response = getStats(aggsFilter, study);

    return extractStats(response.getAggregations(), study);
  }

  private SearchResponse getStats(FilterBuilder filter, String aggName) {
    val donorCountAgg = nestedAgg(StatsAggregationKeys.DONOR, EsFields.DONORS,
        terms(StatsAggregationKeys.DONOR).field(DONOR_ID_RAW_FIELD_NAME).size(30000));

    val fileSizeAgg = averageFileSizePerFileCopyAgg(StatsAggregationKeys.SIZE);

    val fileFormatAgg = nestedAgg(StatsAggregationKeys.FORMAT, EsFields.FILE_COPIES,
        terms(StatsAggregationKeys.FORMAT).field(toRawFieldName(Fields.FILE_FORMAT)));

    val dataTypeAgg = terms(aggName).field(toRawFieldName(Fields.DATA_TYPE))
        .subAggregation(donorCountAgg)
        .subAggregation(fileSizeAgg)
        .subAggregation(fileFormatAgg);

    // Primary Site => Project Code => Donor ID
    val primarySiteAgg = primarySiteAgg(Fields.PRIMARY_SITE, 100)
        .subAggregation(primarySiteAgg(Fields.PROJECT_CODE, 100)
            .subAggregation(primarySiteAgg(Fields.DONOR_ID, 30000)));

    val statsAgg = filter(aggName)
        .filter(filter)
        .subAggregation(dataTypeAgg)
        .subAggregation(nestedAgg(StatsAggregationKeys.DONOR_PRIMARY_SITE, EsFields.DONORS, primarySiteAgg));

    val response = searchFileCentric("getStats Request", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .addAggregation(statsAgg));

    log.debug("getStats Response: '{}'.", response);

    return response;
  }

  private static TermsBuilder primarySiteAgg(@NonNull String fieldAlias, int size) {
    return terms(StatsAggregationKeys.DONOR_PRIMARY_SITE)
        .field(toRawFieldName(fieldAlias))
        .size(size);
  }

  private static Map<String, Map<String, Map<String, Object>>> extractStats(Aggregations aggs, String aggName) {
    val stats = (Filter) aggs.get(aggName);
    val statsAggregations = stats.getAggregations();

    val result = Maps.<String, Map<String, Map<String, Object>>> newHashMap();

    // donorPrimarySite
    val donorPrimarySite = Maps.<String, Map<String, Object>> newHashMap();
    val primarySiteAggKey = StatsAggregationKeys.DONOR_PRIMARY_SITE;
    val donorFacets = (Terms) getSubAggResultFromNested(statsAggregations, primarySiteAggKey)
        .get(primarySiteAggKey);

    for (val bucket : donorFacets.getBuckets()) {
      val projectFacets = (Terms) bucket.getAggregations().get(primarySiteAggKey);
      val newEntries = projectFacets.getBuckets().stream().collect(toMap(
          project -> project.getKey(),
          project -> bucketSize(project.getAggregations(), primarySiteAggKey)));

      val name = bucket.getKey();
      val map = donorPrimarySite.getOrDefault(name, Maps.<String, Object> newHashMap());
      map.putAll(newEntries);

      donorPrimarySite.putIfAbsent(name, map);
    }

    result.put(primarySiteAggKey, donorPrimarySite);

    // statistics
    val statistics = Maps.<String, Map<String, Object>> newHashMap();
    val datatypes = (Terms) statsAggregations.get(aggName);

    for (val bucket : datatypes.getBuckets()) {
      val bucketAggregations = bucket.getAggregations();
      val donorCount = bucketSize(getSubAggResultFromNested(bucketAggregations, StatsAggregationKeys.DONOR),
          StatsAggregationKeys.DONOR);

      val fileSizeResult = (Terms) bucketAggregations.get(StatsAggregationKeys.SIZE);
      val totalFileSize = sumFileCopySize(fileSizeResult.getBuckets(), StatsAggregationKeys.SIZE);

      val dataFormat = (Terms) getSubAggResultFromNested(bucketAggregations, StatsAggregationKeys.FORMAT)
          .get(StatsAggregationKeys.FORMAT);
      val formats = transform(dataFormat.getBuckets(), b -> b.getKey());

      // TODO: We should use StatsAggregationKeys for these keys too, though it requires changes in the client side.
      val map = ImmutableMap.<String, Object> of(
          "fileCount", bucket.getDocCount(),
          "donorCount", donorCount,
          "fileSize", totalFileSize,
          "dataFormat", formats);

      statistics.put(bucket.getKey(), map);
    }

    result.put("stats", statistics);

    log.debug("Result {}", result);
    return result;
  }

  @SneakyThrows
  public Map<String, String> getIndexMetaData() {
    val state = client.admin().cluster().prepareState().setIndices(index).execute().actionGet().getState();

    val realIndex = (state.getMetaData().getAliases().get(index)).iterator().next().key;

    val indexMetaData = state.getMetaData().index(realIndex);

    val mappingMetaData = indexMetaData.getMappings().values().iterator().next().value;
    val source = mappingMetaData.sourceAsMap();

    @SuppressWarnings("unchecked")
    val meta = (Map<String, String>) source.get("_meta");

    return (meta == null) ? emptyMap() : meta;
  }

}