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

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.resolveSourceFields;
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.util.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
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
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvMapWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

@Slf4j
@Component
public class RepositoryFileRepository {

  private final String INDEX_NAME = "icgc-repository";

  // private static final Type TYPE = Type.RELEASE;
  private static final Kind KIND = Kind.REPOSITORY_FILE;
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  private static final ImmutableList<String> FACETS = ImmutableList.of("study", "dataType", "dataFormat", "access",
      "projectCode", "primarySite", "donorStudy", "repositoryNames");

  private final ImmutableMap<String, String> EXPORT_FIELDS = ImmutableMap.<String, String> builder()
      .put("access", "Access")
      .put("repository.file_name", "File name")
      .put("donor.donor_id", "ICGC Donor")
      .put("repository.repo_server.repo_name", "Repository")
      .put("donor.project_code", "Project")
      .put("study", "Study")
      .put("data_types.data_type", "Data type")
      .put("data_types.data_format", "Format")
      .put("repository.file_size", "Size")
      .build();

  private final ImmutableList<String> ARRAY_FIELDS = ImmutableList.of("data_types.data_type", "data_types.data_format",
      "repository.repo_server.repo_name");

  private final Client client;
  private final String index;

  @Autowired
  public RepositoryFileRepository(Client client) {
    this.index = INDEX_NAME;
    this.client = client;
  }

  /**
   * FIXME: This is a temporary solution. We really should use the PQL infrastructure to build. <br>
   * Negation is not supported <br>
   * _missing is not supported for data_types.datatype and data_type.dataformat <br>
   */
  public static FilterBuilder buildRepoFilters(ObjectNode filters, boolean nested) {
    val termFilters = FilterBuilders.boolFilter();
    val fields = filters.path(KIND.getId()).fields();
    val typeMapping = FIELDS_MAPPING.get(KIND);

    Map<String, List<String>> nestedTerms = Maps.<String, List<String>> newHashMap();

    if (fields.hasNext() == false) return FilterBuilders.matchAllFilter();
    while (fields.hasNext()) {
      val facetField = fields.next();

      if (!typeMapping.containsKey(facetField.getKey())) continue;
      String fieldName = typeMapping.get(facetField.getKey());

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
    val typeMapping = FIELDS_MAPPING.get(KIND);

    // General case
    for (String facet : FACETS) {
      val globalAgg = AggregationBuilders.global(facet);
      val facetAgg = AggregationBuilders.filter(facet);
      // if (facet.equals("dataType") || facet.equals("dataFormat")) continue;
      String fieldName = typeMapping.get(facet);

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
    val repoFiltered = "repositoryNamesFiltered";
    val field = typeMapping.get("repositoryNames");
    aggs.add(global(repoFiltered)
        .subAggregation(filter(repoFiltered)
            .filter(buildRepoFilters(filters.deepCopy(), false))
            .subAggregation(terms(repoFiltered).size(1024).field(field))
            .subAggregation(missing("_missing").field(field))));

    // Special nested case - Disabled as currently there are no nested fields
    // for (String facet : FACETS) {
    // String fieldName = typeMapping.get(facet);
    // if (facet.equals("dataType") || facet.equals("dataFormat")) {
    // // Remove one self
    // val facetFilters = filters.deepCopy();
    // if (facetFilters.has(KIND.getId())) {
    // facetFilters.with(KIND.getId()).remove(facet);
    // }
    // val globalAgg = AggregationBuilders.global(facet);
    // val filterAgg = AggregationBuilders.filter(facet);
    // filterAgg.filter(buildRepoFilters(facetFilters, false));
    // val nestedAgg = AggregationBuilders.nested(facet).path("data_types");
    // val termAgg = AggregationBuilders.terms(facet).size(1024).field(fieldName);
    // val reverseAgg = AggregationBuilders.reverseNested(facet);
    //
    // termAgg.subAggregation(reverseAgg);
    //
    // nestedAgg.subAggregation(termAgg);
    //
    // filterAgg.subAggregation(nestedAgg);
    //
    // globalAgg.subAggregation(filterAgg);
    // aggs.add(globalAgg);
    // }
    // }

    return aggs;
  }

  /**
   * FIXME: This is just temporary until PQL is in place, it does just enough to work.
   */
  public Map<String, TermFacet> convertAggregations2Facets(Aggregations aggs) {
    Map<String, TermFacet> result = Maps.<String, TermFacet> newHashMap();
    for (Aggregation agg : aggs) {
      val name = agg.getName();

      // Disabled as there are no nested fields at the moment
      // if (name.equals("dataFormat") || name.equals("dataType")) {
      // result.put(name, convertNestedAggregation(agg));
      // } else {
      // result.put(name, convertNormalAggregation(agg));
      // }
      result.put(name, convertNormalAggregation(agg));
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

      int count = (int) reverseNestedAgg.getDocCount();
      total += count;
      termsBuilder.add(new Term(bucket.getKey(), count));
    }
    log.info("");

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

      int count = (int) bucket.getDocCount(); // FIXME: this is long to int
      total += count;
      termsBuilder.add(new Term(bucket.getKey(), count));
    }
    log.info("{} {}", "Missng", missingAgg.getDocCount());
    if (missingAgg.getDocCount() > 0) {
      termsBuilder.add(new Term("_missing", (int) missingAgg.getDocCount()));
    }
    log.info("");

    return TermFacet.repoTermFacet(total, missingAgg.getDocCount(), termsBuilder.build());
  }

  public StreamingOutput exportData(Query query) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        val filters = buildRepoFilters(query.getFilters(), false);
        String headers[] = EXPORT_FIELDS.values().toArray(new String[EXPORT_FIELDS.values().size()]);
        String keys[] = EXPORT_FIELDS.keySet().toArray(new String[EXPORT_FIELDS.keySet().size()]);

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
            .addFields(EXPORT_FIELDS.keySet().toArray(new String[EXPORT_FIELDS.keySet().size()]));

        SearchResponse response = search.execute().actionGet();
        while (true) {
          response = client.prepareSearchScroll(response.getScrollId())
              .setScroll(KEEP_ALIVE)
              .execute().actionGet();

          val finished = !hasHits(response);

          if (finished) {
            break;
          } else {

            for (SearchHit hit : response.getHits()) {
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
        result.put(key, Joiner.on(StringUtils.COMMA).join((List<String>) map.get(key)));
      } else if (key.equals("repository.file_size")) {
        result.put(key, getLong(map.get(key)).toString());
      } else {
        result.put(key, getString(map.get(key)));
      }
    }
    return result;
  }

  public Map<String, Object> findOne(String id, Query query) {
    val kind = Kind.REPOSITORY_FILE;
    val search = client.prepareGet(index, "file", id);

    search.setFields(getFields(query, kind));
    String[] sourceFields = resolveSourceFields(query, KIND);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, KIND), EMPTY_SOURCE_FIELDS);
    }

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val map = createResponseMap(response, query, KIND);
    return map;
  }

  public SearchResponse findAll(Query query) {
    val kind = Kind.REPOSITORY_FILE;

    val filters = buildRepoFilters(query.getFilters(), false);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELDS_MAPPING.get(KIND).get(query.getSort()), query.getOrder());

    search.setPostFilter(filters);

    search.addFields(getFields(query, KIND));
    String[] sourceFields = resolveSourceFields(query, kind);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, kind), EMPTY_SOURCE_FIELDS);
    }

    val aggs = this.aggs(query.getFilters());
    for (val agg : aggs) {
      search.addAggregation(agg);
    }

    log.info(" !!! {}", search);
    val response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  @NonNull
  public SearchResponse findDownloadInfo(Query query, final String[] fields, String sortField, String sourceField) {
    // Get the total count first.
    query.setLimit(0);
    val response = findDownloadInfo(COUNT, query, fields, sortField, sourceField);
    val count = getTotalHitCount(response);
    log.info("A total of {} files for this query.", count);

    // Run the query to retrieve.
    query.setLimit(Ints.saturatedCast(count));
    return findDownloadInfo(QUERY_THEN_FETCH, query, fields, sortField, sourceField);
  }

  @NonNull
  private SearchResponse findDownloadInfo(SearchType searchType, Query query, final String[] fields, String sortField,
      String sourceField) {
    val filters = buildRepoFilters(query.getFilters(), false);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(searchType)
        .setFrom(query.getFrom())
        .setSize(query.getLimit())
        .addSort(sortField, SortOrder.ASC)
        .setPostFilter(filters)
        .addFields(fields)
        // Need to use _source because 'repository.repo_server' is an array.
        .setFetchSource(sourceField, null);

    log.info("ES request is: {}", search);
    val response = search.execute().actionGet();
    log.debug("ES response is: {}", response);

    return response;
  }

  /**
   * @param queryStr - either matching donor
   * @return
   */
  public SearchResponse findRepoDonor(String queryStr) {

    val typeMapping = FIELDS_MAPPING.get(KIND);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(1);

    val donorIdFilter = FilterBuilders.termFilter(typeMapping.get("donorId"), queryStr);
    val specimenIdFilter = FilterBuilders.termFilter(typeMapping.get("specimenId"), queryStr);
    val sampleIdFilter = FilterBuilders.termFilter(typeMapping.get("sampleId"), queryStr);
    val sampleSubmitterIdFilter = FilterBuilders.termFilter(typeMapping.get("sampleSubmitterId"), queryStr);
    val specimenSubmitterIdFilter = FilterBuilders.termFilter(typeMapping.get("specimenSubmitterId"), queryStr);
    val TCGAAliquotBarcode = FilterBuilders.termFilter(typeMapping.get("TCGAAliquotBarcode"), queryStr);
    val TCGASampleBarcode = FilterBuilders.termFilter(typeMapping.get("TCGASampleBarcode"), queryStr);

    val postFilter =
        FilterBuilders.boolFilter()
            .should(donorIdFilter)
            .should(specimenIdFilter)
            .should(sampleIdFilter)
            .should(sampleSubmitterIdFilter)
            .should(specimenSubmitterIdFilter)
            .should(TCGASampleBarcode)
            .should(TCGAAliquotBarcode);

    search.setPostFilter(postFilter);
    log.info(">>> {}", search);
    return search.execute().actionGet();
  }
}
