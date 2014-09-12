package org.icgc.dcc.portal.browser.ds;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;

public class MutationDataSource implements AnnotationDataSource {

  private MutationParser parser;

  @Override
  public void init(Client client, String indexName) {
    this.parser = new MutationParser(client, indexName);
  }

  @Override
  public List<Object> getSegment(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String consequenceValue = queryMap.get("consequence_type");
    List<String> consequenceTypes = consequenceValue != null ? Arrays.asList(consequenceValue.split(",")) : null;

    String projectFilterValue = queryMap.get("consequence_type");
    List<String> projectFilters = projectFilterValue != null ? Arrays.asList(projectFilterValue.split(",")) : null;

    return parser.parse(segmentId, start, stop, consequenceTypes, projectFilters);
  }

  @Override
  public List<Object> getHistogramSegment(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String consequenceValue = queryMap.get("consequence_type");
    List<String> consequenceTypes = consequenceValue != null ? Arrays.asList(consequenceValue.split(",")) : null;

    String projectFilterValue = queryMap.get("consequence_type");
    List<String> projectFilters = projectFilterValue != null ? Arrays.asList(projectFilterValue.split(",")) : null;

    String intervalValue = queryMap.get("interval");
    Long interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : null;

    return parser.parseHistogram(segmentId, start, stop, interval, consequenceTypes, projectFilters);
  }

}
