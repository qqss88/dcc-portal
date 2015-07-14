/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.pql.convert;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.util.List;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.ast.builder.PqlBuilders;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.PqlParser;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Converts JSON-like queries to PQL ones.
 */
@Slf4j
public class Jql2PqlConverter {

  private final static Jql2PqlConverter INSTANCE = new Jql2PqlConverter();

  private final static FiltersConverter FILTERS_CONVERTER = new FiltersConverter();
  private final static ObjectMapper MAPPER = createObjectMapper();
  private final static String SEPARATOR = ",";

  private final static String LIMIT_NO_FROM_TEMPLATE = "limit(%d)";
  private final static String LIMIT_TEMPLATE = "limit(%d,%d)";

  private final static String SORT_TEMPLATE = "sort(%s%s)";
  private final static String ASC_SORT = "+";
  private final static String DESC_SORT = "-";

  public String convert(@NonNull Query query, @NonNull Type type) {
    val result = new StringBuilder();
    result.append(parseFields(query.getFields()));

    if (query.getIncludes() != null && !query.getIncludes().isEmpty()) {
      result.append(SEPARATOR);
      result.append(parseIncludes(query.getIncludes()));
    }

    if (query.hasFilters()) {
      val filters = remapFilters(query.getFilters(), type);
      val convertedFilters = convertFilters(filters.toString(), type);

      // After the cleaning project filters may get empty
      if (!convertedFilters.isEmpty()) {
        result.append(SEPARATOR);
        result.append(convertedFilters);
      }
    }

    // if (query.hasScoreFilters()) {
    // result.append(SEPARATOR);
    // result.append(convertFilters(query.getScoreFilters().toString(), type));
    // }

    val sort = query.getSort();
    if (sort != null && !sort.isEmpty()) {
      val order = query.getOrder();
      checkState(order != null, "The query is missing sort order");
      result.append(SEPARATOR);
      result.append(parseSort(sort, order));
    }

    if (query.getSize() > 0) {
      result.append(SEPARATOR);

      // TODO: implement limit
      checkState(query.getLimit() == null, "Limit is not implemented");
      val from = query.getFrom();
      val size = query.getSize();
      result.append(from > 0 ? format(LIMIT_TEMPLATE, from, size) : format(LIMIT_NO_FROM_TEMPLATE, size));
    }

    // FIXME: implement
    checkState(query.getScore() == null && query.getQuery() == null, "Not implemented");

    return result.toString();
  }

  public String convertCount(@NonNull Query query, @NonNull Type type) {
    val pql = this.convert(query, type);
    val pqlNode = PqlParser.parse(pql);

    log.info("Input pql {}", pql);
    log.info("PQL Node {}", pqlNode);
    log.info("PQL Node filters {}", pqlNode.getFilters());

    val root = PqlBuilders.count().build();
    if (query.getFilters().elements().hasNext()) {
      log.info(" +++ {}", query.getFilters());
      root.setFilters(pqlNode.getFilters());
    }

    log.info("Converted count PQL query: {}", root.toString());

    return root.toString();
  }

  private static ObjectNode remapFilters(ObjectNode filters, Type type) {
    if (type != Type.MUTATION_CENTRIC) {
      return filters;
    }

    /*
     * if (filters.has("mutation")) { val mutation = (ObjectNode) filters.get("mutation"); if
     * (mutation.has("consequenceType")) { mutation.put("consequenceTypeNested", mutation.remove("consequenceType")); }
     * 
     * if (mutation.has("platform")) { mutation.put("platformNested", mutation.remove("platform")); }
     * 
     * if (mutation.has("sequencingStrategy")) { mutation.put("sequencingStrategyNested",
     * mutation.remove("sequencingStrategy")); }
     * 
     * if (mutation.has("verificationStatus")) { mutation.put("verificationStatusNested",
     * mutation.remove("verificationStatus")); }
     * 
     * if (mutation.has("functionalImpact")) { mutation.put("functionalImpactNested",
     * mutation.remove("functionalImpact")); } }
     */

    return filters;
  }

  private static String parseIncludes(List<String> queryIncludes) {
    val includes = Lists.newArrayList(queryIncludes);
    val result = new StringBuilder();
    if (includes.contains("facets")) {
      includes.remove("facets");
      result.append(parseFacets());
    }

    if (!includes.isEmpty()) {
      // Added facets
      if (result.length() != 0) {
        result.append(SEPARATOR);
      }

      result.append(parseFields(includes));
    }

    return result.toString();
  }

  public static Jql2PqlConverter getInstance() {
    return INSTANCE;
  }

  private static String parseFacets() {
    return "facets(*)";
  }

  private static String parseSort(String field, SortOrder order) {
    return order == SortOrder.ASC ?
        format(SORT_TEMPLATE, ASC_SORT, field) :
        format(SORT_TEMPLATE, DESC_SORT, field);
  }

  private static String parseFields(List<String> fields) {
    if (fields == null || fields.isEmpty()) {
      return "select(*)";
    }

    val result = new StringBuilder();
    result.append("select(");
    for (int i = 0; i < fields.size(); i++) {
      result.append(fields.get(i));
      if (i != fields.size() - 1) {
        result.append(",");
      }
    }

    result.append(")");

    return result.toString();
  }

  @SneakyThrows
  private static String convertFilters(@NonNull String jqlFilters, Type indexType) {
    val filtersEntry = MAPPER.readValue(jqlFilters, JqlFilters.class);
    log.debug("Parsed JQL filters: {}", filtersEntry);

    return FILTERS_CONVERTER.convertFilters(filtersEntry, indexType);
  }

  private static ObjectMapper createObjectMapper() {
    return registerJqlDeserializer(new ObjectMapper());
  }

  private static ObjectMapper registerJqlDeserializer(ObjectMapper mapper) {
    val module = new SimpleModule();
    module.addDeserializer(JqlFilters.class, new JqlFiltersDeserializer());
    mapper.registerModule(module);

    return mapper;
  }

}
