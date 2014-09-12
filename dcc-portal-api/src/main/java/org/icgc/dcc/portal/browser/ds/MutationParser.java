package org.icgc.dcc.portal.browser.ds;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.elasticsearch.action.search.SearchType.QUERY_AND_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.andFilter;
import static org.elasticsearch.index.query.FilterBuilders.orFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.search.facet.FacetBuilders.histogramFacet;
import static org.icgc.dcc.portal.util.FormatUtils.formatRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;
import org.icgc.dcc.portal.browser.model.HistogramMutation;
import org.icgc.dcc.portal.browser.model.Mutation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Slf4j
@RequiredArgsConstructor
public class MutationParser {

  /**
   * Constants.
   */
  private static final String INDEX_TYPE = "mutation-centric";
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.reader();
  private static final TypeReference<List<String>> LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {};

  /**
   * Parser state.
   */
  @NonNull
  private final Client client;
  @NonNull
  private final String indexName;

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
      val mutation = getMutation(projectFilters, hit.get("fields"));
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
   * Queries elasticsearch for appropriate data.
   */
  private SearchResponse getResponse(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters, Integer size) {
    val filter = getFilter(segmentId, start, stop, consequenceTypes, projectFilters);

    val request = client.prepareSearch(indexName)
        .setTypes(INDEX_TYPE)
        .setSearchType(QUERY_AND_FETCH)
        .setFilter(filter)
        .addFields(
            "_mutation_id",
            "chromosome",
            "chromosome_start",
            "chromosome_end",
            "mutation_type",
            "mutation",
            "reference_genome_allele",
            "functional_impact_prediction_summary",
            "ssm_occurrence.project._project_id",
            "ssm_occurrence.project.project_name",
            "ssm_occurrence.project._summary._ssm_tested_donor_count")
        .addPartialField("transcript",
            includes(
                "transcript.gene.symbol",
                "transcript.consequence._transcript_id",
                "transcript.consequence.consequence_type",
                "transcript.consequence.aa_mutation"),
            excludes())
        .setFrom(0)
        .setSize(size);

    logRequest(request);

    return request.execute().actionGet();
  }

  /**
   * Queries elasticsearch with a histogram facet.
   */
  private SearchResponse getHistogramResponse(Long interval, String segmentId, Long start, Long stop,
      List<String> consequenceTypes, List<String> projectFilters) {
    val filter = getFilter(segmentId, start, stop, consequenceTypes, projectFilters);

    val histogramFacet = histogramFacet("hf")
        .facetFilter(filter)
        .field("chromosome_start")
        .interval(interval);

    val request = client.prepareSearch(indexName)
        .setTypes(INDEX_TYPE)
        .setSearchType(QUERY_AND_FETCH)
        .setFilter(filter)
        .addFacet(histogramFacet)
        .setFrom(0)
        .setSize(0);

    logRequest(request);

    return request.execute().actionGet();
  }

  /**
   * Builds a mutation.
   */
  private static Mutation getMutation(List<String> projectFilters, JsonNode hit) throws IOException {
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
          entry.getValue().get("ssmTestedDonors")
          );
    }

    List<List<String>> consequences = newArrayList();
    for (val transcript : hit.get("transcript").get("transcript")) {
      List<String> consequence = getConsequence(
          transcript.path("consequence"),
          transcript.path("gene"));

      consequences.add(consequence);
    }

    List<String> functionalImpact = newArrayList();
    for (val fi : (ArrayNode) hit.get("functional_impact_prediction_summary")) {
      functionalImpact.add(fi.asText());
    }

    // Reference genome allele's accross ssm occurrences are same. This will be changed later so that the Reference
    // Genome Allele is at the source level instead of nested.
    val refGenAllele = hit.get("reference_genome_allele").asText();

    val mutation = Mutation.builder()
        .id(hit.path("_mutation_id").asText())
        .chromosome(hit.path("chromosome").asText())
        .start(hit.path("chromosome_start").asLong())
        .end(hit.path("chromosome_end").asLong())
        .mutationType(hit.path("mutation_type").asText())
        .mutation(hit.path("mutation").asText())
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
    String transcriptId = consequence.path("_transcript_id").isMissingNode() ? "null" :
        consequence.path("_transcript_id").asText();

    String consequenceType = consequence.path("consequence_type").isMissingNode() ? "null" :
        consequence.path("consequence_type").asText();

    String aaMutation = consequence.path("aa_mutation").isMissingNode() ? "null" :
        consequence.path("aa_mutation").asText();

    String geneSymbol = gene.path("symbol").isMissingNode() ? "null" :
        gene.path("symbol").asText();

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
   * Readability method for naming literal array of inclusion paths.
   */
  private static String[] includes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Readability method for naming literal array of exclusion paths.
   */
  private static String[] excludes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Readability method to build a list from a JsonNode returned as an array.
   */
  @SneakyThrows
  private static List<String> asList(JsonNode node) {
    return MAPPER.readValue(node.traverse(), LIST_TYPE_REFERENCE);
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static FilterBuilder getFilter(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters) {

    val filter = andFilter(
        termFilter("chromosome", segmentId),
        rangeFilter("chromosome_start").lte(stop),
        rangeFilter("chromosome_end").gte(start));

    if (consequenceTypes != null) {
      val consequenceFilter = getConsequenceFilter(consequenceTypes);
      filter.add(consequenceFilter);
    }

    if (projectFilters != null) {
      val projectFilter = getProjectFilter(projectFilters);
      filter.add(projectFilter);
    }

    return filter;
  }

  /**
   * Builds a FilterBuilder for project names.
   */
  private static FilterBuilder getProjectFilter(List<String> projects) {
    val projectFilter = orFilter();
    for (val project : projects) {
      projectFilter.add(FilterBuilders.termFilter("ssm_occurrence.project.project_name", project));
    }

    return projectFilter;
  }

  /**
   * Builds a FilterBuilder for consequence types
   */
  private static FilterBuilder getConsequenceFilter(List<String> consequenceTypes) {
    val consequenceFilter = orFilter();
    for (val consequenceType : consequenceTypes) {
      consequenceFilter.add(FilterBuilders.termFilter("transcript.consequence.consequence_type", consequenceType));
    }

    return consequenceFilter;
  }

  private static void logRequest(ActionRequestBuilder<?, ?, ?> builder) {
    String requestType = builder.request().getClass().getSimpleName();
    String message = formatRequest(builder);

    log.info("Sending {}: \n{}\n", requestType, message);
  }

}
