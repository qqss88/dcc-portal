package org.icgc.dcc.portal.service;

import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.repository.GeneRepository.GENE_ID_SEARCH_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.SearchResponses.getCounts;
import static org.icgc.dcc.portal.util.SearchResponses.getNestedCounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.model.Gene;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.AggregationToFacetConverter;
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

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneService {

  private final GeneRepository geneRepository;
  private final AggregationToFacetConverter aggregationsConverter = AggregationToFacetConverter.getInstance();

  ImmutableMap<String, String> fields = FIELDS_MAPPING.get("gene");

  /**
   * Convert result from gene-text to a gene model
   */
  private Gene geneText2Gene(SearchHit hit) {
    val fieldMap = createResponseMap(hit, Query.builder().build(), Kind.GENE);
    Map<String, Object> geneMap = Maps.newHashMap();
    for (val key : fieldMap.keySet()) {
      geneMap.put(GENE_ID_SEARCH_FIELDS.get(key), fieldMap.get(key));
    }
    return new Gene(geneMap);
  }

  /**
   * Check whether <strong>ids</strong> match any gene identifiers.
   * 
   * The output is doubly grouped/pivoted. The first level is by the search type, i.e., symbol, id. The second level is
   * by the input id. In most cases the input id is one-to-one with a specific gene, but there are exceptions.
   * 
   * Note here we assume the <strong>ids</strong> are in lower cases <br>
   * <br>
   * 
   * Example repository response: <br>
   * <ul>
   * <li>hit1
   * <ul>
   * <li>fields: [...]</li>
   * <li>highlight: [...]</li>
   * </ul>
   * </li>
   * <li>hit2
   * <ul>
   * <li>fields: [...]</li>
   * <li>highlight: [...]</li>
   * </ul>
   * </li>
   * 
   * </ul>
   * 
   * Example output:<br>
   * <ul>
   * <li>
   * symbol:
   * <ul>
   * <li>id1: [G1]</li>
   * <li>id2: [G2]</li>
   * </ul>
   * </li>
   * <li>
   * uniprot:
   * <ul>
   * <li>
   * id3: [G3, G4]</li>
   * <li>
   * id:4: [G5]</li>
   * </ul>
   * </li>
   * </ul>
   */
  public Map<String, Multimap<String, Gene>> validateIdentifiers(List<String> ids) {
    val response = geneRepository.validateIdentifiers(ids);

    val result = Maps.<String, Multimap<String, Gene>> newHashMap();
    for (val search : GENE_ID_SEARCH_FIELDS.values()) {
      val typeResult = ArrayListMultimap.<String, Gene> create();
      result.put(search, typeResult);
    }

    // Organize the results into the categories
    // Note: it may be possible that a uniprot id can be matched to multiple genes
    for (val hit : response.getHits()) {
      val fields = hit.getFields();
      val highlightedFields = hit.getHighlightFields();
      val matchedGene = geneText2Gene(hit);

      // Check which search field got the "hit"
      for (val searchField : GENE_ID_SEARCH_FIELDS.keySet()) {

        if (highlightedFields.containsKey(searchField)) {

          val field = GENE_ID_SEARCH_FIELDS.get(searchField);

          // Note: it is possible that a gene hit has multiple uniprot ids (TAF9, FAU, to name a few)
          // Because we need to group by the inpu, we need to figure out which one of the uniprot ids
          // was in the input identifiers - this requires us to normalize to lower case to make the comparisons
          if (field.equals(GENE_UNIPROT_IDS)) {
            val keys = fields.get(searchField).getValues();

            for (val key : keys) {
              if (ids.contains(key.toString().toLowerCase())) {
                result.get(field).put(getString(key), matchedGene);
              }
            }
          } else {
            val key = getString(fields.get(searchField).getValues());
            result.get(field).put(key, matchedGene);
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
    log.debug("Response: {}", response);
    val hits = response.getHits();

    boolean includeScore = !query.hasFields() || query.getFields().contains("affectedDonorCountFiltered");

    val list = ImmutableList.<Gene> builder();

    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, query, Kind.GENE);
      if (includeScore) fieldMap.put("_score", hit.getScore());
      fieldMap.put("projectIds", projectIds);
      list.add(new Gene(fieldMap));
    }

    Genes genes = new Genes(list.build());
    genes.addFacets(aggregationsConverter.convert(response.getAggregations()));
    // genes.setFacets(response.getFacets());
    genes.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return genes;
  }

  public long count(Query query) {
    return geneRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = geneRepository.counts(queries);

    return getCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = geneRepository.nestedCounts(queries);

    return getNestedCounts(queries, sr);
  }

  public Gene findOne(String geneId, Query query) {
    return new Gene(geneRepository.findOne(geneId, query));
  }

  public List<String> getAffectedTranscripts(String geneId) {
    return geneRepository.getAffectedTranscripts(geneId);
  }
}
