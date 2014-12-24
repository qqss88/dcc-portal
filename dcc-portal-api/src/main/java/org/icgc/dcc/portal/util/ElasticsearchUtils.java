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
package org.icgc.dcc.portal.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.model.Query;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provides methods to retrieve values from SearchHit
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ElasticsearchUtils {

  private static final String FLATTEN_MAP_DEFAULT_SEPARATOR = ".";
  private static final String FLATTEN_MAP_FIRST_LEVEL_SEPARATOR = "";
  private static final String FLATTEN_MAP_FIRST_LEVEL_PREFIX = "";

  /**
   * Returns first value of the list as a String
   */
  public static String getString(Object values) {
    if (values == null) return null;

    if (values instanceof String) {
      return (String) values;
    }

    @SuppressWarnings("unchecked")
    val resultList = (List<Object>) values;

    return (String) resultList.get(0);
  }

  /**
   * Removes inner fields of <code>prefix</code> object. Otherwise, only explicitly defined fields are returned by
   * Elasticsearch.
   */
  public static List<String> removeDuplicateFields(List<String> fieldsList, String prefix) {
    if (fieldsList == null) return null;
    val result = Lists.<String> newArrayList();

    for (val field : fieldsList) {
      if (!field.startsWith(prefix)) {
        result.add(field);
      }
    }
    result.add(prefix);

    return result;
  }

  public static Long getLong(Object value) {
    // FIXME: some duplicates are here
    if (value instanceof Long) return (Long) value;
    else if (value instanceof Integer) return (long) (Integer) value;
    else if (value instanceof Float) return ((Float) value).longValue();
    else if (value instanceof List) {
      val newValue = ((List<Object>) value).get(0);
      if (newValue instanceof Long) return (Long) newValue;
      if (newValue instanceof Float) return ((Float) newValue).longValue();
      if (newValue instanceof Integer) return (long) (Integer) newValue;

      return null;
    }
    else
      return null;
  }

  public static Boolean getBoolean(Object values) {
    if (values == null) return null;

    if (values instanceof Boolean) {
      return (Boolean) values;
    }

    @SuppressWarnings("unchecked")
    val resultList = (List<Object>) values;

    return (Boolean) resultList.get(0);
  }

  /**
   * Transforms a map returned by Elasticsearch which may contain nested maps to "flat" map where internal maps are
   * flattened with prefix equal to field name of the map.
   */
  public static Map<String, Object> flattenFieldsMap(Map<String, Object> sourceMap) {
    return flattenFieldsMap(FLATTEN_MAP_FIRST_LEVEL_PREFIX, FLATTEN_MAP_FIRST_LEVEL_SEPARATOR, sourceMap);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> flattenFieldsMap(String prefix, String separator, Object object) {
    val sourceMap = (Map<String, Object>) object;
    // Requires a mutable map because MutationRepository.findOne() does fields re-mapping by adding/removing map's
    // entities
    val resultMap = Maps.<String, Object> newHashMap();

    for (val key : sourceMap.keySet()) {
      val value = sourceMap.get(key);
      if (value instanceof Map) {
        resultMap.putAll(flattenFieldsMap(prefix + separator + key, FLATTEN_MAP_DEFAULT_SEPARATOR, value));
      } else {
        resultMap.put(prefix + separator + key, value);
      }
    }

    return resultMap;
  }

  public static void processIncludes(GetRequestBuilder search, Query query) {
    val sourceFields = prepareIncludes(query);

    String[] excludeFields = null;
    search.setFetchSource(sourceFields.toArray(new String[sourceFields.size()]), excludeFields);
  }

  public static void processIncludes(SearchRequestBuilder search, Query query) {
    val sourceFields = prepareIncludes(query);

    String[] excludeFields = null;
    search.setFetchSource(sourceFields.toArray(new String[sourceFields.size()]), excludeFields);
  }

  private static List<String> prepareIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("transcripts") || query.hasInclude("consequences")) {
      sourceFields.add("transcript");
    }

    if (query.hasInclude("occurrences")) {
      sourceFields.add("ssm_occurrence");
    }

    return sourceFields;
  }

  public static void addResponseIncludes(Query query, GetResponse response, Map<String, Object> map) {
    if (!query.getIncludes().isEmpty()) {
      map.putAll(response.getSource());
      processConsequences(map, query);
    }
  }

  public static void addResponseIncludes(Query query, SearchHit searchHit, Map<String, Object> map) {
    if (!query.getIncludes().isEmpty()) {
      map.putAll(searchHit.getSource());
      processConsequences(map, query);
    }
  }

  private static void processConsequences(Map<String, Object> map, Query query) {
    if (query.hasInclude("consequences")) {
      log.info("Copying transcripts to consequences...");
      map.put("consequences", map.get("transcript"));
      if (!query.hasInclude("transcripts")) {
        log.info("Removing transcripts...");
        map.remove("transcript");
      }
    }
  }

}
