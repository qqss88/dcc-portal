package org.icgc.dcc.portal.browser.model;

import java.util.List;

import lombok.Value;
import lombok.Builder;

@Value
@Builder
public class Mutation {

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
