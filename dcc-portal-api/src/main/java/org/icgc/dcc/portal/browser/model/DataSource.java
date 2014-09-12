package org.icgc.dcc.portal.browser.model;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class DataSource {

  @JsonProperty
  String uri;

  @JsonProperty
  String className;

}
