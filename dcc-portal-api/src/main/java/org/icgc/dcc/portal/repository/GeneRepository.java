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

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.QueryBuilders.customScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.service.QueryService.buildConsequenceFilters;
import static org.icgc.dcc.portal.service.QueryService.buildDonorFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneFilters;
import static org.icgc.dcc.portal.service.QueryService.buildMutationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildObservationFilters;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.remapM2C;
import static org.icgc.dcc.portal.service.QueryService.remapM2O;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@Slf4j
@Component
public class GeneRepository implements Repository {

  private static final String SCORE = "x = doc['donor._summary._ssm_count']; x.empty || x.value < 1 ? 0 : 1";

  private static final Type CENTRIC_TYPE = Type.GENE_CENTRIC;
  private static final Type TYPE = Type.GENE;
  private static final Kind KIND = Kind.GENE;

  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap.of(
      Kind.DONOR, "donor",
      Kind.MUTATION, "donor.ssm",
      Kind.CONSEQUENCE, "donor.ssm.consequence",
      Kind.OBSERVATION, "donor.ssm.observation"));
  protected static final ImmutableMap<Kind, String> PREFIX_MAPPING = NESTED_MAPPING;

  private static final ImmutableList<String> FACETS = ImmutableList.of("type", "list");

  private final Client client;
  private final String index;

  @Autowired
  GeneRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  private List<TermsFacetBuilder> getFacets(Query query, ObjectNode filters) {
    val fs = Lists.<TermsFacetBuilder> newArrayList();

    if (query.hasInclude("facets")) {
      for (String facet : FACETS) {
        val tf =
            FacetBuilders.termsFacet(facet).field(FIELDS_MAPPING.get(KIND).get(facet))
                .size(MAX_FACET_TERM_COUNT);

        if (filters.fieldNames().hasNext()) {
          val facetFilters = filters.deepCopy();
          if (facetFilters.has(KIND.getId())) {
            facetFilters.with(KIND.getId()).remove(facet);
          }

          tf.facetFilter(getFilters(facetFilters));
        }
        fs.add(tf);
      }
    }
    return fs;
  }

  private FilterBuilder getFilters(ObjectNode filters) {
    if (filters.fieldNames().hasNext()) return buildFilters(filters);
    return matchAllFilter();
  }

  private FilterBuilder buildFilters(ObjectNode filters) {
    val qb = FilterBuilders.boolFilter();
    val musts = Lists.<FilterBuilder> newArrayList();
    boolean matchAll = true;

    boolean hasDonor = hasDonor(filters);
    boolean hasGene = hasGene(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasGene || hasDonor || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      if (hasGene) {
        musts.add(buildGeneFilters(filters, PREFIX_MAPPING));
      }
      if (hasDonor || hasMutation || hasConsequence || hasObservation) {
        val db = FilterBuilders.boolFilter();
        val dMusts = buildDonorNestedFilters(filters, hasDonor, hasMutation, hasConsequence, hasObservation);
        db.must(dMusts.toArray(new FilterBuilder[dMusts.size()]));
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.DONOR), db));
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
  }

  @Override
  public SearchResponse findAllCentric(Query query) {
    val search = buildFindAllRequest(query, CENTRIC_TYPE);

    search.setQuery(buildQuery(query));

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  @Override
  public SearchResponse findAll(Query query) {
    val search = buildFindAllRequest(query, TYPE);

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    val search =
        client.prepareSearch(index).setTypes(type.getId()).setSearchType(QUERY_THEN_FETCH).setFrom(query.getFrom())
            .setSize(query.getSize());

    ObjectNode filters = remapFilters(query.getFilters());
    search.setFilter(getFilters(filters));

    search.addFields(getFields(query, KIND));

    if (query.hasInclude("transcripts")) search.addField("transcripts");
    if (query.hasInclude("projects")) search.addField("project");
    if (query.hasInclude("pathways")) search.addField("pathways");

    val facets = getFacets(query, filters);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    String sort = FIELDS_MAPPING.get(KIND).get(query.getSort());
    search.addSort(fieldSort(sort).order(query.getOrder()));
    if (!sort.equals("_score")) search.addSort("_score", SortOrder.DESC);
    return search;
  }

  public FilterBuilder buildScoreFilters(Query query) {
    ObjectNode filters = remapFilters(query.getFilters());

    val qb = FilterBuilders.boolFilter();
    boolean matchAll = true;

    boolean hasDonor = hasDonor(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasDonor || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      val dMusts = buildDonorNestedFilters(filters, hasDonor, hasMutation, hasConsequence, hasObservation);
      qb.must(dMusts.toArray(new FilterBuilder[dMusts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
  }

  private List<FilterBuilder> buildDonorNestedFilters(ObjectNode filters,
      boolean hasDonor, boolean hasMutation, boolean hasConsequence, boolean hasObservation) {
    val dMusts = Lists.<FilterBuilder> newArrayList();
    if (hasDonor) dMusts.add(buildDonorFilters(filters, PREFIX_MAPPING));
    if (hasMutation || hasConsequence || hasObservation) {
      val nb = FilterBuilders.boolFilter();
      val nMusts = Lists.<FilterBuilder> newArrayList();
      if (hasMutation) nMusts.add(buildMutationFilters(filters, PREFIX_MAPPING));
      if (hasConsequence) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.CONSEQUENCE),
          buildConsequenceFilters(filters, PREFIX_MAPPING)));

      if (hasObservation) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION),
          buildObservationFilters(filters, PREFIX_MAPPING)));

      nb.must(nMusts.toArray(new FilterBuilder[nMusts.size()]));
      dMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.MUTATION), nb));
    }
    return dMusts;
  }

  @Override
  public long count(Query query) {
    val search = buildCountRequest(query, CENTRIC_TYPE);

    log.debug("{}", search);

    return search.execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val id : queries.keySet()) {
      search.add(buildCountRequest(queries.get(id), CENTRIC_TYPE));
    }

    log.debug("{}", search);

    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val id1 : queries.keySet()) {
      val nestedQuery = queries.get(id1);
      for (val id2 : nestedQuery.keySet()) {
        search.add(buildCountRequest(nestedQuery.get(id2), CENTRIC_TYPE));
      }
    }

    log.debug("{}", search);

    return search.execute().actionGet();
  }

  public SearchRequestBuilder buildCountRequest(Query query, Type type) {
    val search = client.prepareSearch(index).setTypes(type.getId()).setSearchType(COUNT);

    if (query.hasFilters()) {
      ObjectNode filters = remapFilters(query.getFilters());
      search.setFilter(getFilters(filters));
      search.setQuery(buildQuery(query));
    }
    return search;
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    return nestedQuery(
        "donor",
        customScoreQuery(filteredQuery(matchAllQuery(), buildScoreFilters(query))).script(SCORE)).scoreMode("total");
  }

  public ObjectNode remapFilters(ObjectNode filters) {
    return remapM2O(remapM2C(filters));
  }

  public Map<String, Object> findOne(String id, Query query) {
    val fieldMapping = FIELDS_MAPPING.get(KIND);
    val fs = Lists.<String> newArrayList();

    val search = client.prepareGet(index, CENTRIC_TYPE.getId(), id);

    if (query.hasFields()) {
      for (String field : query.getFields()) {
        if (fieldMapping.containsKey(field)) {
          fs.add(fieldMapping.get(field));
        }
      }
    } else
      fs.addAll(fieldMapping.values().asList());

    if (query.hasInclude("transcripts")) fs.add("transcripts");
    if (query.hasInclude("projects")) fs.add("project");
    if (query.hasInclude("pathways")) fs.add("pathways");

    search.setFields(fs.toArray(new String[fs.size()]));

    GetResponse response = search.execute().actionGet();

    if (!response.isExists()) {
      String type = KIND.getId().substring(0, 1).toUpperCase() + KIND.getId().substring(1);
      log.info("{} {} not found.", type, id);
      String msg = String.format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(msg).build());
    }

    val map = Maps.<String, Object> newHashMap();
    for (val f : response.getFields().values()) {
      if (Lists.newArrayList(fieldMapping.get("affectedTranscriptIds"), fieldMapping.get("synonyms"), "transcripts",
          "project", fieldMapping.get("list"), "pathways").contains(f.getName())) {
        map.put(f.getName(), f.getValues());
      } else {
        map.put(f.getName(), f.getValue());
      }
    }

    log.debug("{}", map);

    return map;
  }

  /*
   * Lookup up genes by ensembl gene_id or gene symbol
   * 
   * @param input a list of string identifiers of either ensembl id or gene symbol
   * 
   * @returns a map of matched identifiers
   */
  public Multimap<String, String> validateIdentifiers(List<String> input) {
    val boolFilter = FilterBuilders.boolFilter();
    val idFilter = FilterBuilders.termsFilter("_gene_id", input.toArray());
    val symbolFilter = FilterBuilders.termsFilter("symbol", input.toArray());

    // gene_id => symbol is many-to-one, so we need two lookup tables
    val idLookup = Maps.<String, String> newHashMap();
    val symbolLookup = ArrayListMultimap.<String, String> create();

    boolFilter.should(idFilter, symbolFilter);
    val search = client.prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFilter(boolFilter)
        .addFields("_gene_id", "symbol")
        .setSize(5000);

    // Get the valid lookups
    log.info("Search is {}", search);
    val response = search.execute().actionGet();
    for (SearchHit hit : response.getHits()) {
      val id = (String) hit.getFields().get("_gene_id").getValue();
      val symbol = (String) hit.getFields().get("symbol").getValue();

      idLookup.put(id, symbol);
      symbolLookup.put(symbol, id);
      log.info("{} - {}", id, symbol);
    }

    // Now match with input
    val result = ArrayListMultimap.<String, String> create();
    for (String key : input) {
      if (idLookup.containsKey(key)) {
        result.put(key, key);
        continue;
      }
      if (symbolLookup.containsKey(key)) {
        result.putAll(key, symbolLookup.get(key));
        continue;
      }
    }

    return result;
  }
}
