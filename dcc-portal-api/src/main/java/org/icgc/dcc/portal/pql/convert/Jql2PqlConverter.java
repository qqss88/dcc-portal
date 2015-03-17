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

import org.dcc.portal.pql.meta.Type;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;

/**
 * Converts JSON-like queries to PQL ones.
 */
@Slf4j
public class Jql2PqlConverter {

  private final static FiltersConverter FILTERS_CONVERTER = new FiltersConverter();
  private final static ObjectMapper mapper = createObjectMapper();
  private final static String SEPARATOR = ",";

  private final static String LIMIT_NO_FROM_TEMPLATE = "limit(%d)";
  private final static String LIMIT_TEMPLATE = "limit(%d,%d)";

  private final static String SORT_TEMPLATE = "sort(%s%s)";
  private final static String ASC_SORT = "+";
  private final static String DESC_SORT = "-";

  public String convert(@NonNull Query query, Type type) {
    val result = new StringBuilder();
    boolean hasPreviousClause = false;

    val combinedFields = Lists.<String> newArrayList();
    if (query.getIncludes() != null) {
      val includes = Lists.newArrayList(query.getIncludes());
      if (includes.contains("facets")) {
        includes.remove("facets");
        result.append(parseFacets(type));
      }

      combinedFields.addAll(includes);
    }

    if (query.hasFields()) {
      combinedFields.addAll(query.getFields());
    }

    // Have facets been included?
    if (result.length() != 0) {
      result.append(SEPARATOR);
    }

    result.append(parseFields(combinedFields));
    hasPreviousClause = true;

    if (query.hasFilters()) {
      if (hasPreviousClause) {
        result.append(SEPARATOR);
      }

      result.append(convertFilters(query.getFilters().toString()));
      hasPreviousClause = true;
    }

    if (query.hasScoreFilters()) {
      if (hasPreviousClause) {
        result.append(SEPARATOR);
      }

      result.append(convertFilters(query.getScoreFilters().toString()));
      hasPreviousClause = true;
    }

    val sort = query.getSort();
    if (sort != null && !sort.isEmpty()) {
      val order = query.getOrder();
      checkState(order != null, "The query is missing sort order");
      if (hasPreviousClause) {
        result.append(SEPARATOR);
      }

      result.append(parseSort(sort, order));
    }

    if (query.getSize() > 0) {
      if (hasPreviousClause) {
        result.append(SEPARATOR);
      }

      // TODO: implement limit
      checkState(query.getLimit() == null, "Limit is not implemented");
      val from = query.getFrom();
      val size = query.getSize();
      checkState(from < size, "From must be greater then size. From: {}, Size: {}", from, size);
      result.append(from > 0 ? format(LIMIT_TEMPLATE, from, size) : format(LIMIT_NO_FROM_TEMPLATE, size));
    }

    // FIXME: implement
    checkState(query.getScore() == null && query.getQuery() == null, "Not implemented");

    return result.toString();
  }

  private String parseFacets(Type type) {
    return "facets(*)";
  }

  private static String parseSort(String field, SortOrder order) {
    return order == SortOrder.ASC ?
        format(SORT_TEMPLATE, ASC_SORT, field) :
        format(SORT_TEMPLATE, DESC_SORT, field);
  }

  private static String parseFields(List<String> fields) {
    if (fields.isEmpty()) {
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
  private static String convertFilters(@NonNull String jqlFilters) {
    val filtersEntry = mapper.readValue(jqlFilters, JqlFilters.class);
    log.debug("Parsed JQL filters: {}", filtersEntry);

    return FILTERS_CONVERTER.convertFilters(filtersEntry);
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
