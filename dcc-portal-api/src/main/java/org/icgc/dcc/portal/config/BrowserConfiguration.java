package org.icgc.dcc.portal.config;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

import org.icgc.dcc.portal.browser.model.DataSource;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@ToString
public class BrowserConfiguration {

  @JsonProperty
  private List<DataSource> dataSources;

}
