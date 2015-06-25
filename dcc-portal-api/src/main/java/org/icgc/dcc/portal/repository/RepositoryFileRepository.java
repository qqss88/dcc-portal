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

import static com.google.common.collect.Iterables.transform;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.sortBuilder;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.REPOSITORY_INDEX_NAME;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.SearchResponses.getTotalHitCount;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvMapWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

@Slf4j
@Component
public class RepositoryFileRepository {

  private static final String DONOR_SEARCH_EXACT_MATCH_SUFFIX = ".raw";
  private static final List<String> DONOR_SEARCH_FIELDS_OF_EXACT_MATCH = ImmutableList.of(
      "specimen_id",
      "sample_id",
      "submitted_specimen_id",
      "submitted_sample_id");
  private static final String DONOR_SEARCH_PARTIAL_MATCH_SUFFIX = ".analyzed";
  private static final List<String> DONOR_SEARCH_FIELDS_OF_PARTIAL_MATCH = ImmutableList.of(
      "tcga_participant_barcode",
      "tcga_sample_barcode",
      "tcga_aliquot_barcode");
  private static final ImmutableList<String> MANIFEST_DOWNLOAD_INFO_FIELDS = RepositoryFileTypeModel.toAliasList(
      Fields.REPO_TYPE,
      Fields.REPO_ENTITY_ID,
      Fields.REPO_DATA_PATH,
      Fields.REPO_SERVER_OBJECT,
      Fields.FILE_NAME,
      Fields.FILE_SIZE,
      Fields.FILE_MD5SUM);
  private static final String MANIFEST_DOWNLOAD_INFO_SORT_FIELD = Fields.REPO_TYPE.getAlias();

  // private static final Type TYPE = Type.RELEASE;
  private static final Kind KIND = Kind.REPOSITORY_FILE;
  private static final Map<String, String> TYPE_MAPPING = FIELDS_MAPPING.get(KIND);
  private static final String FILE_INDEX_TYPE = Type.REPOSITORY_FILE.getId();
  private static final String FILE_DONOR_TEXT_INDEX_TYPE = Type.REPOSITORY_FILE_DONOR_TEXT.getId();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  private static final ImmutableList<String> FACETS = ImmutableList.of("study", "dataType", "dataFormat", "access",
      "projectCode", "primarySite", "donorStudy", "repositoryNames", "experimentalStrategy");

  private final ImmutableMap<String, String> EXPORT_FIELDS = ImmutableMap.<String, String> builder()
      .put("access", "Access")
      .put("repository.file_name", "File name")
      .put("donor.donor_id", "ICGC Donor")
      .put("repository.repo_server.repo_name", "Repository")
      .put("donor.project_code", "Project")
      .put("study", "Study")
      .put("data_type.data_type", "Data type")
      .put("data_type.data_format", "Format")
      .put("repository.file_size", "Size")
      .build();
  private final ImmutableList<String> ARRAY_FIELDS = ImmutableList.of(
      "data_type.data_type",
      "data_type.data_format",
      "repository.repo_server.repo_name");

  private final Client client;
  private final String index;
  private final QueryEngine queryEngine;

  @Autowired
  public RepositoryFileRepository(Client client) {
    this.index = REPOSITORY_INDEX_NAME;
    this.client = client;
    this.queryEngine = new QueryEngine(client, index);
  }

  /**
   * FIXME: This is a temporary solution. We really should use the PQL infrastructure to build. <br>
   * Negation is not supported <br>
   * _missing is not supported for data_types.datatype and data_type.dataformat <br>
   */
  public static FilterBuilder buildRepoFilters(ObjectNode filters, boolean nested) {
    val termFilters = FilterBuilders.boolFilter();
    val fields = filters.path(KIND.getId()).fields();

    val nestedTerms = Maps.<String, List<String>> newHashMap();

    if (fields.hasNext() == false) return FilterBuilders.matchAllFilter();
    while (fields.hasNext()) {
      val facetField = fields.next();

      if (!TYPE_MAPPING.containsKey(facetField.getKey())) {
        continue;
      }

      val fieldName = TYPE_MAPPING.get(facetField.getKey());

      // Assume "IS"
      JsonNode boolNode = facetField.getValue();
      FilterBuilder fb;
      val items = Lists.<String> newArrayList();
      for (val item : boolNode.get("is")) {
        items.add(item.textValue());
      }

      if (nested && (fieldName.equals("data_types.data_type") || fieldName.equals("data_types.data_format"))) {
        nestedTerms.put(fieldName, items);
        continue;
      } else {
        val terms = termsFilter(fieldName, items);

        // Special processing for "no data" terms
        if (items.remove(IndexModel.MISSING)) {
          val missing = missingFilter(fieldName).existence(true).nullValue(true);
          fb = FilterBuilders.boolFilter().should(missing).should(terms);
        } else {
          fb = FilterBuilders.boolFilter().must(terms);
        }

      }
      termFilters.must(fb);
    }

    // Handle special case. Datatype and Dataformat, note these should never have missing values
    if (!nestedTerms.isEmpty()) {
      val nestedBoolFilter = FilterBuilders.boolFilter();
      for (String fieldName : nestedTerms.keySet()) {
        nestedBoolFilter.must(termsFilter(fieldName, nestedTerms.get(fieldName)));
      }
      termFilters.must(nestedFilter("data_types", nestedBoolFilter));
    }

    return termFilters;
  }

  public List<AggregationBuilder> aggs(ObjectNode filters) {
    val aggs = Lists.<AggregationBuilder> newArrayList();

    // General case
    for (String facet : FACETS) {
      val globalAgg = AggregationBuilders.global(facet);
      val facetAgg = AggregationBuilders.filter(facet);
      // if (facet.equals("dataType") || facet.equals("dataFormat")) continue;
      val fieldName = TYPE_MAPPING.get(facet);

      if (filters.fieldNames().hasNext()) {
        val facetFilters = filters.deepCopy();

        // Remove one self
        if (facetFilters.has(KIND.getId())) {
          facetFilters.with(KIND.getId()).remove(facet);
        }
        log.info("Processing {}", fieldName);
        facetAgg.filter(buildRepoFilters(facetFilters, false));
        facetAgg.subAggregation(AggregationBuilders.terms(facet).size(1024).field(fieldName));
        facetAgg.subAggregation(AggregationBuilders.missing("_missing").field(fieldName));
      } else {
        facetAgg.filter(FilterBuilders.matchAllFilter());
        facetAgg.subAggregation(AggregationBuilders.terms(facet).size(1024).field(fieldName));
        facetAgg.subAggregation(AggregationBuilders.missing("_missing").field(fieldName));
      }
      globalAgg.subAggregation(facetAgg);
      aggs.add(globalAgg);
    }

    // Special filtered case - reponames, do not exclude self filtering
    val field = TYPE_MAPPING.get("repositoryNames");
    val repoFiltered = "repositoryNamesFiltered";
    aggs.add(global(repoFiltered)
        .subAggregation(filter(repoFiltered)
            .filter(buildRepoFilters(filters.deepCopy(), false))
            .subAggregation(terms(repoFiltered).size(1024).field(field))
            .subAggregation(missing("_missing").field(field))));

    // Special filtered case - repo sizes and repo donors
    val repoSizeFitered = "repositorySizes";
    aggs.add(global(repoSizeFitered)
        .subAggregation(filter(repoSizeFitered).filter(buildRepoFilters(filters.deepCopy(), false))
            .subAggregation(terms(repoSizeFitered).size(1024).field(field)
                .subAggregation(terms("donor").size(100000).field("donor.donor_id"))
                .subAggregation(sum("fileSize").field("repository.file_size")))));

    return aggs;
  }

  /**
   * FIXME: This is just temporary until PQL is in place, it does just enough to work.
   */
  public Map<String, TermFacet> convertAggregations2Facets(Aggregations aggs) {
    Map<String, TermFacet> result = Maps.<String, TermFacet> newHashMap();
    for (Aggregation agg : aggs) {
      val name = agg.getName();

      if (name.equals("repositorySizes")) {
        result.put("repositorySizes", convertRepoSizeAggregation(agg));
        result.put("repositoryDonors", convertRepoDonorAggregation(agg));
      } else {
        result.put(name, convertNormalAggregation(agg));
      }
    }
    return result;
  }

  // FIXME: Temporary code
  private TermFacet convertNestedAggregation(Aggregation agg) {
    val name = agg.getName();
    Global globalAgg = (Global) agg;
    Filter filterAgg = (Filter) globalAgg.getAggregations().get(name);
    Nested nestedAgg = (Nested) filterAgg.getAggregations().get(name);
    Terms termAgg = (Terms) nestedAgg.getAggregations().get(name);

    val termsBuilder = new ImmutableList.Builder<Term>();

    log.info("Nested Facet {}", name);
    long total = 0;
    for (val bucket : termAgg.getBuckets()) {
      ReverseNested reverseNestedAgg = (ReverseNested) bucket.getAggregations().get(name);
      log.info("{} {}", bucket.getKey(), reverseNestedAgg.getDocCount());

      long count = (int) reverseNestedAgg.getDocCount();
      total += count;
      termsBuilder.add(new Term(bucket.getKey(), count));
    }
    log.info("");

    return TermFacet.repoTermFacet(total, 0, termsBuilder.build());
  }

  // FIXME: Temporary code
  // Special aggregation to get unique donor count for each repository
  private TermFacet convertRepoDonorAggregation(Aggregation agg) {
    val name = agg.getName();
    Global globalAgg = (Global) agg;
    Filter filterAgg = (Filter) globalAgg.getAggregations().get(name);
    Terms termAgg = (Terms) filterAgg.getAggregations().get(name);

    val termsBuilder = new ImmutableList.Builder<Term>();
    long total = -1; // Total does not have any meaning in this context because a donor an cross repositories
    for (val bucket : termAgg.getBuckets()) {
      val child = (Terms) bucket.getAggregations().get("donor");
      termsBuilder.add(new Term(bucket.getKey(), (long) child.getBuckets().size()));
    }
    return TermFacet.repoTermFacet(total, 0, termsBuilder.build());
  }

  // FIXME: Temporary code
  // Special aggregation to get file size for each repository
  private TermFacet convertRepoSizeAggregation(Aggregation agg) {
    val name = agg.getName();
    Global globalAgg = (Global) agg;
    Filter filterAgg = (Filter) globalAgg.getAggregations().get(name);
    Terms termAgg = (Terms) filterAgg.getAggregations().get(name);

    val termsBuilder = new ImmutableList.Builder<Term>();
    long total = 0;
    for (val bucket : termAgg.getBuckets()) {
      val child = (Sum) bucket.getAggregations().get("fileSize");
      termsBuilder.add(new Term(bucket.getKey(), (long) child.getValue()));
      total += (long) child.getValue();
    }

    return TermFacet.repoTermFacet(total, 0, termsBuilder.build());
  }

  // FIXME: Temporary code
  private TermFacet convertNormalAggregation(Aggregation agg) {
    val name = agg.getName();
    Global globalAgg = (Global) agg;
    Filter filterAgg = (Filter) globalAgg.getAggregations().get(name);
    Terms termAgg = (Terms) filterAgg.getAggregations().get(name);
    Missing missingAgg = (Missing) filterAgg.getAggregations().get("_missing");

    val termsBuilder = new ImmutableList.Builder<Term>();

    log.info("Normal Facet {}", name);
    long total = 0;
    for (val bucket : termAgg.getBuckets()) {
      log.info("{} {}", bucket.getKey(), bucket.getDocCount());

      long count = bucket.getDocCount(); // FIXME: this is long to int
      total += count;
      termsBuilder.add(new Term(bucket.getKey(), count));
    }
    log.info("{} {}", "Missng", missingAgg.getDocCount());
    if (missingAgg.getDocCount() > 0) {
      termsBuilder.add(new Term("_missing", missingAgg.getDocCount()));
    }
    log.info("");

    return TermFacet.repoTermFacet(total, missingAgg.getDocCount(), termsBuilder.build());
  }

  public StreamingOutput exportData(Query query) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        val filters = buildRepoFilters(query.getFilters(), false);
        final String headers[] = toStringArray(EXPORT_FIELDS.values());
        final String keys[] = toStringArray(EXPORT_FIELDS.keySet());

        @Cleanup
        val writer = new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(os)), TAB_PREFERENCE);
        writer.writeHeader(headers);

        val search = client
            .prepareSearch(index)
            .setTypes(KIND.getId())
            .setSearchType(SCAN)
            .setSize(5000)
            .setScroll(KEEP_ALIVE)
            .setPostFilter(filters)
            .setQuery(matchAllQuery())
            .addFields(keys);

        SearchResponse response = search.execute().actionGet();
        while (true) {
          response = client.prepareSearchScroll(response.getScrollId())
              .setScroll(KEEP_ALIVE)
              .execute().actionGet();

          val finished = !hasHits(response);

          if (finished) {
            break;
          } else {
            for (val hit : response.getHits()) {
              val map = normalize(createResponseMap(hit, query, Kind.REPOSITORY_FILE));
              writer.write(map, keys);
            }
          }

        }

      }

    };
  }

  /**
   * Untangle array/list objects, numeric objects
   */
  private Map<String, String> normalize(Map<String, Object> map) {
    val result = Maps.<String, String> newHashMap();
    for (val key : map.keySet()) {
      if (ARRAY_FIELDS.contains(key)) {
        result.put(key, COMMA.join((List<String>) map.get(key)));
      } else if (key.equals("repository.file_size")) {
        result.put(key, getLong(map.get(key)).toString());
      } else {
        result.put(key, getString(map.get(key)));
      }
    }
    return result;
  }

  public GetResponse findOne(String id) {
    val search = client.prepareGet(index, FILE_INDEX_TYPE, id);
    val response = search.execute().actionGet();
    // This check is important as it validates if there is any document at all in the GET response.
    checkResponseState(id, response, KIND);

    return response;
  }

  public SearchResponse findAll(Query query) {
    val filters = buildRepoFilters(query.getFilters(), false);
    val search = client.prepareSearch(index)
        .setTypes(FILE_INDEX_TYPE)
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(TYPE_MAPPING.get(query.getSort()), query.getOrder())
        .setPostFilter(filters);

    val aggs = this.aggs(query.getFilters());
    for (val agg : aggs) {
      search.addAggregation(agg);
    }

    log.info("ES search query is: '{}'.", search);
    val response = search.execute().actionGet();
    log.debug("ES search response is: '{}'.", response);

    return response;
  }

  @NonNull
  public SearchResponse findDownloadInfo(String pql) {
    val pqlAst = parse(pql);
    pqlAst.setSelect(select(MANIFEST_DOWNLOAD_INFO_FIELDS));
    pqlAst.setSort(sortBuilder()
        .sortAsc(MANIFEST_DOWNLOAD_INFO_SORT_FIELD).build());

    val newPql = pqlAst.toString();
    log.info("Updated PQL is: '{}'.", newPql);

    // Get the total count first.
    val count = getTotalHitCount(findDownloadInfo(newPql, COUNT, 0));
    log.info("A total of {} files for this query.", count);

    return findDownloadInfo(newPql, QUERY_THEN_FETCH, Ints.saturatedCast(count));
  }

  @NonNull
  private SearchResponse findDownloadInfo(String pql, SearchType searchType, int size) {
    val search = queryEngine.execute(pql, REPOSITORY_FILE).getRequestBuilder()
        .setSearchType(searchType)
        .setSize(size);

    log.info("ES request is: {}", search);
    val response = search.execute().actionGet();
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

    // Due to the mapping of the file-donor-text type, we need to add certain suffixes to field names.
    // Adds '.raw' to field names for fields that need exact match.
    val fieldNames = transform(fields, fieldName ->
        DONOR_SEARCH_FIELDS_OF_EXACT_MATCH.contains(fieldName) ?
            fieldName + DONOR_SEARCH_EXACT_MATCH_SUFFIX : fieldName);
    // Adds '.analyzed' to field names for fields that need partial match.
    val names = transform(fieldNames, fieldName ->
        DONOR_SEARCH_FIELDS_OF_PARTIAL_MATCH.contains(fieldName) ?
            fieldName + DONOR_SEARCH_PARTIAL_MATCH_SUFFIX : fieldName);

    val search = client.prepareSearch(index)
        .setTypes(FILE_DONOR_TEXT_INDEX_TYPE)
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(maxNumberOfDocs)
        .setQuery(multiMatchQuery(queryString, toStringArray(names)));
    log.info(">>> ES Search is: {}", search);

    return search.execute().actionGet();
  }

  /**
   * Get total file size, total donor count and total number of files based on query
   */
  public Map<String, Long> getSummary(Query query) {
    val filters = buildRepoFilters(query.getFilters(), false);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .setPostFilter(filters);

    val name = "summary";
    val summaryAgg = global(name)
        .subAggregation(filter(name).filter(buildRepoFilters(query.getFilters(), false))
            .subAggregation(sum("file").field("repository.file_size"))
            .subAggregation(terms("donor").size(100000).field("donor.donor_id")));

    search.addAggregation(summaryAgg);
    val response = search.execute().actionGet();

    val result = Maps.<String, Long> newHashMap();

    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) global.getAggregations().get(name);

    result.put("fileCount", response.getHits().getTotalHits());
    result.put("donorCount", (long) ((Terms) filter.getAggregations().get("donor")).getBuckets().size());
    result.put("totalFileSize", (long) ((Sum) filter.getAggregations().get("file")).getValue());

    return result;
  }

  /**
   * Returns the unique donor count across repositories Note we are counting the bucket size of a term aggregation. It
   * appears that using cardinality aggregation yields imprecise result.
   */
  public long getDonorCount(Query query) {
    val filters = buildRepoFilters(query.getFilters(), false);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .setPostFilter(filters);

    val name = "donorCount";
    val donorCountAgg = global(name)
        .subAggregation(filter(name).filter(buildRepoFilters(query.getFilters(), false))
            .subAggregation(terms(name).size(100000).field("donor.donor_id")));

    search.addAggregation(donorCountAgg);

    log.info(">>> {}", search);
    val response = search.execute().actionGet();
    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) global.getAggregations().get(name);
    return ((Terms) filter.getAggregations().get(name)).getBuckets().size();
  }
}
