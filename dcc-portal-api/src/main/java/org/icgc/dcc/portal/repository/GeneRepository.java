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

import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.search.facet.FacetBuilders.termsFacet;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.Filters.andFilter;
import static org.icgc.dcc.portal.util.Filters.geneSetFilter;
import static org.icgc.dcc.portal.util.Filters.inputGeneListFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class GeneRepository implements Repository {

  private static final Type CENTRIC_TYPE = Type.GENE_CENTRIC;
  private static final Type TYPE = Type.GENE;
  private static final Kind KIND = Kind.GENE;

  public static final Map<String, String> GENE_ID_SEARCH_FIELDS = ImmutableMap.<String, String>
      of("id.search", "_gene_id",
          "symbol.search", "symbol",
          "uniprotkbSwissprot.search", "external_db_ids.uniprotkb_swissprot");

  private final Client client;
  private final String index;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  @Autowired
  GeneRepository(Client client, IndexModel indexModel, QueryEngine queryEngine) {
    this.index = indexModel.getIndex();
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {

    // Converter does not handle limits
    Integer limit = query.getLimit();
    query.setLimit(null);

    val pql = converter.convert(query, GENE_CENTRIC);
    log.info(" find all centric {}", pql);
    val search = queryEngine.execute(pql, GENE_CENTRIC);
    if (limit != null) {
      search.getRequestBuilder().setSize(limit.intValue());
    }

    log.info(" find all centric {}", search);

    return search.getRequestBuilder().execute().actionGet();
  }

  private Map<String, String> findGeneSymbolsByFilters(@NonNull ObjectNode filters) {
    val maxGenes = 70000;
    val symbolFieldName = "symbol";

    val query = Query.builder().filters(filters).build();
    val pql = converter.convert(query, GENE_CENTRIC);
    val response = queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder()
        .setSize(maxGenes)
        .addField(symbolFieldName)
        .execute().actionGet();

    val map = Maps.<String, String> newLinkedHashMap();
    for (val hit : response.getHits()) {
      String id = hit.getId();
      String symbol = hit.getFields().get(symbolFieldName).getValue();

      map.put(id, symbol);
    }

    return map;
  }

  public Map<String, String> findGeneSymbolsByGeneListIdAndGeneSetId(@NonNull UUID inputGeneListId,
      @NonNull String geneSetId) {
    val filters = andFilter(geneSetFilter(geneSetId), inputGeneListFilter(inputGeneListId));
    return findGeneSymbolsByFilters(filters);
  }

  public Map<String, String> findGeneSymbolsByGeneListId(@NonNull UUID inputGeneListId) {
    val filters = inputGeneListFilter(inputGeneListId);
    return findGeneSymbolsByFilters(filters);
  }

  public SearchResponse findGeneSetCounts(Query query) {
    log.info(" My Query {} ", query.getFilters());

    val pql = converter.convert(query, GENE_CENTRIC);
    val search = queryEngine.execute(pql, GENE_CENTRIC);

    for (val universe : Universe.values()) {
      val universeFacetName = universe.getGeneSetFacetName();

      // FIXME: migrate to aggregation
      search.getRequestBuilder()
          .addFacet(termsFacet(universeFacetName).field(universeFacetName).size(50000));

    }
    search.getRequestBuilder().setSearchType(COUNT);
    return search.getRequestBuilder().execute().actionGet();
  }

  @Override
  public SearchResponse findAll(Query query) {
    throw new UnsupportedOperationException("Not applicable");
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    throw new UnsupportedOperationException("Not applicable");
  }

  @Override
  public long count(Query query) {
    val pql = converter.convertCount(query, GENE_CENTRIC);
    val search = queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder();

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    val search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      val pql = converter.convertCount(query, GENE_CENTRIC);
      search.add(queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder());
    }

    log.debug("{}", search);

    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        val pql = converter.convertCount(innerQuery, GENE_CENTRIC);
        search.add(queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder());
      }
    }

    log.debug("{}", search);

    return search.execute().actionGet();
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    throw new UnsupportedOperationException("Not applicable");
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE.getId(), id);
    val sourceFields = prepareSourceFields(query, getFields(query, KIND));
    String[] excludeFields = null;
    search.setFetchSource(sourceFields, excludeFields);

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val result = response.getSource();
    log.debug("{}", result);

    return result;
  }

  private String[] prepareSourceFields(Query query, String[] fields) {
    val typeFieldsMap = FIELDS_MAPPING.get(KIND);
    val result = Lists.newArrayList(fields);

    if (!query.hasFields()) {
      result.add(typeFieldsMap.get("externalDbIds"));
      result.add(typeFieldsMap.get("pathways"));
    }

    if (query.getIncludes() != null) {
      result.addAll(query.getIncludes());
    }

    return result.toArray(new String[result.size()]);
  }

  /*
   * Lookup up genes by ensembl gene_id or gene symbol or uniprot
   * 
   * @param input a list of string identifiers of either ensembl id or gene symbol or uniprot
   * 
   * @returns a map of matched identifiers
   */
  public SearchResponse validateIdentifiers(List<String> input) {
    val boolQuery = QueryBuilders.boolQuery();

    val search = client.prepareSearch(index)
        .setTypes("gene-text")
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(5000);

    for (val searchField : GENE_ID_SEARCH_FIELDS.keySet()) {
      boolQuery.should(QueryBuilders.termsQuery(searchField, input.toArray()));
      search.addHighlightedField(searchField);
      search.addField(searchField);
    }
    search.setQuery(boolQuery);
    log.info("Search is {}", search);

    val response = search.execute().actionGet();
    return response;
  }

  /**
   * Find transcripts for a specific gene that have mutations
   * @param geneId
   * @return unique list of transcript ids
   */
  public List<String> getAffectedTranscripts(String geneId) {
    val transcriptField = "donor.ssm.consequence.transcript_affected";
    val result = Lists.<String> newArrayList();
    val search =
        client
            .prepareSearch(index)
            .setTypes(CENTRIC_TYPE.getId())
            .setSearchType(QUERY_THEN_FETCH)
            .setSize(0)
            .addFacet(
                FacetBuilders.termsFacet("affectedTranscript")
                    .nested("donor.ssm.consequence")
                    .size(IndexModel.MAX_FACET_TERM_COUNT)
                    .field(transcriptField)
                    .facetFilter(FilterBuilders.termFilter("donor.ssm.consequence._gene_id", geneId)));

    val response = search.execute().actionGet();
    val facet = (InternalStringTermsFacet) response.getFacets().facet("affectedTranscript");

    for (val entry : facet.getEntries()) {
      result.add(entry.getTerm().toString());
    }
    return result;
  }
}
