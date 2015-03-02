package org.icgc.dcc.portal.browser.model;

import lombok.Value;
import lombok.Builder;

@Value
@Builder
public class Exon {

  private String stableId;

  private String chromosome;

  private String start;

  private String end;

  private String strand;

}
