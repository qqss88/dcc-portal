package org.icgc.dcc.portal.browser.model;

import lombok.Value;

@Value
public class HistogramMutation {

  private final long start;
  private final long end;
  private final int interval;
  private final long absolute;
  private final double value;

}
