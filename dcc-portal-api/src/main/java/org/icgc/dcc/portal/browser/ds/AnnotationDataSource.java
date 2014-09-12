package org.icgc.dcc.portal.browser.ds;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;

public interface AnnotationDataSource {

  /**
   * Intermediate method to get complete structure of the requested object
   * 
   * @param chromosome
   * @param start
   * @param stop
   * @param queryMap
   * @return
   * @throws IOException
   */
  public List<Object> getSegment(String chromosome, Long start, Long stop, Map<String, String> queryMap)
      throws IOException;

  /**
   * Intermediate method to get histogram representation of the requested object
   * 
   * @param chromosome
   * @param start
   * @param stop
   * @param queryMap
   * @return
   */
  public List<Object> getHistogramSegment(String chromosome, Long start, Long stop, Map<String, String> queryMap);

  /**
   * Method called to initialize class variables
   * 
   * @param client
   * @param indexName
   */
  public void init(Client client, String indexName);

}
