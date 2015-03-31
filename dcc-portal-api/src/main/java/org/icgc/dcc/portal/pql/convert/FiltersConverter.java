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

import static com.google.common.collect.Iterables.getLast;
import static java.lang.String.format;
import static org.icgc.dcc.portal.pql.convert.model.JqlValue.asString;
import static org.icgc.dcc.portal.pql.convert.model.JqlValue.isString;
import static org.icgc.dcc.portal.pql.convert.model.Operation.IS;
import static org.icgc.dcc.portal.pql.convert.model.Operation.NOT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.pql.convert.model.JqlField;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.pql.convert.model.Operation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
public class FiltersConverter {

  private static final String MISSING_VALUE = "_missing";
  private static final String EMPTY_NESTED_PATH = "";
  private final static String QUERY_SEPARATOR = ",";
  private final static String NOT_TEMPLATE = "not(%s)";
  private final static String IN_TEMPLATE = "in(%s,%s)";
  private final static String EQ_TEMPLATE = "eq(%s,%s)";
  private final static String NE_TEMPLATE = "ne(%s,%s)";
  private static final String EXISTS_TEMPLATE = "exists(%s)";
  private static final String MISSING_TEMPLATE = "missing(%s)";

  private static final Map<String, String> SPECIAL_FIELDS_NESTING = ImmutableMap.of(
      "goTermId", "",
      "hasPathway", "",
      "location", "");

  public String convertFilters(JqlFilters filters, Type indexType) {
    val sortedFields = ArrayListMultimap.<String, JqlField> create();
    val prefixByNestedPath = Maps.<String, String> newHashMap();

    for (val type : filters.getTypeValues().entrySet()) {
      val fieldsByNestedPath = groupFieldsByNestedPath(type.getKey(), type.getValue(), indexType);
      sortedFields.putAll(fieldsByNestedPath);
      prefixByNestedPath.putAll(createPrefixByNestedPath(type.getKey(), fieldsByNestedPath.keySet()));
    }

    log.debug("Fields by nested path: {}", sortedFields);
    val sortedDescPaths = Lists.newArrayList(Sets.newTreeSet(sortedFields.keySet()).descendingSet());
    log.debug("Sorted: {}", sortedDescPaths);
    val groupedPaths = groupNestedPaths(sortedDescPaths);
    log.debug("Groupped paths: {}", groupedPaths);

    int index = groupedPaths.asMap().size();
    val result = new StringBuilder();
    for (val entry : groupedPaths.asMap().entrySet()) {
      val res =
          createFilterByNestingPath(indexType, sortedFields, prefixByNestedPath,
              Lists.newArrayList(Sets.newTreeSet(entry.getValue()).descendingSet()));
      result.append(res);

      if (--index > 0) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static ArrayListMultimap<String, String> groupNestedPaths(Collection<String> sortedDescPaths) {
    val result = ArrayListMultimap.<String, String> create();
    for (val path : Sets.newTreeSet(sortedDescPaths)) {
      val tokens = path.split("\\.");
      val prefix = tokens[0];

      result.put(prefix, path);
    }

    return result;
  }

  private static String createFilterByNestingPath(Type indexType, ArrayListMultimap<String, JqlField> sortedFields,
      Map<String, String> prefixByNestedPath, List<String> sortedDescPaths) {

    val filterStack = new Stack<String>();
    for (int i = 0; i < sortedDescPaths.size(); i++) {
      val nestedPath = sortedDescPaths.get(i);
      val filter = createTypeFilter(prefixByNestedPath.get(nestedPath), sortedFields.get(nestedPath), indexType);

      if (i == 0) {
        if (nestedPath.equals(EMPTY_NESTED_PATH)) {
          filterStack.push(filter);
        } else {
          filterStack.push(format("nested(%s,%s)", nestedPath, filter));
        }

      } else {
        val prevFilter = filterStack.pop();
        if (nestedPath.equals(EMPTY_NESTED_PATH)) {
          filterStack.push(format("%s,%s", prevFilter, filter));
        } else {
          filterStack.push(format("nested(%s,and(%s,%s))", nestedPath, prevFilter, filter));
        }

      }
    }

    return filterStack.pop();
  }

  private Map<String, String> createPrefixByNestedPath(String prefix, Set<String> nestedPaths) {
    val result = new ImmutableMap.Builder<String, String>();
    for (val path : nestedPaths) {
      result.put(path, prefix);
    }

    return result.build();
  }

  private static String convertType(Entry<String, Collection<JqlField>> fieldsByType, Type indexType) {
    log.debug("Converting type '{}' {}", fieldsByType.getKey(), fieldsByType.getValue());
    val result = new StringBuilder();

    val typePrefix = fieldsByType.getKey();
    val fieldsByNestedPath = groupFieldsByNestedPath(typePrefix, fieldsByType.getValue(), indexType);
    log.debug("Fields by nested path: {}", fieldsByNestedPath);

    val lastEntry = getLast(fieldsByNestedPath.asMap().entrySet());
    for (val entry : fieldsByNestedPath.asMap().entrySet()) {
      if (entry.getKey().equals(EMPTY_NESTED_PATH)) {
        result.append(createTypeFilter(typePrefix, entry.getValue(), indexType));
      } else {
        result.append(createNestedFilter(typePrefix, entry.getKey(), entry.getValue(), indexType));
      }

      if (!entry.equals(lastEntry)) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static String createTypeFilter(String typePrefix, Collection<JqlField> fields, Type indexType) {
    val result = new StringBuilder();
    val lastField = getLast(fields);

    for (val jqlField : fields) {
      result.append(createFilter(typePrefix, jqlField, indexType));
      if (!jqlField.equals(lastField)) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static String createNestedFilter(String typePrefix, String nestedPath, Collection<JqlField> fields, Type type) {
    val result = new StringBuilder();
    result.append(format("nested(%s,", nestedPath));
    result.append(createTypeFilter(typePrefix, fields, type));
    result.append(")");

    return result.toString();
  }

  private static ArrayListMultimap<String, JqlField> groupFieldsByNestedPath(String typePrefix,
      Collection<JqlField> fields, Type indexType) {

    val result = ArrayListMultimap.<String, JqlField> create();
    if (isTypeMatch(typePrefix, indexType)) {
      result.putAll(EMPTY_NESTED_PATH, fields);

      return result;
    }

    val indexModel = IndexModel.getTypeModel(indexType);
    for (val field : fields) {
      String nestedPath = "";
      if (SPECIAL_FIELDS_NESTING.containsKey(field.getName())) {
        nestedPath = SPECIAL_FIELDS_NESTING.get(field.getName());
      } else if (indexModel.isNested(parseFieldName(typePrefix, field))) {
        nestedPath = indexModel.getNestedPath(parseFieldName(typePrefix, field));
      }

      result.put(nestedPath, field);
    }

    return result;
  }

  private static boolean isTypeMatch(String typePrefix, Type indexType) {
    return indexType.getId().startsWith(typePrefix);
  }

  private static String createFilter(String typePrefix, JqlField jqlField, Type indexType) {
    if (jqlField.getOperation() == Operation.HAS) {
      val typeModel = IndexModel.getTypeModel(indexType);
      val fieldName = typeModel.getField(jqlField.getName());

      return resolveMissingFilter(fieldName, jqlField);
    }

    if (jqlField.getValue().contains(MISSING_VALUE)) {
      return createMissingFilter(typePrefix, jqlField, indexType);
    }

    val filterType = createFilterByValueType(typePrefix, jqlField);
    if (jqlField.getOperation() == NOT && jqlField.getValue().isArray()) {
      return (format(NOT_TEMPLATE, filterType));
    }

    return (filterType);
  }

  private static String resolveMissingFilter(String fieldName, JqlField jqlField) {
    return jqlField.getValue().get() == Boolean.TRUE ?
        format(EXISTS_TEMPLATE, fieldName) :
        format(MISSING_TEMPLATE, fieldName);
  }

  private static String createMissingFilter(String typePrefix, JqlField jqlField, Type indexType) {
    val fieldName = parseFieldName(typePrefix, jqlField);

    if (jqlField.getValue().isArray()) {
      // FIXME: assumes the operation is 'IS'
      val arrayFilter = createArrayFilterForMissingField(typePrefix, jqlField);
      val missingFilter = format(MISSING_TEMPLATE, fieldName);

      return arrayFilter.isPresent() ? format("or(%s,%s)", missingFilter, arrayFilter.get()) : missingFilter;
    }

    return jqlField.getValue().get() == Boolean.FALSE ?
        format(EXISTS_TEMPLATE, fieldName) :
        format(MISSING_TEMPLATE, fieldName);
  }

  private static Optional<String> createArrayFilterForMissingField(String typePrefix, JqlField jqlField) {
    val values = Lists.newArrayList(((JqlArrayValue) jqlField.getValue()).get());
    values.remove(MISSING_VALUE);

    if (values.isEmpty()) {
      return Optional.empty();
    }

    val newJqlValue = new JqlArrayValue(values);
    val newJqlField = new JqlField(jqlField.getName(), jqlField.getOperation(), newJqlValue);

    return Optional.of(createInFilter(typePrefix, newJqlField));
  }

  private static String createFilterByValueType(String prefix, JqlField jqlField) {
    return jqlField.getValue().isArray() ? createInFilter(prefix, jqlField) : createEqFilter(prefix, jqlField);
  }

  private static String createEqFilter(String prefix, JqlField jqlField) {
    val fieldValue = jqlField.getValue().get();
    val value = isString(fieldValue) ? asString(fieldValue) : fieldValue;

    return jqlField.getOperation() == IS ?
        format(EQ_TEMPLATE, parseFieldName(prefix, jqlField), value) :
        format(NE_TEMPLATE, parseFieldName(prefix, jqlField), value);
  }

  private static String createInFilter(String prefix, JqlField jqlField) {
    val arrayField = (JqlArrayValue) jqlField.getValue();

    return format(IN_TEMPLATE, parseFieldName(prefix, jqlField), arrayField.valuesToString());
  }

  private static String parseFieldName(String prefix, JqlField jqlField) {
    return format("%s.%s", prefix, jqlField.getName());
  }

}
