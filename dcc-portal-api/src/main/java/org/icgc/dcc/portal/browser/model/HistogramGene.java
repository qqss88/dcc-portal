package org.icgc.dcc.portal.browser.model;

import lombok.Value;

@Value
public class HistogramGene {

  private final long start;
  private final long end;
  private final int interval;
  private final long absolute;
  private final double value;

}
