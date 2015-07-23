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
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
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
import static org.icgc.dcc.portal.model.IndexModel.IS;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.model.IndexModel.MISSING;
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
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvMapWriter;

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

  private static final List<String> FACETS = RepositoryFileTypeModel.AVAILABLE_FACETS;

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
    val fields = filters.path(KIND.getId()).fields();

    if (fields.hasNext() == false) {
      return matchAllFilter();
    }

    val termFilters = boolFilter();
    val nestedTerms = Maps.<String, List<String>> newHashMap();

    while (fields.hasNext()) {
      val facetField = fields.next();
      val facetFieldKey = facetField.getKey();

      if (!TYPE_MAPPING.containsKey(facetFieldKey)) {
        continue;
      }

      val fieldName = TYPE_MAPPING.get(facetFieldKey);

      // Assume "IS"
      FilterBuilder fb;
      val boolNode = facetField.getValue();
      val items = Lists.<String> newArrayList();

      for (val item : boolNode.get(IS)) {
        items.add(item.textValue());
      }

      if (nested && (fieldName.equals("data_types.data_type") || fieldName.equals("data_types.data_format"))) {
        nestedTerms.put(fieldName, items);
        continue;
      } else {
        val terms = termsFilter(fieldName, items);

        // Special processing for "no data" terms
        if (items.remove(MISSING)) {
          val missing = missingFilter(fieldName).existence(true).nullValue(true);
          fb = boolFilter().should(missing).should(terms);
        } else {
          fb = boolFilter().must(terms);
        }

      }
      termFilters.must(fb);
    }

    // Handle special case. Datatype and Dataformat, note these should never have missing values
    if (!nestedTerms.isEmpty()) {
      val nestedBoolFilter = boolFilter();

      for (val entry : nestedTerms.entrySet()) {
        nestedBoolFilter.must(termsFilter(entry.getKey(), entry.getValue()));
      }

      termFilters.must(nestedFilter("data_types", nestedBoolFilter));
    }

    return termFilters;
  }

  public List<AggregationBuilder<?>> aggs(ObjectNode filters) {
    val aggs = Lists.<AggregationBuilder<?>> newArrayList();

    // General case
    for (String facet : FACETS) {
      val globalAgg = global(facet);
      val facetAgg = filter(facet);

      // if (facet.equals("dataType") || facet.equals("dataFormat")) continue;
      val fieldName = TYPE_MAPPING.get(facet);

      if (filters.fieldNames().hasNext()) {
        val facetFilters = filters.deepCopy();

        // Remove one self
        if (facetFilters.has(KIND.getId())) {
          facetFilters.with(KIND.getId()).remove(facet);
        }
        log.info("Processing {}", fieldName);

        addSubAggregations(facetAgg, buildRepoFilters(facetFilters, false), facet, fieldName);
      } else {
        addSubAggregations(facetAgg, matchAllFilter(), facet, fieldName);
      }

      globalAgg.subAggregation(facetAgg);
      aggs.add(globalAgg);
    }

    // Special filtered case - reponames, do not exclude self filtering
    val field = TYPE_MAPPING.get("repoName");
    val repoFiltered = "repositoryNamesFiltered";
    val subAgg = addSubAggregations(filter(repoFiltered),
        buildRepoFilters(filters.deepCopy(), false), repoFiltered, field);
    aggs.add(global(repoFiltered)
        .subAggregation(subAgg));

    // Special filtered case - repo sizes and repo donors
    val repoSizeFitered = "repositorySizes";

    aggs.add(global(repoSizeFitered)
        .subAggregation(filter(repoSizeFitered).filter(buildRepoFilters(filters.deepCopy(), false))
            .subAggregation(terms(repoSizeFitered).size(MAX_FACET_TERM_COUNT).field(field)
                .subAggregation(terms("donor").size(100000).field("donor.donor_id"))
                .subAggregation(sum("fileSize").field("repository.file_size")))));

    return aggs;
  }

  @NonNull
  private static FilterAggregationBuilder addSubAggregations(FilterAggregationBuilder builder, FilterBuilder filter,
      String facetName, String fieldName) {
    return builder
        .filter(filter)
        .subAggregation(terms(facetName).size(MAX_FACET_TERM_COUNT).field(fieldName))
        .subAggregation(missing(MISSING).field(fieldName));
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
  @SuppressWarnings("unchecked")
  private Map<String, String> normalize(Map<String, Object> map) {
    val result = Maps.<String, String> newHashMap();

    for (val entry : map.entrySet()) {
      val key = entry.getKey();
      val value = entry.getValue();

      if (ARRAY_FIELDS.contains(key)) {
        result.put(key, COMMA.join((List<String>) value));
      } else if (key.equals("repository.file_size")) {
        result.put(key, getLong(value).toString());
      } else {
        result.put(key, getString(value));
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

    log.info("findAll() - ES query is: '{}'.", search);
    val response = search.execute().actionGet();
    log.debug("findAll() - ES response is: '{}'.", response);

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
    val fieldNames = transform(fields, fieldName -> DONOR_SEARCH_FIELDS_OF_EXACT_MATCH.contains(fieldName) ? fieldName
        + DONOR_SEARCH_EXACT_MATCH_SUFFIX : fieldName);
    // Adds '.analyzed' to field names for fields that need partial match.
    val names = transform(fieldNames, fieldName -> DONOR_SEARCH_FIELDS_OF_PARTIAL_MATCH.contains(fieldName) ? fieldName
        + DONOR_SEARCH_PARTIAL_MATCH_SUFFIX : fieldName);

    val search = client.prepareSearch(index)
        .setTypes(FILE_DONOR_TEXT_INDEX_TYPE)
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(maxNumberOfDocs)
        .setQuery(multiMatchQuery(queryString, toStringArray(names)));

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
            .subAggregation(terms("donor").size(100000).field("donor.donor_id")) // Get unique donors
            .subAggregation(terms("project").size(1000).field("donor.project_code")) // Get unique projects
            .subAggregation(terms("primarySite").size(1000).field("donor.primary_site")));

    search.addAggregation(summaryAgg);
    log.info("Summary {}", search);
    val response = search.execute().actionGet();

    val result = Maps.<String, Long> newHashMap();

    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) global.getAggregations().get(name);

    result.put("fileCount", response.getHits().getTotalHits());
    result.put("donorCount", (long) ((Terms) filter.getAggregations().get("donor")).getBuckets().size());
    result.put("projectCount", (long) ((Terms) filter.getAggregations().get("project")).getBuckets().size());
    result.put("primarySiteCount", (long) ((Terms) filter.getAggregations().get("primarySite")).getBuckets().size());
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

    val response = search.execute().actionGet();
    val global = (Global) response.getAggregations().get(name);
    val filter = (Filter) global.getAggregations().get(name);
    return ((Terms) filter.getAggregations().get(name)).getBuckets().size();
  }

  private final String PCAWG = "PCAWG";

  public Map<String, Map<String, Object>> getPancancerStats() {
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0);

    val stats = AggregationBuilders.filter(PCAWG).filter(FilterBuilders.termFilter("study", PCAWG));
    val datatypeAgg = AggregationBuilders.terms(PCAWG).field("data_type.data_type");

    // Cardinality doesn't work, set size to large number to get accurate bucket (donor) count
    val donorCountAgg = AggregationBuilders.terms("donor").field("donor.donor_id").size(30000);
    val fileSizeAgg = AggregationBuilders.sum("size").field("repository.file_size");
    val dataFormatAgg = AggregationBuilders.terms("format").field("data_type.data_format");

    // Primary Site => Project Code => Donor #
    stats.subAggregation(datatypeAgg
        .subAggregation(donorCountAgg)
        .subAggregation(fileSizeAgg)
        .subAggregation(dataFormatAgg))
        .subAggregation(AggregationBuilders.terms("donorPrimarySite").field("donor.primary_site").size(100)
            .subAggregation(AggregationBuilders.terms("donorPrimarySite").field("donor.project_code").size(100)
                .subAggregation(AggregationBuilders.terms("donorPrimarySite").field("donor.donor_id").size(30000))));

    search.addAggregation(stats);

    log.info("PCAWG {}", search);
    val response = search.execute().actionGet();
    return extractPancancerStats(response.getAggregations());
  }

  private Map<String, Map<String, Object>> extractPancancerStats(Aggregations aggs) {
    val stats = (Filter) aggs.get(PCAWG);
    val datatypes = (Terms) stats.getAggregations().get(PCAWG);
    val donorFacets = (Terms) stats.getAggregations().get("donorPrimarySite");
    val result = Maps.<String, Map<String, Object>> newHashMap();

    Map<String, Object> donorPrimarySite = Maps.<String, Object> newHashMap();
    for (val bucket : donorFacets.getBuckets()) {
      val name = bucket.getKey();

      if (donorPrimarySite.get(name) == null) {
        donorPrimarySite.put(name, Maps.<String, Object> newHashMap());
      }

      val projectFacets = (Terms) bucket.getAggregations().get("donorPrimarySite");
      @SuppressWarnings("unchecked")
      val map = (Map<String, Object>) donorPrimarySite.get(name);
      for (val projectBucket : projectFacets.getBuckets()) {
        val donorCount = ((Terms) projectBucket.getAggregations().get("donorPrimarySite")).getBuckets().size();
        map.put(projectBucket.getKey(), (long) donorCount);
      }

    }

    Map<String, Object> statistics = Maps.<String, Object> newHashMap();
    for (val bucket : datatypes.getBuckets()) {
      val name = bucket.getKey();
      val donorCount = ((Terms) bucket.getAggregations().get("donor")).getBuckets().size();
      val fileSize = ((Sum) bucket.getAggregations().get("size")).getValue();
      val dataFormat = (Terms) bucket.getAggregations().get("format");
      val fileCount = bucket.getDocCount();

      val formats = Lists.<String> newArrayList();
      for (val f : dataFormat.getBuckets()) {
        formats.add(f.getKey());
      }
      val map = ImmutableMap.<String, Object> of(
          "fileCount", fileCount,
          "donorCount", (long) donorCount,
          "fileSize", (long) fileSize,
          "dataFormat", formats);
      statistics.put(name, map);
    }
    result.put("donorPrimarySite", donorPrimarySite);
    result.put("stats", statistics);
    log.debug("Result {}", result);

    return result;
  }

  @SneakyThrows
  public Map<String, String> getIndexMetaData() {
    val state = client.admin().cluster().prepareState().setIndices(index).execute().actionGet().getState();

    val realIndex = (state.getMetaData().getAliases().get(index)).iterator().next().key;

    IndexMetaData indexMetaData = state.getMetaData().index(realIndex);

    MappingMetaData mappingMetaData = indexMetaData.getMappings().values().iterator().next().value;
    Map<String, Object> source = mappingMetaData.sourceAsMap();
    @SuppressWarnings("unchecked")
    Map<String, String> meta = (Map<String, String>) source.get("_meta");
    if (meta == null) return Maps.newHashMap();
    return meta;

  }
}
