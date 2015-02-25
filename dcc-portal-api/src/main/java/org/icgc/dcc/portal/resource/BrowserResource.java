package org.icgc.dcc.portal.resource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;

import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.browser.ds.AnnotationDataSource;
import org.icgc.dcc.portal.browser.model.DataSource;
import org.icgc.dcc.portal.service.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.ApiOperation;
import com.yammer.metrics.annotation.Timed;

/**
 * Genome browser endpoints that mimic the Cellbase API style.
 */
@Component
@Path("/browser")
@Produces(TEXT_PLAIN)
@Setter
public class BrowserResource {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper().setSerializationInclusion(NON_NULL);
  private static final String CHROMOSOME_LOCATION_SEPERATOR = ":";
  private static final String LOCATION_SEPERATOR = "-";
  private static final long WITH_TRANSCRIPT_SEGMENT_RANGE_MAX = 10 * 1000 * 1000;
  private static final long EXCLUDE_TRANSCRIPT_SEGMENT_RANGE_MAX = 20 * 1000 * 1000;

  /**
   * Configuration
   */
  @Value("#{indexName}")
  private String indexName;
  @Resource
  private List<DataSource> dataSources;

  /**
   * State.
   */
  @Autowired
  private Client client;

  /**
   * Request state.
   */
  private Map<String, String> queryMap = newHashMap();

  /**
   * Handles requests for gene objects
   */
  @GET
  @Path("/gene")
  @Timed
  @ApiOperation(value = "Retrieves a list of genes")
  public String getGene(

      @QueryParam("segment") String segment,

      @QueryParam("histogram") String histogram,

      @QueryParam("dataType") String dataType,

      @QueryParam("interval") String interval,

      @QueryParam("resource") String resource,

      @QueryParam("biotype") String bioType,

      @QueryParam("consequence_type") String consequenceType) {
    return getData(segment, histogram, dataType, interval, resource, bioType, consequenceType);
  }

  /**
   * Handles requests for mutation objects.
   */
  @GET
  @Path("/mutation")
  @Timed
  @ApiOperation(value = "Retrieves a list of mutations")
  public String getMutation(

      @QueryParam("segment") String segment,

      @QueryParam("histogram") String histogram,

      @QueryParam("dataType") String dataType,

      @QueryParam("interval") String interval,

      @QueryParam("resource") String resource,

      @QueryParam("biotype") String bioType,

      @QueryParam("consequence_type") String consequenceType) {
    return getData(segment, histogram, dataType, interval, resource, bioType, consequenceType);
  }

  /**
   * Common method used to retrieve data of all types.
   */
  @SneakyThrows
  String getData(String segment, String histogram, String dataType,
      String interval, String resource, String bioType, String consequenceType) {
    queryMap.put("segment", segment);
    queryMap.put("histogram", histogram);
    queryMap.put("dataType", dataType);
    queryMap.put("interval", interval);
    queryMap.put("resource", resource);
    queryMap.put("biotype", bioType);
    queryMap.put("consequence_type", consequenceType);

    AnnotationDataSource dataSource = newInstance(queryMap.get("resource"));

    boolean hist = histogram != null && "true".equals(histogram);
    if (hist) {
      List<Object> result = getHistogram(dataSource);
      return MAPPER.writeValueAsString(result);
    } else {
      List<List<Object>> result = getRecords(dataSource);
      return MAPPER.writeValueAsString(result);
    }
  }

  /**
   * Instantiates a data source depending on the value of the resource URL parameter.
   */
  @SneakyThrows
  AnnotationDataSource newInstance(String sourceName) {
    AnnotationDataSource dataSource = null;
    for (DataSource ds : dataSources) {
      if (ds.getUri().equals(sourceName)) {
        Class<?> dataSourceClass = Class.forName(ds.getClassName());
        dataSource = (AnnotationDataSource) dataSourceClass.newInstance();
        dataSource.init(client, indexName);
        return dataSource;
      }
    }

    return dataSource;
  }

  /**
   * Retrieves histogram.
   */
  List<Object> getHistogram(AnnotationDataSource dataSource) {
    String segmentRegion = queryMap.get("segment");
    String interval = queryMap.get("interval");
    if (nullToEmpty(interval).isEmpty()) {
      throw new BadRequestException("Histogram request requires interval");
    }

    String chromosome = segmentRegion.split(CHROMOSOME_LOCATION_SEPERATOR)[0];
    long start = Long.parseLong(segmentRegion.split(CHROMOSOME_LOCATION_SEPERATOR)[1].split(LOCATION_SEPERATOR)[0]);
    long stop = Long.parseLong(segmentRegion.split(CHROMOSOME_LOCATION_SEPERATOR)[1].split(LOCATION_SEPERATOR)[1]);

    return dataSource.getHistogramSegment(chromosome, start, stop, queryMap);
  }

  /**
   * Retrieves complete data.
   */
  @SneakyThrows
  List<List<Object>> getRecords(AnnotationDataSource dataSource) {
    String[] segmentRegions = queryMap.get("segment").split(",");

    List<List<Object>> result = newArrayList();
    for (int i = 0; i < segmentRegions.length; i++) {
      String chromosome = segmentRegions[i].split(CHROMOSOME_LOCATION_SEPERATOR)[0];

      long start =
          Long.parseLong(segmentRegions[i].split(CHROMOSOME_LOCATION_SEPERATOR)[1].split(LOCATION_SEPERATOR)[0]);
      long stop =
          Long.parseLong(segmentRegions[i].split(CHROMOSOME_LOCATION_SEPERATOR)[1].split(LOCATION_SEPERATOR)[1]);

      // validateSegmentRange(start, stop);

      List<Object> segment = dataSource.getSegment(chromosome, start, stop, queryMap);

      result.add(segment);
    }

    return result;
  }

  /**
   * Ensures the requested range isn't going to exceed available memory.
   * 
   * @param start - segment start
   * @param stop - segment end
   */
  // TODO: When requirements are stable
  @SuppressWarnings("unused")
  private void validateSegmentRange(long start, long stop) {
    val histogram = nullToEmpty(queryMap.get("histogram")).equals("true");
    if (histogram) {
      return;
    }

    val transcripts = nullToEmpty(queryMap.get("dataType")).equals("withTranscripts");

    long max = transcripts ? WITH_TRANSCRIPT_SEGMENT_RANGE_MAX : EXCLUDE_TRANSCRIPT_SEGMENT_RANGE_MAX;
    long length = stop - start;
    if (length > max) {
      val message = format("Chromosome segment range of '%s' bp exceeds configured maximum of '%s' bp", length, max);
      throw new BadRequestException(message);
    }
  }

}
