package org.icgc.dcc.portal.browser.model;

import java.util.List;

import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;

import com.fasterxml.jackson.databind.JsonNode;

@Value
@Builder
public class Transcript {

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

  public static Long getTranscriptStart(JsonNode trans) {
    Long transcriptStart = Long.MAX_VALUE;
    for (val exon : trans.path("exons")) {
      Long start = exon.path("start").asLong();
      if (start < transcriptStart) {
        transcriptStart = start;
      }
    }

    return transcriptStart;
  }

  public static long getTranscriptEnd(JsonNode trans) {
    Long transcriptEnd = 0l;
    for (val exon : trans.path("exons")) {
      Long end = exon.path("end").asLong();
      if (end > transcriptEnd) {
        transcriptEnd = end;
      }
    }

    return transcriptEnd;
  }

}
