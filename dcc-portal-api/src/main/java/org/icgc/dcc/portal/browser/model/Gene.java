package org.icgc.dcc.portal.browser.model;

import java.util.List;

import lombok.Value;
import lombok.experimental.Builder;

@Value
@Builder
public class Gene {

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
