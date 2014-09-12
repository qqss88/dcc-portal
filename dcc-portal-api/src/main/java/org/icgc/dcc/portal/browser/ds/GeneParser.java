package org.icgc.dcc.portal.browser.ds;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.elasticsearch.action.search.SearchType.QUERY_AND_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.andFilter;
import static org.elasticsearch.index.query.FilterBuilders.orFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.search.facet.FacetBuilders.histogramFacet;
import static org.icgc.dcc.portal.browser.model.Transcript.getTranscriptEnd;
import static org.icgc.dcc.portal.browser.model.Transcript.getTranscriptStart;
import static org.icgc.dcc.portal.util.FormatUtils.formatRequest;

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
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;
import org.icgc.dcc.portal.browser.model.Exon;
import org.icgc.dcc.portal.browser.model.ExonToTranscript;
import org.icgc.dcc.portal.browser.model.Gene;
import org.icgc.dcc.portal.browser.model.HistogramGene;
import org.icgc.dcc.portal.browser.model.Transcript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Slf4j
@RequiredArgsConstructor
public class GeneParser {

  /**
   * Constants.
   */
  private static final String TYPE_NAME = "gene-centric";
  private static final ObjectReader READER = new ObjectMapper().reader();
  private static final String EXON_ID_SEPERATOR = ".";
  private static final int MAX_HIT_COUNT = 10000;

  /**
   * Parser state.
   */
  @NonNull
  private final Client client;
  @NonNull
  private final String indexName;

  /**
   * Build a fully completed list of Gene objects.
   */
  @SneakyThrows
  public List<Object> parse(String segmentId, Long start, Long stop, List<String> biotypes, boolean withTranscripts) {
    val searchResponse = getResponse(segmentId, start, stop, biotypes, withTranscripts, MAX_HIT_COUNT);
    String json = searchResponse.toString();
    JsonNode response = READER.readTree(json);

    int geneId = 1;
    List<Object> genes = newArrayList();
    for (JsonNode hit : embeddedHit(response.get("hits"), "hits")) {
      JsonNode fields = hit.path("fields");

      List<Transcript> transcripts = withTranscripts ? getTranscript(fields) : null;

      genes.add(Gene.builder()
          .geneId(geneId)
          .stableId(fields.path("_gene_id").asText())
          .externalName(fields.path("name").asText())
          .biotype(fields.path("biotype").asText())
          .chromosome(fields.path("chromosome").asText())
          .start(fields.path("start").asLong())
          .end(fields.path("end").asLong())
          .strand(fields.path("strand").asText())
          .description(fields.path("description").asText())
          .transcripts(transcripts)
          .build());

      geneId++;
    }

    return genes;
  }

  /**
   * Build a histogram representation of genes.
   */
  public List<Object> parseHistogram(String segmentId, Long start, Long stop, Long interval, List<String> biotypes) {
    val searchResponse = getHistogramResponse(interval, segmentId, start, stop, biotypes);
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

    List<Object> genes = newArrayList();
    int intervalNumber = 0;
    long intervalStart = 0;
    long intervalStop = intervalStart + interval - 1;
    while (intervalStart < stop) {
      if (intervalStop >= start) {
        Entry entry = entries.get(intervalStart);
        val geneCount = entry != null ? entry.getCount() : 0;

        val gene = new HistogramGene(
            intervalStart,
            intervalStop,
            intervalNumber,
            geneCount,
            (double) geneCount / highestAbsolute);

        genes.add(gene);

        intervalNumber++;
      }

      // Advance
      intervalStart += interval;
      intervalStop += interval;
    }

    return genes;
  }

  /**
   * Queries elasticsearch for appropriate data.
   */
  private SearchResponse getResponse(String segmentId, Long start, Long stop, List<String> biotypes,
      boolean withTranscripts, Integer size) {
    val filter = getFilter(segmentId, start, stop, biotypes);

    val request = client.prepareSearch(indexName)
        .setTypes(TYPE_NAME)
        .setSearchType(QUERY_AND_FETCH)
        .addFields(
            "_gene_id",
            "name",
            "biotype",
            "chromosome",
            "start",
            "end",
            "strand",
            "description")
        .setFilter(filter)
        .setFrom(0)
        .setSize(size);

    if (withTranscripts) {
      request.addField("transcripts");
    }

    logRequest(request);

    return request.execute().actionGet();
  }

  /**
   * Queries elasticsearch with a histogram facet.
   */
  private SearchResponse getHistogramResponse(Long interval, String segmentId, Long start, Long stop,
      List<String> biotypes) {
    val filter = getFilter(segmentId, start, stop, biotypes);

    val histogramFacet = histogramFacet("hf")
        .facetFilter(filter)
        .field("start")
        .interval(interval);

    val request = client.prepareSearch(indexName)
        .setTypes(TYPE_NAME)
        .setSearchType(QUERY_AND_FETCH)
        .setFilter(filter)
        .addFacet(histogramFacet)
        .setSize(0);

    logRequest(request);

    return request.execute().actionGet();
  }

  /**
   * Build a fully completed list of Transcript objects.
   */
  private static List<Transcript> getTranscript(JsonNode hit) {
    List<Transcript> transcripts = newArrayList();
    Integer transcriptId = 1;
    for (val trans : hit.path("transcripts")) {

      List<ExonToTranscript> exonToTranscripts = getExonToTranscripts(hit, trans);
      long transcriptStart = getTranscriptStart(trans);
      long transcriptEnd = getTranscriptEnd(trans);

      Transcript transcript = Transcript.builder()
          .transcriptId(transcriptId)
          .stableId(trans.path("id").asText())
          .externalName(trans.path("name").asText())
          .biotype(trans.path("biotype").asText())
          .chromosome(trans.path("chromosome").asText())
          .start(transcriptStart)
          .end(transcriptEnd)
          .strand(hit.path("strand").asText())
          .codingRegionStart(trans.path("coding_region_start").asLong())
          .codingRegionEnd(trans.path("coding_region_end").asLong())
          .cdnaCodingStart(trans.path("cdna_coding_start").asLong())
          .cdnaCodingEnd(trans.path("cdna_coding_end").asLong())
          .exonToTranscripts(exonToTranscripts).build();

      transcripts.add(transcript);
      transcriptId++;
    }

    return transcripts;
  }

  /**
   * Build a fully completed list of ExonToTranscript objects.
   */
  private static List<ExonToTranscript> getExonToTranscripts(JsonNode hit, JsonNode trans) {
    List<ExonToTranscript> exonToTranscripts = newArrayList();
    int exonId = 1;
    for (val exon : trans.path("exons")) {
      ExonToTranscript exonToTranscript = ExonToTranscript.builder()
          .exonToTranscriptId(exonId)
          .genomicCodingStart(exon.path("genomic_coding_start").asInt())
          .genomicCodingEnd(exon.path("genomic_coding_end").asInt())
          .cdnaCodingStart(exon.path("cdna_coding_start").asInt())
          .cdnaCodingEnd(exon.path("cdna_coding_end").asInt())
          .cdnaStart(exon.path("cdna_start").asInt())
          .cdnaEnd(exon.path("cdna_end").asInt())
          .exon(Exon.builder()
              .stableId(trans.path("id").asText() + EXON_ID_SEPERATOR + exonId)
              .chromosome(hit.path("chromosome").asText())
              .start(exon.path("start").asText())
              .end(exon.path("end").asText())
              .strand(hit.path("strand").asText())
              .build())
          .build();

      exonToTranscripts.add(exonToTranscript);
      exonId++;
    }

    return exonToTranscripts;
  }

  /**
   * Readability method for extracting embedded objects as a reuslt of partial fields.
   */
  private static JsonNode embeddedHit(JsonNode hit, String fieldName) {
    return hit.get(fieldName);
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static FilterBuilder getFilter(String segmentId, Long start, Long stop, List<String> biotypes) {
    val filter = andFilter(
        termFilter("chromosome", segmentId),
        rangeFilter("start").lte(stop),
        rangeFilter("end").gte(start));

    if (biotypes != null) {
      val biotypeFilter = getBiotypeFilterBuilder(biotypes);
      filter.add(biotypeFilter);
    }

    return filter;
  }

  /**
   * Readability method to build list of biotype filters.
   */
  private static FilterBuilder getBiotypeFilterBuilder(List<String> biotypes) {
    val biotypeFilter = orFilter();
    for (val biotype : biotypes) {
      biotypeFilter.add(termFilter("biotype", biotype));
    }

    return biotypeFilter;
  }

  private static void logRequest(ActionRequestBuilder<?, ?, ?> builder) {
    String requestType = builder.request().getClass().getSimpleName();
    String message = formatRequest(builder);

    log.info("Sending {}: \n{}\n", requestType, message);
  }

}
