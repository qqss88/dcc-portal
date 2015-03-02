package org.icgc.dcc.portal.service;

import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.ServiceUtils.buildCounts;
import static org.icgc.dcc.portal.service.ServiceUtils.buildNestedCounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.icgc.dcc.portal.model.Gene;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneService {

  private final GeneRepository geneRepository;

  ImmutableMap<String, String> fields = FIELDS_MAPPING.get("gene");

  public Map<String, Multimap<String, Gene>> validateIdentifiers(List<String> ids) {
    val response = geneRepository.validateIdentifiers(ids);

    // Initialize results container
    val result = Maps.<String, Multimap<String, Gene>> newHashMap();
    for (val searchField : GeneRepository.GENE_ID_SEARCH_FIELDS) {
      val typeResult = ArrayListMultimap.<String, Gene> create();
      result.put(searchField, typeResult);
    }

    // Organize the results into the categories
    for (val hit : response.getHits()) {
      val fields = hit.getFields();
      val highlightedFields = hit.getHighlightFields();

      val fieldMap = Maps.<String, Object> newHashMap();
      for (val field : hit.getFields().entrySet()) {
        fieldMap.put(field.getKey(), field.getValue().getValue());
      }
      val matchedGene = new Gene(fieldMap);

      for (val searchField : GeneRepository.GENE_ID_SEARCH_FIELDS) {
        if (highlightedFields.containsKey(searchField)) {
          if (searchField.equals(GENE_UNIPROT_IDS)) {
            @SuppressWarnings("unchecked")
            val keys = (List<String>) fields.get(searchField).getValue();
            for (val key : keys) {
              if (ids.contains(key)) {
                result.get(searchField).put(key, matchedGene);
              }
            }
          } else {
            val key = (String) fields.get(searchField).getValue();
            result.get(searchField).put(key, matchedGene);
          }

        }
      }
    }
    return result;
  }

  public Genes findAllCentric(Query query) {
    val projectIds = Lists.<String> newArrayList();

    // Get a list of projectId to filter the projects sub-object in the gene model
    // FIXME This won't support NOT
    val path = query.getFilters().path("donor").path("projectId");
    if (path.path("is").isArray()) {
      for (JsonNode id : path.get("is")) {
        projectIds.add(String.valueOf(id).replaceAll("\"", ""));
      }
    }
    if (path.path("is").isTextual()) {
      projectIds.add(String.valueOf(path.get("is")).replaceAll("\"", ""));
    }

    val response = geneRepository.findAllCentric(query);
    val hits = response.getHits();

    boolean includeScore = !query.hasFields() || query.getFields().contains("affectedDonorCountFiltered");

    val list = ImmutableList.<Gene> builder();

    for (val hit : hits) {
      val fieldMap = Maps.<String, Object> newHashMap();
      for (val field : hit.getFields().entrySet()) {

        fieldMap.put(field.getKey(), field.getValue().getValue());

      }
      if (includeScore) fieldMap.put("_score", hit.getScore());
      fieldMap.put("projectIds", projectIds);
      list.add(new Gene(fieldMap));
    }

    Genes genes = new Genes(list.build());
    genes.setFacets(response.getFacets());
    genes.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return genes;
  }

  public long count(Query query) {
    return geneRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = geneRepository.counts(queries);

    return buildCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = geneRepository.nestedCounts(queries);

    return buildNestedCounts(queries, sr);
  }

  public Gene findOne(String geneId, Query query) {
    return new Gene(geneRepository.findOne(geneId, query));
  }

  public List<String> getAffectedTranscripts(String geneId) {
    return geneRepository.getAffectedTranscripts(geneId);
  }
}
