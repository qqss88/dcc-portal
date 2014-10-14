package org.icgc.dcc.portal.service;

import static org.icgc.dcc.portal.service.ServiceUtils.buildCounts;
import static org.icgc.dcc.portal.service.ServiceUtils.buildNestedCounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.icgc.dcc.portal.model.Mutation;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_({ @Autowired }))
public class MutationService {

  private final MutationRepository mutationRepository;

  public Mutations findAllCentric(Query query) {
    log.info("{}", query);

    val response = mutationRepository.findAllCentric(query);
    val hits = response.getHits();

    // Include _score if either: no custom fields or custom fields include affectedDonorCountFiltered
    boolean includeScore = !query.hasFields() || query.getFields().contains("affectedDonorCountFiltered");

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = Maps.<String, Object> newHashMap();
      for (val field : hit.getFields().entrySet()) {
        map.put(field.getKey(), field.getValue().getValue());
      }

      if (includeScore) map.put("_score", hit.getScore());

      if (query.hasInclude("consequences")) {
        if (query.hasInclude("transcripts")) {
          // TODO redundant
          map.put("consequences", map.get("transcript"));
        } else {
          // TODO redundant
          map.put("consequences", map.get("transcript"));
          map.remove("transcript");
        }
      }

      list.add(new Mutation(map));
    }

    Mutations mutations = new Mutations(list.build());
    Facets mergedFacets = mergeFacets(response.getFacets(), "consequenceTypeNested", "consequenceType");
    mergedFacets = mergeFacets(mergedFacets, "platformNested", "platform");
    mergedFacets = mergeFacets(mergedFacets, "verificationStatusNested", "verificationStatus");
    mergedFacets = mergeFacets(mergedFacets, "functionalImpactNested", "functionalImpact");
    mergedFacets = mergeFacets(mergedFacets, "sequencingStrategyNested", "sequencingStrategy");

    mutations.setFacets(mergedFacets);
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
          .getValue());

      List<Object> c = hit.getFields().get("transcript.id").getValue();

      for (int i = 0; i < c.size(); ++i) {
        val transcript = Maps.<String, Object> newHashMap();

        List<String> f1 = hit.getFields().get("transcript.id").getValue();
        transcript.put("id", f1.get(i));

        List<String> f2 = hit.getFields().get("transcript.functional_impact_prediction_summary").getValue();
        transcript.put("functional_impact_prediction_summary", f2.get(i));

        // FIXME: This part is weird, but works
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

  private Facets mergeFacets(Facets facets, String nested, String top) {
    if (facets != null) {
      TermsFacet nestedFacet = facets.facet(nested);
      TermsFacet topFacet = facets.facet(top);

      val topEntries = topFacet.getEntries();

      Set<String> topTerms = Sets.newHashSet();
      for (val entry : topEntries) {
        topTerms.add(String.valueOf(entry.getTerm()));
      }

      Set<String> nestedTerms = Sets.newHashSet();
      for (val entry : nestedFacet.getEntries()) {
        nestedTerms.add(String.valueOf(entry.getTerm()));
      }

      val removableTerms = Sets.difference(topTerms, nestedTerms).immutableCopy();

      val removableEntries = Lists.<TermsFacet.Entry> newArrayList();

      for (val entry : topEntries) {
        if (removableTerms.contains(String.valueOf(entry.getTerm()))) {
          removableEntries.add(entry);
        }
      }

      topEntries.removeAll(removableEntries);
    }

    return facets;
  }
}
