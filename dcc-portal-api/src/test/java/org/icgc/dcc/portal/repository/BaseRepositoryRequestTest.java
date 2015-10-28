/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.repository;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.apache.commons.lang.StringUtils.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.mockito.Mockito.when;

import java.io.File;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

/**
 * Base class that adds support for executing test that verify the content and structure of requests sent by repository
 * implementations.
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseRepositoryRequestTest {

  /**
   * Allow for more liberal JSON strings to simplify literals with constants, etc.
   */
  static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(INDENT_OUTPUT)
      .configure(ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(ALLOW_SINGLE_QUOTES, true)
      .configure(ALLOW_COMMENTS, true);

  protected static String DONOR_FILTER =
      "'donor':{'primarySite':{'is':['Brain']},'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}";

  protected static String PROJECT_FILTER =
      "'project':{'primarySite':{'is':['Brain']}}";

  protected static String GENE_FILTER =
      "'gene':{'type':{'is':['protein_coding']},'list':{'is':['Cancer Gene Census']}}";

  protected static String GENE_PATHWAY_FILTER =
      "'gene':{'pathwayId':{'is':['REACT_150361']},'type':{'is':['protein_coding']},'list':{'is':['Cancer Gene Census']}}";

  protected static String PATHWAY_FILTER = "'gene':{'pathwayId':{'is':['REACT_150361']}}";

  protected static String MUTATION_FILTER =
      "'mutation':{'type':{'is':['substitution']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  protected static String MUTATION_CONSEQUENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'consequenceType':{'is':['missense']},'functionalImpact':{'is':['High']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  protected static String CONSEQUENCE_FILTER =
      "'mutation':{'consequenceType':{'is':['missense']},'functionalImpact':{'is':['High']}}";

  protected static final String FILTER_BOOL_MUST = "/post_filter/bool/must";

  protected static final String BOOL_MUST_R = "{\"bool\":{\"must\":[";

  protected static final String NESTED_FILTER_BOOL_MUST_R = "{\"nested\":{\"filter\":{\"bool\":{\"must\":";

  protected static final String BOOL_MUST = "{\"bool\":{\"must\":";

  protected static final String donorTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.DONOR;
    return "{\"terms\":{\"" + getField("primarySite", kind, prefixMapping) + "\":[\"Brain\"]}},"
        + "{\"terms\":{\"" + getField("gender", kind, prefixMapping) + "\":[\"male\"]}},"
        + "{\"terms\":{\"" + getField("availableDataTypes", kind, prefixMapping) + "\":[\"cnsm\"]}}]}}";
  }

  protected static final String geneTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.GENE;
    return "{\"terms\":{\"" + getField("type", kind, prefixMapping) + "\":[\"protein_coding\"]}},"
        + "{\"terms\":{\"" + getField("list", kind, prefixMapping) + "\":[\"Cancer Gene Census\"]}}]}}";
  }

  protected static final String pathwayTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.PATHWAY;
    return "{\"terms\":{\"" + getField("id", kind, prefixMapping) + "\":[\"REACT_150361\"]}}";
  }

  protected static final String mutationTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.MUTATION;
    return "{\"terms\":{\"" + getField("type", kind, prefixMapping) + "\":[\"substitution\"]}}}}";
  }

  protected static final String observationTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.OBSERVATION;
    return "{\"terms\":{\"" + getField("platform", kind, prefixMapping) + "\":[\"Illumina GA sequencing\"]}}," +
        "{\"terms\":{\"" + getField("verificationStatus", kind, prefixMapping) + "\":[\"not tested\"]}}";
  }

  protected static final String consequenceTerms(ImmutableMap<Kind, String> prefixMapping) {
    val kind = Kind.CONSEQUENCE;
    return "{\"terms\":{\"" + getField("type", kind, prefixMapping) + "\":[\"missense\"]}},"
        + "{\"terms\":{\"" + getField("functionalImpact", kind, prefixMapping) + "\":[\"High\"]}}]}}";
  }

  protected static final String getField(String fieldName, Kind kind, ImmutableMap<Kind, String> prefixMapping) {
    String f = FIELDS_MAPPING.get(kind).get(fieldName);
    if (prefixMapping != null && prefixMapping.containsKey(kind)) {
      f = String.format("%s.%s", prefixMapping.get(kind), f);
    }
    return f;
  }

  @Spy
  final IndexModel indexModel = new IndexModel("test-index", "icgc-repository");

  @Mock
  Client client;

  @Before
  public void setUp() {
    val searchRequestBuilder = new SearchRequestBuilder(client);
    when(client.prepareSearch("test-index")).thenReturn(searchRequestBuilder);
  }

  public String joinFilters(String... filters) {
    return "{" + join(filters, ",") + "}";
  }

  protected void assertFilter(ObjectNode request, String expected) {
    assertThat(request.at(FILTER_BOOL_MUST).toString()).isEqualTo(expected);
  }

  protected void assertScore(String request, String expected) {
    assertThat(request).isEqualTo(expected);
  }

  /**
   * Utility method that returns a {@code JsonNode} given a JSON String.
   * <p>
   * The name and use is inspired by jQuery's {@code $} function.
   * 
   * @param json the JSON string to parse
   * @return the parsed {@code ObjectNode}
   */
  @SneakyThrows
  static ObjectNode $(String json) {
    return MAPPER.readValue(json, ObjectNode.class);
  }

  /**
   * Utility method that returns a {@code JsonNode} given a JSON String.
   * <p>
   * The name and use is inspired by jQuery's {@code $} function.
   * 
   * @param jsonFile the file to parse
   * @return the parsed {@code ObjectNode}
   */
  @SneakyThrows
  static ObjectNode $(File jsonFile) {
    return MAPPER.readValue(jsonFile, ObjectNode.class);
  }

  /**
   * Converts the supplied {@code request} to an {@code ObjectNode}.
   * 
   * @param request the request to convert
   * @return the converted {@code ObjectNode}
   */
  @SneakyThrows
  static ObjectNode $(SearchRequestBuilder request) {
    return $(request.toString());
  }

  /**
   * Converts the supplied {@code request} to an {@code ObjectNode}.
   * 
   * @param request the request to convert
   * @return the converted {@code ObjectNode}
   */
  static ObjectNode $(FilterBuilder request) {
    return $(request.toString());
  }

  /**
   * Converts the supplied {@code request} to an {@code ObjectNode}.
   * 
   * @param request the request to convert
   * @return the converted {@code ObjectNode}
   */
  static ObjectNode $(NestedQueryBuilder request) {
    return $(request.toString());
  }

  /**
   * Converts the supplied {@code object} to a prettified JSON string.
   * 
   * @param object the object to convert
   * @return the prettified JSON string
   */
  @SneakyThrows
  static String convertToString(Object object) {
    return MAPPER.writeValueAsString(object);
  }

  /**
   * Utility to reduce call-site noise and add uninteresting, but required, parameters.
   */
  static org.icgc.dcc.portal.model.Query.QueryBuilder query() {
    return Query.builder()
        .order("desc")
        .from(1)
        .size(10);
  }

  static void log(ObjectNode filters, ObjectNode request) {
    log.info("Filters:");
    log.info(convertToString(filters));
    log.info("Request:");
    log.info(convertToString(request));
  }

}
