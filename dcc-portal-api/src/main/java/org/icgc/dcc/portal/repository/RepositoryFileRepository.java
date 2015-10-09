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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.union;
import static com.google.common.math.LongMath.divide;
import static java.lang.Long.parseLong;
import static java.math.RoundingMode.CEILING;
import static java.util.Collections.emptyMap;
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
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
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
import static org.icgc.dcc.portal.util.SearchResponses.getTotalHitCount;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.MatchAllFilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
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
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvMapWriter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

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
  private static final Joiner COMMA_JOINER = COMMA.skipNulls();

  private static final Map<String, String> DATA_TABLE_EXPORT_MAP = ImmutableMap.<String, String> builder()
      .put(Fields.ACCESS, "Access")
      .put(Fields.FILE_ID, "File ID")
      .put(Fields.DONOR_ID, "ICGC Donor")
      .put(Fields.REPO_NAME, "Repository")
      .put(Fields.PROJECT_CODE, "Project")
      .put(Fields.STUDY, "Study")
      .put(Fields.DATA_TYPE, "Data Type")
      .put(Fields.FILE_FORMAT, "Format")
      .put(Fields.FILE_SIZE, "Size (bytes)")
      .build();
  private static final Set<String> DATA_TABLE_EXPORT_MAP_FIELD_KEYS = toRawFieldSet(
      DATA_TABLE_EXPORT_MAP.keySet());
  private static final Set<String> DATA_TABLE_EXPORT_SUMMARY_FIELDs = toRawFieldSet(newArrayList(
      Fields.DONOR_ID, Fields.PROJECT_CODE));
  private static final Set<String> DATA_TABLE_EXPORT_AVERAGE_FIELDs = toRawFieldSet(newArrayList(
      Fields.FILE_SIZE));
  private static final Set<String> DATA_TABLE_EXPORT_OTHER_FIELDs = difference(DATA_TABLE_EXPORT_MAP_FIELD_KEYS,
      union(DATA_TABLE_EXPORT_SUMMARY_FIELDs, DATA_TABLE_EXPORT_AVERAGE_FIELDs));

  private static final Jql2PqlConverter PQL_CONVERTER = Jql2PqlConverter.getInstance();
  private static final Kind KIND = Kind.REPOSITORY_FILE;
  private static final Map<String, String> JQL_FIELD_NAME_MAPPING = FIELDS_MAPPING.get(KIND);
  private static final String FILE_INDEX_TYPE = REPOSITORY_FILE.getId();
  private static final String FILE_DONOR_TEXT_INDEX_TYPE = Type.REPOSITORY_FILE_DONOR_TEXT.getId();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);
  private static final String PCAWG = "PCAWG";

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

  private static Set<String> toRawFieldSet(Collection<String> aliases) {
    return aliases.stream().map(k -> toRawFieldName(k))
        .collect(toImmutableSet());
  }

  private static boolean isNestedField(String fieldAlias) {
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
    BoolFilterBuilder entitySetIdFilter = null;
    BoolFilterBuilder donorIdFilter = null;

    while (fields.hasNext()) {
      val facetField = fields.next();
      val fieldAlias = facetField.getKey();

      if (!JQL_FIELD_NAME_MAPPING.containsKey(fieldAlias)) {
        continue;
      }

      val filterValues = transform(newArrayList(facetField.getValue().get(IS)),
          item -> item.textValue());

      if (fieldAlias.equals(ENTITY_SET_ID)) {
        // The assumption here is there should be only one "entitySetId" filter in JQL.
        entitySetIdFilter = buildEntitySetIdFilter(filterValues);
      } else {
        val rawFieldName = toRawFieldName(fieldAlias);
        val filter = buildDataFilter(fieldAlias, rawFieldName, filterValues);

        if (rawFieldName.equals(DONOR_ID_RAW_FIELD_NAME)) {
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

  private static BoolFilterBuilder buildEntitySetIdFilter(Iterable<String> filterValues) {
    val result = boolFilter();

    for (val value : filterValues) {
      val lookupFilter = createTermsLookupFilter(DONOR_ID_RAW_FIELD_NAME, DONOR_IDS, UUID.fromString(value));
      result.should(nestedFilter(EsFields.DONORS, lookupFilter));
    }

    return result;
  }

  private static BoolFilterBuilder buildDataFilter(String fieldAlias, String rawfieldName, List<String> filterValues) {
    val result = boolFilter();
    val terms = termsFilter(rawfieldName, filterValues);

    // Special processing for "no data" terms
    if (filterValues.remove(MISSING)) {
      val missing = missingFilter(rawfieldName).existence(true).nullValue(true);
      result.should(missing).should(terms);
    } else {
      result.must(terms);
    }

    return isNestedField(fieldAlias) ? boolFilter()
        .must(nestedFilter(TYPE_MODEL.getNestedPath(fieldAlias), result)) : result;
  }

  @NonNull
  private static NestedBuilder nestedAgg(String aggName, String path, AbstractAggregationBuilder... subAggs) {
    val result = nested(aggName).path(path);

    for (val subAgg : subAggs) {
      result.subAggregation(subAgg);
    }

    return result;
  }

  @UtilityClass
  private class CustomAggregationFields {

    private final String DONOR = "donor";
    private final String FILE_SIZE = "fileSize";
    private final String REPO_SIZE = "repositorySizes";
    private final String REPO_NAME = "repositoryNamesFiltered";

  }

  private static List<AggregationBuilder<?>> aggs(final ObjectNode filters) {
    val result = Lists.<AggregationBuilder<?>> newArrayList();

    // Regular UI facets
    for (val facet : AVAILABLE_FACETS) {
      val filterAgg = filter(facet).filter(aggFilter(filters, facet));
      val subAgg = isNestedField(facet) ? addNestedSubAggregations(filterAgg, facet, toRawFieldName(facet),
          TYPE_MODEL.getNestedPath(facet)) : addSubAggregations(filterAgg, facet, toRawFieldName(facet));

      result.add(global(facet).subAggregation(subAgg));
    }

    val repoFilters = buildRepoFilters(filters.deepCopy());
    val repoNameFieldName = toRawFieldName(Fields.REPO_NAME);

    /*
     * Facets that aren't visible in the UI. Special filtered cases (do not exclude self filtering):
     * repositoryNamesFiltered & repositorySize
     */
    // repositoryNamesFiltered
    val repoNameAggName = CustomAggregationFields.REPO_NAME;
    val filterAgg = filter(repoNameAggName).filter(repoFilters);
    val repoNameSubAgg = addNestedSubAggregations(filterAgg, repoNameAggName, repoNameFieldName, EsFields.FILE_COPIES);

    result.add(global(repoNameAggName).subAggregation(repoNameSubAgg));

    // repositorySize
    val repoSizeAggName = CustomAggregationFields.REPO_SIZE;
    val repoSizeTermsSubAgg = terms(repoSizeAggName).field(repoNameFieldName)
        .size(MAX_FACET_TERM_COUNT)
        .subAggregation(nestedAgg(CustomAggregationFields.DONOR, EsFields.DONORS,
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

  @NonNull
  private static FilterAggregationBuilder addSubAggregations(FilterAggregationBuilder builder,
      String facetName, String fieldName) {
    val termsAgg = terms(facetName).field(fieldName)
        .size(MAX_FACET_TERM_COUNT);
    val missingAgg = missing(MISSING).field(fieldName);

    return builder.subAggregation(termsAgg).subAggregation(missingAgg);
  }

  public static Map<String, TermFacet> convertAggregations2Facets(@NonNull Aggregations aggs) {
    val result = Maps.<String, TermFacet> newHashMap();

    for (val agg : aggs) {
      val name = agg.getName();

      if (name.equals(CustomAggregationFields.REPO_SIZE)) {
        val buckets = getAggregationBuckets(agg);

        result.put(CustomAggregationFields.REPO_SIZE, convertRepoSizeAggregation(buckets));
        result.put("repositoryDonors", convertRepoDonorAggregation(buckets));
      } else {
        result.put(name, convertNormalAggregation(agg));
      }
    }

    return result;
  }

  private static List<Bucket> getAggregationBuckets(Aggregation aggregation) {
    val name = aggregation.getName();
    val globalAgg = (Global) aggregation;
    val filterAgg = (Filter) globalAgg.getAggregations().get(name);
    val termAgg = (Terms) getSubAggResultFromNested(filterAgg.getAggregations(), name).get(name);

    return termAgg.getBuckets();
  }

  // Special aggregation to get unique donor count for each repository
  private static TermFacet convertRepoDonorAggregation(List<Bucket> buckets) {
    val terms = transform(buckets, bucket -> {
      final int size = bucketSize(getSubAggResultFromNested(bucket.getAggregations(), CustomAggregationFields.DONOR),
          CustomAggregationFields.DONOR);

      return new Term(bucket.getKey(), Long.valueOf(size));
    });

    val total = -1L; // Total does not have any meaning in this context because a donor can cross repositories
    return repoTermFacet(total, 0, ImmutableList.copyOf(terms));
  }

  // Special aggregation to get file size for each repository
  private static TermFacet convertRepoSizeAggregation(List<Bucket> buckets) {
    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : buckets) {
      val childCount = sumValue(bucket.getAggregations(), CustomAggregationFields.FILE_SIZE);

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

  @NonNull
  private SearchResponse searchRepositoryFiles(String indexType, String logMessage,
      Consumer<SearchRequestBuilder> customizer) {
    val request = client.prepareSearch(index).setTypes(indexType);
    customizer.accept(request);

    // FIXME: log.debug
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

  // FIXME: Move the CSV writing piece out to the service and leave the ES search here.
  // The CSV generation belongs to the service, not to the repository.
  @NonNull
  private StreamingOutput exportDataTable(Consumer<SearchRequestBuilder> queryCustomizer, String[] keys) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        @Cleanup
        val writer = new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(os)), TAB_PREFERENCE);
        writer.writeHeader(toStringArray(DATA_TABLE_EXPORT_MAP.values()));

        SearchResponse response = searchFileCentric("exportDataTable", queryCustomizer);

        while (true) {
          response = client.prepareSearchScroll(response.getScrollId())
              .setScroll(KEEP_ALIVE)
              .execute().actionGet();

          val finished = !hasHits(response);

          if (finished) {
            break;
          } else {
            for (val hit : response.getHits()) {
              writer.write(toRowValueMap(hit), keys);
            }
          }

        }

      }

    };
  }

  public StreamingOutput exportData(@NonNull Query query) {
    val size = 5000;
    final String[] keys = toStringArray(DATA_TABLE_EXPORT_MAP_FIELD_KEYS);
    val filters = buildRepoFilters(query.getFilters());

    return exportDataTable((request) -> request
        .setSearchType(SCAN)
        .setSize(size)
        .setScroll(KEEP_ALIVE)
        .setPostFilter(filters)
        .setQuery(MATCH_ALL_QUERY)
        .addFields(keys), keys);
  }

  // FIXME: Support terms lookup on files as part of the filter builder so we don't need an extra method.
  public StreamingOutput exportDataFromSet(@NonNull String setId) {
    val size = 5000;
    final String[] keys = toStringArray(DATA_TABLE_EXPORT_MAP_FIELD_KEYS);
    val lookupFilter = createTermsLookupFilter(toRawFieldName(Fields.FILE_UUID), FILE_IDS, UUID.fromString(setId));
    val query = new FilteredQueryBuilder(MATCH_ALL_QUERY, lookupFilter);

    return exportDataTable((request) -> request
        .setSearchType(SCAN)
        .setSize(size)
        .setScroll(KEEP_ALIVE)
        .setQuery(query)
        .addFields(keys), keys);
  }

  private static String combineUniqueItemsToString(SearchHitField hitField, Function<Set<Object>, String> combiner) {
    return (null == hitField) ? "" : combiner.apply(newHashSet(hitField.getValues()));
  }

  private static String toStringValue(SearchHitField hitField) {
    return combineUniqueItemsToString(hitField, COMMA_JOINER::join);
  }

  private static String toSummarizedString(SearchHitField hitField) {
    return combineUniqueItemsToString(hitField, RepositoryFileRepository::toSummarizedString);
  }

  private static String toSummarizedString(Set<Object> values) {
    if (isEmpty(values)) {
      return "";
    }

    val count = values.size();

    // Get the value if there is only one element; otherwise get the count or empty string if empty.
    return (count > 1) ? String.valueOf(count) : Iterables.get(values, 0).toString();
  }

  private static String toAverageSizeString(SearchHitField hitField) {
    if (null == hitField) {
      return "0";
    }

    val average = hitField.getValues().stream()
        .mapToLong(o -> parseLong(o.toString()))
        .average();

    return String.valueOf(average.orElse(0));
  }

  @NonNull
  private static Map<String, String> toRowValueMap(SearchHit hit) {
    val valueMap = hit.getFields();

    return ImmutableMap.<String, String> builder()
        .putAll(toMap(DATA_TABLE_EXPORT_SUMMARY_FIELDs,
            field -> toSummarizedString(valueMap.get(field))))
        .putAll(toMap(DATA_TABLE_EXPORT_AVERAGE_FIELDs,
            field -> toAverageSizeString(valueMap.get(field))))
        .putAll(toMap(DATA_TABLE_EXPORT_OTHER_FIELDs,
            field -> toStringValue(valueMap.get(field))))
        .build();
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
          .setPostFilter(filters);

      for (AggregationBuilder<?> agg : aggs(queryFilter)) {
        request.addAggregation(agg);
      }
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

    val search = client.prepareSearch(index)
        .setTypes(FILE_INDEX_TYPE)
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(JQL_FIELD_NAME_MAPPING.get(query.getSort()), query.getOrder())
        .setPostFilter(filters);

    log.info("findAll() - ES query is: '{}'.", search);
    val response = search.execute().actionGet();
    log.debug("findAll() - ES response is: '{}'.", response);

    return SearchResponses.getHitIds(response);
  }

  // FIXME: Maybe switch to using pql in the future?
  @NonNull
  public SearchResponse findDownloadInfoFromSet(String setId) {
    val lookupFilter = createTermsLookupFilter("id", FILE_IDS, UUID.fromString(setId));
    val query = new FilteredQueryBuilder(new MatchAllQueryBuilder(), lookupFilter);
    String[] includes = { "file_copies", "donors" };
    String[] excludes = {};
    val search = client.prepareSearch(index)
        .setTypes(REPOSITORY_FILE.getId())
        .setFrom(0)
        .setSize(20000)
        .setQuery(query)
        .setFetchSource(includes, excludes)
        .addFields("id", "file_id", "data_bundle.data_bundle_id");

    log.info("ES request is: {}", search);
    val response = search.execute().actionGet();
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
  private static String[] toStringArray(Iterable<String> strings) {
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
  public Map<String, Long> getSummary(Query query) {
    val donorSubAggs = nestedAgg(SummaryFields.DONOR, EsFields.DONORS,
        terms(SummaryFields.DONOR).size(100000).field(DONOR_ID_RAW_FIELD_NAME))
        .subAggregation(
            terms(SummaryFields.PROJECT).size(1000).field(toRawFieldName(Fields.PROJECT_CODE)))
        .subAggregation(
            terms(SummaryFields.PRIMARY_SITE).size(1000).field(toRawFieldName(Fields.PRIMARY_SITE)));

    val fileSizeSubAgg = averageFileSizePerFileCopyAgg(SummaryFields.FILE);

    val aggName = "summary";
    val filters = buildRepoFilters(query.getFilters());
    val subAgg = filter(aggName).filter(filters)
        .subAggregation(fileSizeSubAgg)
        .subAggregation(donorSubAggs);

    val response = searchFileCentric("Summary aggregation", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .setPostFilter(filters)
        .addAggregation(global(aggName).subAggregation(subAgg)));

    log.debug("ES aggregation result is: '{}'.", response);

    val aggResult = getAggregationResult(response, aggName);
    val buckets = ((Terms) aggResult.get(SummaryFields.FILE)).getBuckets();
    long totalFileSize = 0;

    for (val bucket : buckets) {
      totalFileSize += averageValue(getSubAggResultFromNested(bucket.getAggregations(), SummaryFields.FILE),
          SummaryFields.FILE);
    }

    val donorAggResult = getSubAggResultFromNested(aggResult, SummaryFields.DONOR);

    return ImmutableMap.<String, Long> builder()
        // FIXME: this fileCount might not be correct!
        .put("fileCount", getTotalHitCount(response))
        .put("totalFileSize", totalFileSize)
        .put("donorCount", (long) bucketSize(donorAggResult, SummaryFields.DONOR))
        .put("projectCount", (long) bucketSize(donorAggResult, SummaryFields.PROJECT))
        .put("primarySiteCount", (long) bucketSize(donorAggResult, SummaryFields.PRIMARY_SITE))
        .build();
  }

  /**
   * Returns the unique donor count across repositories Note we are counting the bucket size of a term aggregation. It
   * appears that using cardinality aggregation yields imprecise result.
   */
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

    log.debug("ES aggregation result is: '{}'.", response);

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

  @UtilityClass
  private class PanCancerStatsFields {

    private final String DONOR = "donor";
    private final String SIZE = "size";
    private final String FORMAT = "format";
    private final String DONOR_PRIMARY_SITE = "donorPrimarySite";

  }

  public Map<String, Map<String, Object>> getPancancerStats() {
    // Cardinality doesn't work, set size to large number to get accurate bucket (donor) count
    val donorCountAgg = nestedAgg(PanCancerStatsFields.DONOR, EsFields.DONORS,
        terms(PanCancerStatsFields.DONOR).field(DONOR_ID_RAW_FIELD_NAME).size(30000));

    val fileSizeAgg = averageFileSizePerFileCopyAgg(PanCancerStatsFields.SIZE);

    val fileFormatAgg = nestedAgg(PanCancerStatsFields.FORMAT, EsFields.FILE_COPIES,
        terms(PanCancerStatsFields.FORMAT).field(toRawFieldName(Fields.FILE_FORMAT)));

    val dataTypeAgg = terms(PCAWG).field(toRawFieldName(Fields.DATA_TYPE))
        .subAggregation(donorCountAgg)
        .subAggregation(fileSizeAgg)
        .subAggregation(fileFormatAgg);

    // Primary Site => Project Code => Donor ID
    val primarySiteAgg = primarySiteAgg(Fields.PRIMARY_SITE, 100)
        .subAggregation(primarySiteAgg(Fields.PROJECT_CODE, 100)
            .subAggregation(primarySiteAgg(Fields.DONOR_ID, 30000)));

    val statsAgg = filter(PCAWG).filter(termFilter(toRawFieldName(Fields.STUDY), PCAWG))
        .subAggregation(dataTypeAgg)
        .subAggregation(nestedAgg(PanCancerStatsFields.DONOR_PRIMARY_SITE, EsFields.DONORS, primarySiteAgg));

    val response = searchFileCentric("PCAWG getPancancerStats", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .addAggregation(statsAgg));

    log.debug("PCAWG getPancancerStats query result: '{}'.", response);

    return extractPancancerStats(response.getAggregations());
  }

  private static TermsBuilder primarySiteAgg(@NonNull String fieldAlias, int size) {
    return terms(PanCancerStatsFields.DONOR_PRIMARY_SITE)
        .field(toRawFieldName(fieldAlias))
        .size(size);
  }

  private static Map<String, Map<String, Object>> extractPancancerStats(Aggregations aggs) {
    val stats = (Filter) aggs.get(PCAWG);
    val statsAggregations = stats.getAggregations();

    val result = Maps.<String, Map<String, Object>> newHashMap();

    // donorPrimarySite
    val donorPrimarySite = Maps.<String, Object> newHashMap();
    val donorFacets = (Terms) getSubAggResultFromNested(statsAggregations, PanCancerStatsFields.DONOR_PRIMARY_SITE)
        .get(PanCancerStatsFields.DONOR_PRIMARY_SITE);

    for (val bucket : donorFacets.getBuckets()) {
      val name = bucket.getKey();

      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) donorPrimarySite.get(name);

      if (map == null) {
        map = Maps.<String, Object> newHashMap();
        donorPrimarySite.put(name, map);
      }

      val projectFacets = (Terms) bucket.getAggregations().get(PanCancerStatsFields.DONOR_PRIMARY_SITE);

      for (val projectBucket : projectFacets.getBuckets()) {
        val donorCount = bucketSize(projectBucket.getAggregations(), PanCancerStatsFields.DONOR_PRIMARY_SITE);

        map.put(projectBucket.getKey(), Long.valueOf(donorCount));
      }
    }

    result.put(PanCancerStatsFields.DONOR_PRIMARY_SITE, donorPrimarySite);

    // statistics
    val fileSizeAggName = PanCancerStatsFields.SIZE;
    val statistics = Maps.<String, Object> newHashMap();
    val datatypes = (Terms) statsAggregations.get(PCAWG);

    for (val bucket : datatypes.getBuckets()) {
      val bucketAggregations = bucket.getAggregations();
      val donorCount = bucketSize(getSubAggResultFromNested(bucketAggregations, PanCancerStatsFields.DONOR),
          PanCancerStatsFields.DONOR);

      val fileSizeResult = (Terms) bucketAggregations.get(fileSizeAggName);
      long totalFileSize = 0;

      for (val fileSizeBucket : fileSizeResult.getBuckets()) {
        totalFileSize += averageValue(getSubAggResultFromNested(fileSizeBucket.getAggregations(), fileSizeAggName),
            fileSizeAggName);
      }

      val dataFormat = (Terms) getSubAggResultFromNested(bucketAggregations, PanCancerStatsFields.FORMAT)
          .get(PanCancerStatsFields.FORMAT);
      val formats = transform(dataFormat.getBuckets(), b -> b.getKey());

      // TODO: We should use PanCancerStatsFields for these keys too, though it requires changes in the client side.
      val map = ImmutableMap.<String, Object> of(
          "fileCount", bucket.getDocCount(),
          "donorCount", Long.valueOf(donorCount),
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
