/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

public final class BrowserParsers {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.reader();
  private static final String EXON_ID_SEPERATOR = ".";
  private static final TypeReference<List<String>> LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {};

  /**
   * Build a fully completed list of Mutation objects.
   */
  @SneakyThrows
  public static List<Object> parseMutations(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters, SearchResponse searchResponse) {

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
   * Build a fully completed list of Gene objects.
   */
  @SneakyThrows
  public static List<Object> parseGenes(String segmentId, Long start, Long stop, List<String> biotypes,
      boolean withTranscripts, SearchResponse searchResponse) {
    String json = searchResponse.toString();
    JsonNode response = READER.readTree(json);

    int geneId = 1;
    List<Object> genes = newArrayList();
    for (JsonNode hit : response.get("hits").get("hits")) {
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
   * Build a histogram representation of mutations.
   */
  public static List<Object> parseHistogramMutation(String segmentId, Long start, Long stop, Long interval,
      List<String> consequenceTypes, List<String> projectFilters, SearchResponse searchResponse) {
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
   * Build a histogram representation of genes.
   */
  public static List<Object> parseHistogramGene(String segmentId, Long start, Long stop, Long interval,
      List<String> biotypes,
      SearchResponse searchResponse) {
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

  private static Long getTranscriptStart(JsonNode trans) {
    Long transcriptStart = Long.MAX_VALUE;
    for (val exon : trans.path("exons")) {
      Long start = exon.path("start").asLong();
      if (start < transcriptStart) {
        transcriptStart = start;
      }
    }

    return transcriptStart;
  }

  private static long getTranscriptEnd(JsonNode trans) {
    Long transcriptEnd = 0l;
    for (val exon : trans.path("exons")) {
      Long end = exon.path("end").asLong();
      if (end > transcriptEnd) {
        transcriptEnd = end;
      }
    }

    return transcriptEnd;
  }

  /**
   * Readability method to build a list from a JsonNode returned as an array.
   */
  @SneakyThrows
  private static List<String> asList(JsonNode node) {
    return MAPPER.readValue(node.traverse(), LIST_TYPE_REFERENCE);
  }

  @Value
  @Builder
  private static class Exon {

    private String stableId;
    private String chromosome;
    private String start;
    private String end;
    private String strand;
  }

  @Value
  @Builder
  private static class ExonToTranscript {

    private Integer exonToTranscriptId;
    private Integer genomicCodingStart;
    private Integer genomicCodingEnd;
    private Integer cdnaCodingStart;
    private Integer cdnaCodingEnd;
    private Integer cdnaStart;
    private Integer cdnaEnd;
    private Integer phase;
    private Integer isConstitutive;
    private Exon exon;
  }

  @Value
  @Builder
  private static class Gene {

    private Integer geneId;
    private String stableId;
    private String externalName;
    private String externalDb;
    private String biotype;
    private String status;
    private String chromosome;
    private Long start;
    private Long end;
    private String strand;
    private String source;
    private String description;
    private List<Transcript> transcripts;

  }

  @Value
  private static class HistogramMutation {

    private final long start;
    private final long end;
    private final int interval;
    private final long absolute;
    private final double value;
  }

  @Value
  private static class HistogramGene {

    private final long start;
    private final long end;
    private final int interval;
    private final long absolute;
    private final double value;
  }

  @Value
  @Builder
  private static class Mutation {

    private String id;
    private String chromosome;
    private Long start;
    private Long end;
    private String mutationType;
    private String mutation;
    private String refGenAllele;
    private Integer total;
    private List<String> projectInfo;
    private List<List<String>> consequences;
    private List<String> functionalImpact;
  }

  @Value
  @Builder
  private static class Transcript {

    private Integer transcriptId;
    private String stableId;
    private String externalName;
    private String externalDb;
    private String biotype;
    private String status;
    private String chromosome;
    private Long start;
    private Long end;
    private String strand;
    private Long codingRegionStart;
    private Long codingRegionEnd;
    private Long cdnaCodingStart;
    private Long cdnaCodingEnd;
    private String description;
    private List<ExonToTranscript> exonToTranscripts;
  }
}