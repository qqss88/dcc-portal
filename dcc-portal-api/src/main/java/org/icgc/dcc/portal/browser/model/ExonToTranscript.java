package org.icgc.dcc.portal.browser.model;

import lombok.Value;
import lombok.Builder;

@Value
@Builder
public class ExonToTranscript {

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
