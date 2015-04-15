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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Provides methods to retrieve values from SearchHit
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ElasticsearchResponseUtils {

  /**
   * Returns first value of the list as a String
   */
  @SuppressWarnings("unchecked")
  public static String getString(Object values) {
    if (values == null) return null;

    if (values instanceof List<?>) {
      return ((List<String>) values).get(0);
    }

    if (values instanceof String) {
      return (String) values;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public static Long getLong(Object value) {
    if (value instanceof List) value = ((List<Object>) value).get(0);
    if (value instanceof Long) return (Long) value;
    if (value instanceof Float) return ((Float) value).longValue();
    if (value instanceof Integer) return (long) (Integer) value;

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

  public static void addResponseIncludes(Query query, GetResponse response, Map<String, Object> map) {
    if (query.getIncludes() != null && !query.getIncludes().isEmpty()) {
      map.putAll(response.getSource());
      processConsequences(map, query);
    }
  }

  public static void addResponseIncludes(Query query, SearchHit searchHit, Map<String, Object> map) {
    if (query.getIncludes() != null && !query.getIncludes().isEmpty()) {
      val source = searchHit.getSource();
      if (source == null) {
        return;
      }

      map.putAll(searchHit.getSource());
      processConsequences(map, query);
    }
  }

  private static void processConsequences(Map<String, Object> map, Query query) {
    if (query.hasInclude("consequences")) {
      log.debug("Copying transcripts to consequences...");
      map.put("consequences", map.get("transcript"));
      if (!query.hasInclude("transcripts")) {
        log.debug("Removing transcripts...");
        map.remove("transcript");
      }
    }
  }

  public static Map<String, Object> createMapFromSearchFields(Map<String, SearchHitField> fields) {
    val result = Maps.<String, Object> newHashMap();
    for (val field : fields.entrySet()) {
      result.put(field.getKey(), field.getValue().getValues());
    }

    return result;
  }

  public static Map<String, Object> createMapFromGetFields(Map<String, GetField> fields) {
    val result = Maps.<String, Object> newHashMap();
    for (val field : fields.entrySet()) {
      result.put(field.getKey(), field.getValue().getValues());
    }

    return result;
  }

  public static Map<String, Object> createResponseMap(GetResponse response, Query query) {
    val map = createMapFromGetFields(response.getFields());
    addResponseIncludes(query, response, map);

    return map;
  }

  public static void checkResponseState(String id, GetResponse response, Kind kind) {
    if (!response.isExists()) {
      val type = kind.getId().substring(0, 1).toUpperCase() + kind.getId().substring(1);
      log.info("{} {} not found.", type, id);

      val message = format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(Response.status(NOT_FOUND).entity(message).build());
    }
  }

  public static void processSource(Map<String, Object> source, Map<String, Object> fields) {
    if (source == null) return;

    fields.putAll(source);
  }

  public static Map<String, Object> flatternMap(Map<String, Object> source) {
    if (source == null) {
      return emptyMap();
    }

    return flatternMap(Optional.empty(), source);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> flatternMap(Optional<String> prefix, Map<String, Object> source) {
    val result = ImmutableMap.<String, Object> builder();

    for (val entry : source.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.putAll(flatternMap(Optional.of(entry.getKey()), (Map<String, Object>) entry.getValue()));
      } else {
        result.put(resolvePrefix(prefix, entry.getKey()), entry.getValue());
      }
    }

    return result.build();
  }

  @SuppressWarnings("unchecked")
  private static boolean isNestedList(Object value) {
    if (value instanceof List) {
      val list = (List<Object>) value;
      if (!list.isEmpty() && list.get(0) instanceof List) {
        checkState(list.size() == 1, format("Expected that the parent list would contain only one List child, but its "
            + "size is '%s'", list.size()));

        return true;
      }
    }

    return false;
  }

  private static String resolvePrefix(Optional<String> prefix, String field) {
    return prefix.isPresent() ? format("%s.%s", prefix.get(), field) : field;
  }
}
