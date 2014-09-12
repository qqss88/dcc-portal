package org.icgc.dcc.portal.browser.ds;

import static com.google.common.base.Strings.nullToEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.val;

import org.elasticsearch.client.Client;

public class GeneDataSource implements AnnotationDataSource {

  private GeneParser parser;

  @Override
  public void init(Client client, String indexName) {
    parser = new GeneParser(client, indexName);
  }

  @Override
  public List<Object> getSegment(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String biotypeValue = queryMap.get("biotype");
    List<String> biotypes = biotypeValue != null ? Arrays.asList(biotypeValue.split(",")) : null;

    val withTranscripts = nullToEmpty(queryMap.get("dataType")).equals("withTranscripts");

    return parser.parse(segmentId, start, stop, biotypes, withTranscripts);
  }

  @Override
  public List<Object> getHistogramSegment(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String biotypeValue = queryMap.get("biotype");
    List<String> biotypes = biotypeValue != null ? Arrays.asList(biotypeValue.split(",")) : null;

    String intervalValue = queryMap.get("interval");
    Long interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : null;

    return parser.parseHistogram(segmentId, start, stop, interval, biotypes);
  }

}
