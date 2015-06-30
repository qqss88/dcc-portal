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

import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.resolveSourceFields;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class MutationRepository implements Repository {

  private static final Type CENTRIC_TYPE = Type.MUTATION_CENTRIC;
  private static final Kind KIND = Kind.MUTATION;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  private final Client client;
  private final String index;

  @Autowired
  MutationRepository(Client client, IndexModel indexModel, QueryEngine queryEngine) {
    this.index = indexModel.getIndex();
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {
    val pql = converter.convert(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);

    log.info("Mutation : {}", search.getRequestBuilder());

    SearchResponse response = search.getRequestBuilder().execute().actionGet();
    return response;
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
    log.info("Count Query {}", query.getFilters());
    val pql = converter.convertCount(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);
    return search.getRequestBuilder().execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(@NonNull LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();
    for (val id : queries.keySet()) {
      val pql = converter.convertCount(queries.get(id), MUTATION_CENTRIC);
      search.add(queryEngine.execute(pql, MUTATION_CENTRIC).getRequestBuilder());
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  public MultiSearchResponse countSearches(@NonNull List<QueryBuilder> searches) {
    val search = client.prepareMultiSearch();
    for (val s : searches) {
      search.add(buildCountSearchFromQuery(s, CENTRIC_TYPE));
    }

    log.info("{}", search);
    return search.execute().actionGet();
  }

  public SearchRequestBuilder buildCountSearchFromQuery(QueryBuilder query, Type type) {
    val search = client.prepareSearch(index).setTypes(type.getId()).setSearchType(COUNT);
    search.setQuery(query);

    return search;
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();
    for (val id1 : queries.keySet()) {
      val nestedQuery = queries.get(id1);
      for (val id2 : nestedQuery.keySet()) {
        val pql = converter.convertCount(nestedQuery.get(id2), MUTATION_CENTRIC);
        search.add(queryEngine.execute(pql, MUTATION_CENTRIC).getRequestBuilder());
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
    val search = client.prepareGet(index, CENTRIC_TYPE.getId(), id);
    search.setFields(getFields(query, KIND));
    String[] sourceFields = resolveSourceFields(query, KIND);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, KIND), EMPTY_SOURCE_FIELDS);
    }

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val map = createResponseMap(response, query, KIND);
    log.debug("{}", map);

    return map;
  }

  public SearchResponse protein(Query query) {

    // Customize fields, we need to add more fields once we
    // have the search request, as not all the fields are publicly addressable through the PQL interface
    query.setFields(Lists.<String> newArrayList(
        "id",
        "mutation",
        "affectedDonorCountTotal",
        "functionalImpact",
        "transcriptId"));

    val pql = converter.convert(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);

    search.getRequestBuilder().setFrom(0).setSize(10000);
    search.getRequestBuilder().addFields(new String[] {
        "transcript.consequence.aa_mutation",
        "transcript.functional_impact_prediction_summary"
    });

    log.info("!!! {}", search.getRequestBuilder());

    SearchResponse response = search.getRequestBuilder().execute().actionGet();
    return response;
  }

}
