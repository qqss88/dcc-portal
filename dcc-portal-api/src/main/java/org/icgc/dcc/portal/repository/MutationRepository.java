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
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.buildConsequenceFilters;
import static org.icgc.dcc.portal.service.QueryService.buildDonorFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneSetFilters;
import static org.icgc.dcc.portal.service.QueryService.buildMutationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildObservationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildProjectFilters;
import static org.icgc.dcc.portal.service.QueryService.buildTranscriptFilters;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasGeneSet;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.hasProject;
import static org.icgc.dcc.portal.service.QueryService.hasTranscript;
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
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class MutationRepository implements Repository {

  private static final Type CENTRIC_TYPE = Type.MUTATION_CENTRIC;
  private static final Type TYPE = Type.MUTATION;
  private static final Kind KIND = Kind.MUTATION;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.EMB_OCCURRENCE, "ssm_occurrence")
      .put(Kind.PROJECT, "ssm_occurrence")
      .put(Kind.DONOR, "ssm_occurrence")
      .put(Kind.TRANSCRIPT, "transcript")
      .put(Kind.GENE, "transcript")
      .put(Kind.OBSERVATION, "ssm_occurrence.observation")
      .build());

  static final ImmutableMap<Kind, String> PREFIX_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.EMB_OCCURRENCE, "ssm_occurrence")
      .put(Kind.PROJECT, "ssm_occurrence.project")
      .put(Kind.DONOR, "ssm_occurrence.donor")
      .put(Kind.TRANSCRIPT, "transcript")
      .put(Kind.CONSEQUENCE, "transcript.consequence")
      .put(Kind.GENE, "transcript.gene")
      .put(Kind.GENE_SET, "transcript.gene")
      .put(Kind.OBSERVATION, "ssm_occurrence.observation")
      .build());

  private static final ImmutableList<String> FACETS = ImmutableList.of("type", "consequenceTypeNested",
      "consequenceType", "platform", "verificationStatus", "platformNested", "verificationStatusNested",
      "functionalImpact", "functionalImpactNested", "sequencingStrategy", "sequencingStrategyNested");

  private final Client client;
  private final String index;

  @Autowired
  MutationRepository(Client client, IndexModel indexModel, QueryEngine queryEngine) {
    this.index = indexModel.getIndex();
    this.client = client;
    this.queryEngine = queryEngine;
  }

  // Needed to check for consequenceTypeNested
  private FilterBuilder getFilters(ObjectNode filters, String facetName) {
    if (filters.fieldNames().hasNext()) {
      return buildFilters(filters, facetName);
    }
    return matchAllFilter();
  }

  private FilterBuilder buildFilters(ObjectNode filters, String facetName) {
    val qb = FilterBuilders.boolFilter();
    val musts = Lists.<FilterBuilder> newArrayList();
    boolean matchAll = true;

    boolean hasDonor = hasDonor(filters);
    boolean hasProject = hasProject(filters);
    boolean hasGene = hasGene(filters);
    boolean hasGeneSet = hasGeneSet(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasTranscript = hasTranscript(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasProject || hasGene || hasGeneSet || hasDonor || hasMutation || hasConsequence || hasTranscript
        || hasObservation) {
      matchAll = false;
      if (hasMutation) {
        musts.add(buildMutationFilters(filters, PREFIX_MAPPING));
      }
      if (hasGene || hasGeneSet || hasConsequence || hasTranscript) {
        val tb = FilterBuilders.boolFilter();
        val tMusts = Lists.<FilterBuilder> newArrayList();
        if (hasTranscript) tMusts.add(buildTranscriptFilters(filters, PREFIX_MAPPING));
        if (hasConsequence) tMusts.add(buildConsequenceFilters(filters, PREFIX_MAPPING));
        if (hasGene) tMusts.add(buildGeneFilters(filters, PREFIX_MAPPING));

        if (hasGeneSet) tMusts.add(buildGeneSetFilters(filters, PREFIX_MAPPING));
        // if (hasGeneSet) tMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.GENE_SET),
        // buildGeneSetFilters(filters, PREFIX_MAPPING)));

        tb.must(tMusts.toArray(new FilterBuilder[tMusts.size()]));

        // Facet is nested so filter cannot be nested
        if (facetName.equals("consequenceTypeNested") || facetName.equals("functionalImpactNested")) {
          musts.add(tb);
        } else {
          musts.add(nestedFilter(NESTED_MAPPING.get(Kind.TRANSCRIPT), tb));
        }
      }
      if (hasObservation || hasProject || hasDonor) {
        val ob = FilterBuilders.boolFilter();

        // Facet is nested so filter cannot be nested
        if (facetName.equals("platformNested") || facetName.equals("verificationStatusNested")
            || facetName.equals("sequencingStrategyNested")) {
          val observationMusts = Lists.<FilterBuilder> newArrayList();
          if (hasObservation) observationMusts.add(buildObservationFilters(filters, PREFIX_MAPPING));
          ob.must(observationMusts.toArray(new FilterBuilder[observationMusts.size()]));
          musts.add(ob);
        } else {
          val oMusts = buildOccurrenceNestedFilters(filters, hasDonor, hasProject, hasObservation);
          ob.must(oMusts.toArray(new FilterBuilder[oMusts.size()]));
          musts.add(nestedFilter(NESTED_MAPPING.get(Kind.EMB_OCCURRENCE), ob));
        }
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }

    return matchAll ? matchAllFilter() : qb;
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

  private List<FilterBuilder> buildOccurrenceNestedFilters(
      ObjectNode filters, boolean hasDonor, boolean hasProject, boolean hasObservation) {
    val oMusts = Lists.<FilterBuilder> newArrayList();
    if (hasProject) oMusts.add(buildProjectFilters(filters, PREFIX_MAPPING));
    // if (hasObservation) oMusts.add(buildObservationFilters(filters, PREFIX_MAPPING));
    if (hasObservation) {
      oMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION), buildObservationFilters(filters, PREFIX_MAPPING)));
    }
    if (hasDonor) oMusts.add(buildDonorFilters(filters, PREFIX_MAPPING));
    return oMusts;
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

  public SearchResponse protein2(Query query) {
    ImmutableMap<String, String> fields = FIELDS_MAPPING.get(KIND);

    val search = client
        .prepareSearch(index)
        .setTypes(CENTRIC_TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(10000);

    search.setPostFilter(getFilters(query.getFilters(), null));

    search.addFields(new String[] {
        fields.get("id"),
        fields.get("mutation"),
        fields.get("affectedDonorCountTotal"),
        fields.get("functionalImpact"),
        fields.get("transcriptId"),
        "transcript.consequence.aa_mutation",
        "transcript.functional_impact_prediction_summary"
    });

    log.info("!!! {}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }
}
