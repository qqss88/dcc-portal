package org.icgc.dcc.portal.browser.ds;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.portal.browser.model.Transcript.getTranscriptEnd;
import static org.icgc.dcc.portal.browser.model.Transcript.getTranscriptStart;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;
import org.icgc.dcc.portal.browser.model.Exon;
import org.icgc.dcc.portal.browser.model.ExonToTranscript;
import org.icgc.dcc.portal.browser.model.Gene;
import org.icgc.dcc.portal.browser.model.HistogramGene;
import org.icgc.dcc.portal.browser.model.Transcript;
import org.icgc.dcc.portal.repository.BrowserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
@Service
public class GeneParser {

  @NonNull
  private BrowserRepository browserRepository;

  /**
   * Constants.
   */
  private static final ObjectReader READER = new ObjectMapper().reader();
  private static final String EXON_ID_SEPERATOR = ".";
  private static final int MAX_HIT_COUNT = 10000;

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

      List<Transcript> transcripts = withTranscripts ? getTranscript(hit) : null;

      genes.add(Gene.builder()
          .geneId(geneId)
          .stableId(fields.path("_gene_id").get(0).asText())
          .externalName(fields.path("name").get(0).asText())
          .biotype(fields.path("biotype").get(0).asText())
          .chromosome(fields.path("chromosome").get(0).asText())
          .start(fields.path("start").get(0).asLong())
          .end(fields.path("end").get(0).asLong())
          .strand(fields.path("strand").get(0).asText())
          .description(fields.path("description").isMissingNode() ? "" : fields.path("description").get(0).asText())
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
   * EnrichmentQueries elasticsearch for appropriate data.
   */
  private SearchResponse getResponse(String segmentId, Long start, Long stop, List<String> biotypes,
      boolean withTranscripts, Integer size) {
    return browserRepository.getGene(segmentId, start, stop, biotypes, withTranscripts, size);
  }

  /**
   * EnrichmentQueries elasticsearch with a histogram facet.
   */
  private SearchResponse getHistogramResponse(Long interval, String segmentId, Long start, Long stop,
      List<String> biotypes) {
    return browserRepository.getGeneHistogram(interval, segmentId, start, stop, biotypes);
  }

  /**
   * Build a fully completed list of Transcript objects.
   */
  private static List<Transcript> getTranscript(JsonNode hit) {
    List<Transcript> transcripts = newArrayList();
    Integer transcriptId = 1;
    val fields = hit.path("fields");
    for (val trans : hit.path("_source").path("transcripts")) {

      List<ExonToTranscript> exonToTranscripts = getExonToTranscripts(fields, trans);
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
          .strand(fields.path("strand").get(0).asText())
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
  private static List<ExonToTranscript> getExonToTranscripts(JsonNode fields, JsonNode trans) {
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
              .chromosome(fields.path("chromosome").get(0).asText())
              .start(exon.path("start").asText())
              .end(exon.path("end").asText())
              .strand(fields.path("strand").get(0).asText())
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
}