package org.icgc.dcc.portal.browser.ds;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;
import org.icgc.dcc.portal.browser.model.HistogramMutation;
import org.icgc.dcc.portal.browser.model.Mutation;
import org.icgc.dcc.portal.repository.BrowserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
@Service
public class MutationParser {

  @NonNull
  private BrowserRepository browserRepository;

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.reader();
  private static final TypeReference<List<String>> LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {};

  /**
   * Build a fully completed list of Mutation objects.
   */
  @SneakyThrows
  public List<Object> parse(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters) {
    val searchResponse = getResponse(segmentId, start, stop, consequenceTypes, projectFilters, 100000);
    String json = searchResponse.toString();
    JsonNode response = READER.readTree(json);

    List<Object> mutations = newArrayList();
    for (JsonNode hit : response.get("hits").get("hits")) {
      val mutation = getMutation(projectFilters, hit);
      mutations.add(mutation);
    }

    return mutations;
  }

  /**
   * Build a histogram representation of mutations.
   */
  public List<Object> parseHistogram(String segmentId, Long start, Long stop, Long interval,
      List<String> consequenceTypes, List<String> projectFilters) {
    val searchResponse = getHistogramResponse(interval, segmentId, start, stop, consequenceTypes, projectFilters);
    val histogram = (HistogramFacet) searchResponse.getFacets().facetsAsMap().get("hf");

    // Find max and index entries by start
    long highestAbsolute = 0l;
    Map<Long, HistogramFacet.Entry> entries = newHashMap();
    for (val entry : histogram.getEntries()) {
      entries.put(entry.getKey(), entry);

      if (entry.getCount() > highestAbsolute) {
        highestAbsolute = entry.getCount();
      }
    }

    List<Object> mutations = newArrayList();
    int intervalNumber = 0;
    long intervalStart = 0;
    long intervalStop = intervalStart + interval - 1;
    while (intervalStart < stop) {
      if (intervalStop >= start) {
        Entry entry = entries.get(intervalStart);
        val mutationCount = entry != null ? entry.getCount() : 0;

        val mutation = new HistogramMutation(
            intervalStart,
            intervalStop,
            intervalNumber,
            mutationCount,
            (double) mutationCount / highestAbsolute);

        mutations.add(mutation);

        intervalNumber++;
      }

      // Advance
      intervalStart += interval;
      intervalStop += interval;
    }

    return mutations;
  }

  /**
   * EnrichmentQueries elasticsearch for appropriate data.
   */
  private SearchResponse getResponse(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters, Integer size) {
    return browserRepository.getMutation(segmentId, start, stop, consequenceTypes, projectFilters, size);
  }

  /**
   * EnrichmentQueries elasticsearch with a histogram facet.
   */
  private SearchResponse getHistogramResponse(Long interval, String segmentId, Long start, Long stop,
      List<String> consequenceTypes, List<String> projectFilters) {
    return browserRepository.getMutationHistogram(interval, segmentId, start, stop, consequenceTypes, projectFilters);
  }

  /**
   * Builds a mutation.
   */
  private static Mutation getMutation(List<String> projectFilters, JsonNode response) throws IOException {
    JsonNode hit = response.get("fields");
    val projectKeys = asList(hit.get("ssm_occurrence.project._project_id"));
    val projectNames = asList(hit.get("ssm_occurrence.project.project_name"));
    val projectSsmTestedDonorCount = asList(hit.get("ssm_occurrence.project._summary._ssm_tested_donor_count"));

    val projectIds = getProjectIds(projectFilters, projectKeys, projectNames, projectSsmTestedDonorCount);

    int totalNumberOfDonors = projectKeys.size();

    List<String> projectInfo = newArrayList();

    // Builds project display string
    for (val entry : projectIds.entrySet()) {
      projectInfo.add(entry.getKey() + ": " +
          entry.getValue().get("affectedDonors") + " / " +
          entry.getValue().get("ssmTestedDonors"));
    }

    List<List<String>> consequences = newArrayList();
    for (val transcript : response.get("_source").get("transcript")) {
      List<String> consequence = getConsequence(
          transcript.path("consequence"),
          transcript.path("gene"));

      consequences.add(consequence);
    }

    List<String> functionalImpact = newArrayList();
    val functionalImpactTmp = hit.get("functional_impact_prediction_summary");
    if (functionalImpactTmp != null) {
      for (val fi : (ArrayNode) functionalImpactTmp) {
        functionalImpact.add(fi.asText());
      }
    }

    // Reference genome allele's accross ssm occurrences are same. This will be changed later so that the Reference
    // Genome Allele is at the source level instead of nested.
    val refGenAllele = hit.get("reference_genome_allele").get(0).asText();

    val mutation = Mutation.builder()
        .id(hit.path("_mutation_id").get(0).asText())
        .chromosome(hit.path("chromosome").get(0).asText())
        .start(hit.path("chromosome_start").get(0).asLong())
        .end(hit.path("chromosome_end").get(0).asLong())
        .mutationType(hit.path("mutation_type").get(0).asText())
        .mutation(hit.path("mutation").get(0).asText())
        .refGenAllele(refGenAllele)
        .total(totalNumberOfDonors)
        .projectInfo(projectInfo)
        .consequences(consequences)
        .functionalImpact(functionalImpact)
        .build();

    return mutation;
  }

  /**
   * Checks if a value for a field is missing and replaces it's value with "null" if true. asText() cannot be called on
   * null, and if json node is returned then there are extra quotations that appear in json string
   * 
   * @param consequence - consequence JsonNode
   * @return formatted consequence string
   */
  private static List<String> getConsequence(JsonNode consequence, JsonNode gene) {
    String transcriptId =
        consequence.path("_transcript_id").isMissingNode() ? "null" : consequence.path("_transcript_id").asText();

    String consequenceType =
        consequence.path("consequence_type").isMissingNode() ? "null" : consequence.path("consequence_type").asText();

    String aaMutation =
        consequence.path("aa_mutation").isMissingNode() ? "null" : consequence.path("aa_mutation").asText();

    String geneSymbol = gene.path("symbol").isMissingNode() ? "null" : gene.path("symbol").asText();

    List<String> result = newArrayList();
    result.add(transcriptId);
    result.add(consequenceType);
    result.add(geneSymbol);
    result.add(aaMutation);

    return result;
  }

  /**
   * Builds a linked hash map with project-code as key and a secondary map containing attributes of the project
   * (affected-donors and ssm-tested-donor-count)
   */
  private static Map<String, Map<String, Integer>> getProjectIds(
      List<String> projectFilters,
      List<String> projectKeys,
      List<String> projectNames,
      List<String> projectSsmTestedDonorCounts) {

    Map<String, Map<String, Integer>> result = newLinkedHashMap();

    for (int i = 0; i < projectNames.size(); i++) {
      if (projectFilters == null || projectFilters.contains(projectNames.get(i))) {
        String projectId = projectKeys.get(i);

        if (result.get(projectId) == null) {
          Map<String, Integer> map = newLinkedHashMap();
          map.put("affectedDonors", 1);
          map.put("ssmTestedDonors", Integer.parseInt(projectSsmTestedDonorCounts.get(i)));
          result.put(projectId, map);
        } else {
          val map = result.get(projectId);
          int affectedDonors = map.get("affectedDonors");
          map.put("affectedDonors", ++affectedDonors);
        }
      }
    }
    return result;
  }

  /**
   * Readability method to build a list from a JsonNode returned as an array.
   */
  @SneakyThrows
  private static List<String> asList(JsonNode node) {
    return MAPPER.readValue(node.traverse(), LIST_TYPE_REFERENCE);
  }

}
