package org.icgc.dcc.portal.service;

import static org.icgc.dcc.portal.service.ServiceUtils.buildCounts;
import static org.icgc.dcc.portal.service.ServiceUtils.buildNestedCounts;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Mutation;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class MutationService {

  private final MutationRepository mutationRepository;
  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();
  private final AggregationToFacetConverter aggregationsConverter = AggregationToFacetConverter.getInstance();

  public Mutations findAllCentric(Query query) {
    val pql = converter.convert(query, Type.MUTATION_CENTRIC);
    log.debug("Query: {}. PQL: {}", query, pql);

    val request = queryEngine.execute(pql, Type.MUTATION_CENTRIC);
    log.debug("Request: {}", request);

    val response = request.getRequestBuilder().execute().actionGet();
    log.debug("Response: {}", response);

    val hits = response.getHits();

    // Include _score if either: no custom fields or custom fields include affectedDonorCountFiltered
    boolean includeScore = !query.hasFields() || query.getFields().contains("affectedDonorCountFiltered");

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = createResponseMap(hit, query, Kind.MUTATION);
      if (includeScore) map.put("_score", hit.getScore());
      list.add(new Mutation(map));
    }

    val mutations = new Mutations(list.build());
    mutations.addFacets(aggregationsConverter.convert(response.getAggregations()));
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return mutations;
  }

  public long count(Query query) {
    return mutationRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = mutationRepository.counts(queries);

    return buildCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = mutationRepository.nestedCounts(queries);

    return buildNestedCounts(queries, sr);
  }

  public Mutation findOne(String mutationId, Query query) {
    return new Mutation(mutationRepository.findOne(mutationId, query));
  }

  public Mutations protein(Query query) {
    log.info("{}", query);

    val response = mutationRepository.protein(query);
    val hits = response.getHits();

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = Maps.<String, Object> newHashMap();
      val transcripts = Lists.<Map<String, Object>> newArrayList();

      map.put("_mutation_id", hit.getFields().get("_mutation_id").getValue());
      map.put("mutation", hit.getFields().get("mutation").getValue());
      map.put("_summary._affected_donor_count", hit.getFields().get("_summary._affected_donor_count").getValue());
      map.put("functional_impact_prediction_summary", hit.getFields().get("functional_impact_prediction_summary")
          .getValues());

      List<Object> transcriptIds = hit.getFields().get("transcript.id").getValues();
      val predictionSummary = hit.getFields().get("transcript.functional_impact_prediction_summary").getValues();

      for (int i = 0; i < transcriptIds.size(); ++i) {
        val transcript = Maps.<String, Object> newHashMap();

        transcript.put("id", transcriptIds.get(i));
        transcript.put("functional_impact_prediction_summary", predictionSummary.get(i));

        val consequence = Maps.<String, Object> newHashMap();
        List<Object> f3 = hit.getFields().get("transcript.consequence.aa_mutation").getValues();
        consequence.put("aa_mutation", f3.get(i).toString());
        transcript.put("consequence", consequence);

        transcripts.add(transcript);
      }

      map.put("transcript", transcripts);

      list.add(new Mutation(map));
    }

    Mutations mutations = new Mutations(list.build());
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return mutations;
  }

}
