package org.icgc.dcc.portal.resource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.elasticsearch.client.Client;
import org.icgc.dcc.common.core.model.ChromosomeLocation;
import org.icgc.dcc.portal.browser.ds.AnnotationDataSource;
import org.icgc.dcc.portal.browser.model.DataSource;
import org.icgc.dcc.portal.service.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.wordnik.swagger.annotations.ApiOperation;
import com.yammer.metrics.annotation.Timed;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;

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
  private static final long WITH_TRANSCRIPT_SEGMENT_RANGE_MAX = 10 * 1000 * 1000;
  private static final long EXCLUDE_TRANSCRIPT_SEGMENT_RANGE_MAX = 20 * 1000 * 1000;

  private static final class ParameterNames {

    private static final String SEGMENT = "segment";
    private static final String HISTOGRAM = "histogram";
    private static final String DATATYPE = "dataType";
    private static final String INTERVAL = "interval";
    private static final String RESOURCE = "resource";
    private static final String BIOTYPE = "biotype";
    private static final String CONSEQUENCE_TYPE = "consequence_type";

  }

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
   * Handles requests for gene objects
   */
  @GET
  @Path("/gene")
  @Timed
  @ApiOperation(value = "Retrieves a list of genes")
  public String getGene(
      @QueryParam(ParameterNames.SEGMENT) String segment,
      @QueryParam(ParameterNames.HISTOGRAM) String histogram,
      @QueryParam(ParameterNames.DATATYPE) String dataType,
      @QueryParam(ParameterNames.INTERVAL) String interval,
      @QueryParam(ParameterNames.RESOURCE) String resource,
      @QueryParam(ParameterNames.BIOTYPE) String bioType,
      @QueryParam(ParameterNames.CONSEQUENCE_TYPE) String consequenceType) {
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
      @QueryParam(ParameterNames.SEGMENT) String segment,
      @QueryParam(ParameterNames.HISTOGRAM) String histogram,
      @QueryParam(ParameterNames.DATATYPE) String dataType,
      @QueryParam(ParameterNames.INTERVAL) String interval,
      @QueryParam(ParameterNames.RESOURCE) String resource,
      @QueryParam(ParameterNames.BIOTYPE) String bioType,
      @QueryParam(ParameterNames.CONSEQUENCE_TYPE) String consequenceType) {
    return getData(segment, histogram, dataType, interval, resource, bioType, consequenceType);
  }

  /**
   * Common method used to retrieve data of all types.
   */
  @SneakyThrows
  String getData(String segment, String histogram, String dataType,
      String interval, String resource, String bioType, String consequenceType) {
    checkRequest(isBlank(resource), "'resource' parameter is required but missing.");

    val queryMap = Maps.<String, String> newHashMap();
    queryMap.put(ParameterNames.SEGMENT, segment);
    queryMap.put(ParameterNames.HISTOGRAM, histogram);
    queryMap.put(ParameterNames.DATATYPE, dataType);
    queryMap.put(ParameterNames.INTERVAL, interval);
    queryMap.put(ParameterNames.RESOURCE, resource);
    queryMap.put(ParameterNames.BIOTYPE, bioType);
    queryMap.put(ParameterNames.CONSEQUENCE_TYPE, consequenceType);

    val dataSource = newInstance(resource);
    val isHistogram = histogram != null && "true".equals(histogram);

    return MAPPER
        .writeValueAsString(isHistogram ? getHistogram(dataSource, queryMap) : getRecords(dataSource, queryMap));
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
  List<Object> getHistogram(AnnotationDataSource dataSource, Map<String, String> queryMap) {
    val errorMessage = "Histogram request requires '%s' parameter.";

    val interval = queryMap.get(ParameterNames.INTERVAL);
    checkRequest(isBlank(interval), errorMessage, ParameterNames.INTERVAL);
    checkRequest(Doubles.tryParse(interval) == null, "Bad value '%s' for '%s'", interval, ParameterNames.INTERVAL);
    checkRequest(Doubles.tryParse(interval) <= 0, "Historgram requires %s > 0", ParameterNames.INTERVAL);

    val segmentParameter = ParameterNames.SEGMENT;
    val segmentRegion = queryMap.get(segmentParameter);
    checkRequest(isBlank(segmentRegion), errorMessage, segmentParameter);

    ChromosomeLocation chromosome = null;
    try {
      chromosome = ChromosomeLocation.parse(segmentRegion);
    } catch (Exception e) {
      val message = "Value of the '" + segmentParameter +
          "' parameter (" + segmentRegion + ") is not valid. Reason: " + e.getMessage();
      throw new BadRequestException(message);
    }

    return dataSource.getHistogramSegment(chromosome.getChromosome().getName(),
        Long.valueOf(chromosome.getStart()),
        Long.valueOf(chromosome.getEnd()),
        queryMap);
  }

  /**
   * Retrieves complete data.
   */
  @SneakyThrows
  List<List<Object>> getRecords(AnnotationDataSource dataSource, Map<String, String> queryMap) {
    val segmentParameter = ParameterNames.SEGMENT;
    val errorMessage = "'%s' parameter is required but missing.";

    val segmentRegion = queryMap.get(segmentParameter);
    checkRequest(isBlank(segmentRegion), errorMessage, segmentParameter);

    List<List<Object>> result = newArrayList();

    for (val chromosomeString : segmentRegion.split(",")) {
      ChromosomeLocation chromosome = null;

      try {
        chromosome = ChromosomeLocation.parse(chromosomeString);
      } catch (Exception e) {
        val message = "Value of the '" + segmentParameter +
            "' parameter (" + segmentRegion + ") is not valid. Reason: " + e.getMessage();
        throw new BadRequestException(message);
      }

      result.add(dataSource.getSegment(chromosome.getChromosome().getName(),
          Long.valueOf(chromosome.getStart()),
          Long.valueOf(chromosome.getEnd()),
          queryMap));
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
  private void validateSegmentRange(long start, long stop, Map<String, String> queryMap) {
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