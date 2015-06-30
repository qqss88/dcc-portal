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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.PROJECT;
import static org.dcc.portal.pql.meta.TypeModel.GENE_SET_ID;
import static org.dcc.portal.pql.util.Converters.asString;
import static org.dcc.portal.pql.util.Converters.isString;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.portal.pql.convert.model.Operation.ALL;
import static org.icgc.dcc.portal.pql.convert.model.Operation.HAS;
import static org.icgc.dcc.portal.pql.convert.model.Operation.IS;
import static org.icgc.dcc.portal.pql.convert.model.Operation.NOT;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.icgc.dcc.portal.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.pql.convert.model.JqlField;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

@Slf4j
public class FiltersConverter {

  private static final String GENE_PATH = "gene";
  private static final String MISSING_VALUE = "_missing";
  private static final String EMPTY_NESTED_PATH = "";
  private static final String QUERY_SEPARATOR = ",";
  private static final String NOT_TEMPLATE = "not(%s)";
  private static final String IN_TEMPLATE = "in(%s,%s)";
  private static final String EQ_TEMPLATE = "eq(%s,%s)";
  private static final String NE_TEMPLATE = "ne(%s,%s)";
  private static final String EXISTS_TEMPLATE = "exists(%s)";
  private static final String MISSING_TEMPLATE = "missing(%s)";
  private static final String NESTED_TEMPLATE = "nested(%s,%s)";

  private static final List<String> VALID_PROJECT_FILTERS = ImmutableList.of(
      "id",
      "primarySite",
      "primaryCountries",
      "availableDataTypes");

  private static final List<String> SPECIAL_FIELDS_NESTING = ImmutableList.of(
      "gene.goTermId",
      "gene.hasPathway",
      GENE_SET_ID,
      "entitySetId",
      "mutation.location",
      "gene.location");

  public String convertFilters(@NonNull JqlFilters filters, @NonNull Type indexType) {
    if (indexType == Type.PROJECT) {
      filters = cleanProjectFilters(filters);
    }

    // These fields are required to create the correct nesting order. E.g. nested(gene, nested(gene.ssm ...))
    val fieldsGrouppedByNestedPath = ArrayListMultimap.<String, JqlField> create();

    // filterKind is 'donor' in a filter {donor:{id:{is:'DO1'}}}
    for (val filterKindEntry : filters.getKindValues().entrySet()) {
      val fieldsByNestedPath = groupFieldsByNestedPath(filterKindEntry.getKey(), filterKindEntry.getValue(), indexType);
      fieldsGrouppedByNestedPath.putAll(fieldsByNestedPath);
    }

    log.debug("Fields by nested path: {}", fieldsGrouppedByNestedPath);
    val sortedDescPaths = Lists.newArrayList(Sets.newTreeSet(fieldsGrouppedByNestedPath.keySet()).descendingSet());
    log.debug("Sorted: {}", sortedDescPaths);
    val groupedPaths = groupNestedPaths(sortedDescPaths, IndexModel.getTypeModel(indexType));
    log.debug("Groupped paths: {}", groupedPaths);

    int index = groupedPaths.asMap().size();
    val result = new StringBuilder();
    for (val entry : groupedPaths.asMap().entrySet()) {
      val filter = createFilterByNestedPath(
          indexType,
          fieldsGrouppedByNestedPath,
          Lists.newArrayList(Sets.newTreeSet(entry.getValue()).descendingSet()));

      if (isEncloseWithCommonParent(entry.getValue())) {
        result.append(encloseWithCommonParent(entry.getKey(), filter));
      } else {
        result.append(filter);
      }

      // Separate converted filters with comma except the last one
      if (--index > 0) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static JqlFilters cleanProjectFilters(JqlFilters filters) {
    val kindValues = filters.getKindValues().entrySet().stream()
        .filter(k -> k.getKey().equals(PROJECT.getId()))
        .collect(toMap(k -> k.getKey(), v -> filterValidProjectFilters(v.getValue())));

    return new JqlFilters(kindValues);
  }

  private static List<JqlField> filterValidProjectFilters(List<JqlField> source) {
    return source.stream()
        .filter(f -> VALID_PROJECT_FILTERS.contains(f.getName()))
        .collect(toList());
  }

  private String encloseWithCommonParent(String commonPath, String filter) {
    return format(NESTED_TEMPLATE, commonPath, filter);
  }

  static boolean isEncloseWithCommonParent(Collection<String> nestedPaths) {
    if (nestedPaths.size() > 1 && !hasCommonParent(nestedPaths)) {
      return true;
    }

    return false;
  }

  /**
   * Checks if {@code nestedPaths} already contains a nested path which is common for all.
   */
  private static boolean hasCommonParent(Collection<String> nestedPaths) {
    val firstPath = Ordering.<String> natural().min(nestedPaths);
    for (val path : nestedPaths) {
      if (!path.startsWith(firstPath)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Groups nested paths by most common parent, so they can be nested at the parent's level.
   */
  static ListMultimap<String, String> groupNestedPaths(Collection<String> nestedPaths,
      TypeModel typeModel) {
    val result = ArrayListMultimap.<String, String> create();
    val sortedPaths = Lists.newArrayList(Sets.newTreeSet(nestedPaths));

    if (sortedPaths.size() == 1) {
      result.put(sortedPaths.get(0), sortedPaths.get(0));

      return result;
    }

    String parentPath = EMPTY_NESTED_PATH;
    for (int i = 0; i < sortedPaths.size(); i++) {
      val currentPath = sortedPaths.get(i);
      if (currentPath.equals(EMPTY_NESTED_PATH)) {
        result.put(parentPath, currentPath);
        continue;
      }

      val resolvedPath = typeModel.getParentNestedPath(currentPath);

      // DonorCentric: [gene.ssm, gene.ssm.observation]
      if (hasNextPath(i, sortedPaths.size() - 1) && sortedPaths.get(i + 1).startsWith(currentPath)
          && parentPath.equals(EMPTY_NESTED_PATH)) {
        parentPath = currentPath;

        // DonorCentric: [gene.ssm.consequence, gene.ssm.observation]
      } else if (currentPath.startsWith(resolvedPath) && hasNextPath(i, sortedPaths.size() - 1)
          && sortedPaths.get(i + 1).startsWith(resolvedPath) && parentPath.equals(EMPTY_NESTED_PATH)) {
        parentPath = resolvedPath;

        // Change from one common path to another
      } else if (currentPath.startsWith(resolvedPath) && !currentPath.startsWith(parentPath)) {
        parentPath = currentPath;

        // MutationCentric: [ssm_occurrence, transcript]
      } else if (parentPath.equals(EMPTY_NESTED_PATH)) {
        parentPath = currentPath;
      }

      result.put(parentPath, currentPath);
    }

    return result;
  }

  private static boolean hasNextPath(int i, int size) {
    return i < size;
  }

  /**
   * Creates a filter for all fields with are nested under common nested path. E.g. gene - gene.ssm -
   * gene.ssm.observation - gene.ssm.consequence
   * 
   * @param sortedDescPaths - descending sorted paths. E.g. gene.ssm - gene
   */
  static String createFilterByNestedPath(Type indexType, ListMultimap<String, JqlField> sortedFields,
      List<String> sortedDescPaths) {
    val filterStack = new Stack<String>();
    for (int i = 0; i < sortedDescPaths.size(); i++) {
      val nestedPath = sortedDescPaths.get(i);
      val filter = createTypeFilter(sortedFields.get(nestedPath), indexType);

      val isFirstFilter = i == 0;
      if (isFirstFilter) {
        if (isNestFilter(nestedPath, indexType)) {
          filterStack.push(format(NESTED_TEMPLATE, resolveNestedPath(nestedPath, indexType), filter));
        } else {
          filterStack.push(filter);
        }
      } else {
        val prevFilter = filterStack.pop();
        if (isNestFilter(nestedPath, indexType)) {
          if (isChildNesting(nestedPath, sortedDescPaths.get(i - 1))) {
            filterStack.push(format("nested(%s,and(%s,%s))", resolveNestedPath(nestedPath, indexType), prevFilter,
                filter));
          } else {
            filterStack.push(format("nested(%s,%s),%s", nestedPath, filter, prevFilter));
          }
        } else {
          filterStack.push(format("%s,%s", filter, prevFilter));
        }
      }
    }

    return filterStack.pop();
  }

  /**
   * gene, gene.ssm - true<br>
   * transcript, gene - false
   */
  private static boolean isChildNesting(String parentPath, String childPath) {
    return childPath.startsWith(parentPath);
  }

  private static String resolveNestedPath(String nestedPath, Type indexType) {
    if (nestedPath.equals(GENE_PATH)) {
      switch (indexType) {
      case DONOR_CENTRIC:
        return "gene";
      case MUTATION_CENTRIC:
        return "transcript";
      }
    }

    return nestedPath;
  }

  /**
   * Defines if filter should be nested because it's filtering on a nested field
   */
  private static boolean isNestFilter(String nestedPath, Type indexType) {
    return !(nestedPath.equals(EMPTY_NESTED_PATH));
  }

  private static String createTypeFilter(Collection<JqlField> fields, Type indexType) {
    val result = new StringBuilder();
    val lastField = getLast(fields);

    for (val jqlField : fields) {
      result.append(createFilter(jqlField, indexType));
      if (!jqlField.equals(lastField)) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  /**
   * Groups fields by the most common nested path.<br>
   * E.g. gene, gene.ssm, gene.ssm.consequence, gene.ssm.observation are in one bucket with key 'gene'
   */
  static ListMultimap<String, JqlField> groupFieldsByNestedPath(String typePrefix,
      List<JqlField> fields, Type indexType) {
    val result = ArrayListMultimap.<String, JqlField> create();
    if (isTypeMatch(typePrefix, indexType)) {
      result.putAll(EMPTY_NESTED_PATH, fields);

      return result;
    }

    // Because of the particularity of MutationCentric type some of its fields are remapped to nested fields
    // The fields must be correctly separated before the next steps.
    if (indexType == MUTATION_CENTRIC && typePrefix.equals("mutation")) {
      val nonNestedFields = fields.stream()
          .filter((JqlField f) -> !f.getName().endsWith("Nested"))
          .collect(Collectors.toList());
      result.putAll(EMPTY_NESTED_PATH, nonNestedFields);

      fields = fields.stream()
          .filter((JqlField f) -> f.getName().endsWith("Nested"))
          .collect(Collectors.toList());
    }

    val typeModel = IndexModel.getTypeModel(indexType);
    for (val field : fields) {
      result.put(resolveNestedPath(field, typeModel), field);
    }

    return result;
  }

  private static String resolveNestedPath(JqlField field, TypeModel indexModel) {
    val fieldName = parseFieldName(field);
    if (SPECIAL_FIELDS_NESTING.contains(fieldName) || SPECIAL_FIELDS_NESTING.contains(field.getName())) {
      return resolveSpecialCasesNestedPath(fieldName, indexModel.getType());
    }

    if (indexModel.isNested(fieldName)) {
      return indexModel.getNestedPath(fieldName);
    }

    return EMPTY_NESTED_PATH;
  }

  private static String resolveSpecialCasesNestedPath(String fieldName, Type type) {
    switch (type) {
    case DONOR_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "gene";
      }

      if (fieldName.equals("donor.entitySetId")) {
        return EMPTY_NESTED_PATH;
      }

      return "gene.ssm";

    case GENE_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return EMPTY_NESTED_PATH;
      }

      if (fieldName.equals("donor.entitySetId")) {
        return "donor";
      }

      return "donor.ssm";

    case MUTATION_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "transcript";
      }

      if (fieldName.equals("donor.entitySetId")) {
        return "ssm_occurrence";
      }

      return EMPTY_NESTED_PATH;

    case OBSERVATION_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "ssm.consequence";
      }

      return "ssm";
    }

    throw new IllegalArgumentException(format("Could not resolve nested path for field %s in type %s", fieldName,
        type.getId()));
  }

  private static boolean isTypeMatch(String typePrefix, Type indexType) {
    // Does not apply to MUTATION_CENTRIC, because its filters must be separated on nested and non-nested
    // See comment @ line 186 groupFieldsByNestedPath()
    if (indexType == MUTATION_CENTRIC) {
      return false;
    }

    return indexType.getId().startsWith(typePrefix);
  }

  private static String createFilter(JqlField jqlField, Type indexType) {
    if (jqlField.getOperation() == HAS) {
      val typeModel = IndexModel.getTypeModel(indexType);
      val fieldName = typeModel.getField(jqlField.getName());

      return resolveMissingFilter(fieldName, jqlField);
    }

    if (jqlField.getOperation() == ALL) {
      return resolveAllFilter(jqlField);
    }

    if (jqlField.getValue().contains(MISSING_VALUE)) {
      return createMissingFilter(jqlField, indexType);
    }

    val filterType = createFilterByValueType(jqlField);
    if (jqlField.getOperation() == NOT && jqlField.getValue().isArray()) {
      return (format(NOT_TEMPLATE, filterType));
    }

    return filterType;
  }

  private static String resolveAllFilter(JqlField jqlField) {
    val values = createAllFilters(jqlField);

    return format("and(%s)", COMMA.join(values));
  }

  private static List<String> createAllFilters(JqlField jqlField) {
    val fieldName = parseFieldName(jqlField);
    val values = (JqlArrayValue) jqlField.getValue();
    val result = ImmutableList.<String> builder();

    for (val rawValue : values.get()) {
      val value = isString(rawValue) ? asString(rawValue) : rawValue;
      result.add(format(EQ_TEMPLATE, fieldName, value));
    }

    return result.build();
  }

  private static String resolveMissingFilter(String fieldName, JqlField jqlField) {
    return jqlField.getValue().get() == Boolean.TRUE ?
        format(EXISTS_TEMPLATE, fieldName) :
        format(MISSING_TEMPLATE, fieldName);
  }

  private static String createMissingFilter(JqlField jqlField, Type indexType) {
    val fieldName = parseFieldName(jqlField);

    if (jqlField.getValue().isArray()) {
      // FIXME: assumes the operation is 'IS'
      val arrayFilter = createArrayFilterForMissingField(jqlField);
      val missingFilter = format(MISSING_TEMPLATE, fieldName);

      return arrayFilter.isPresent() ? format("or(%s,%s)", missingFilter, arrayFilter.get()) : missingFilter;
    }

    return jqlField.getValue().get() == Boolean.FALSE ?
        format(EXISTS_TEMPLATE, fieldName) :
        format(MISSING_TEMPLATE, fieldName);
  }

  private static Optional<String> createArrayFilterForMissingField(JqlField jqlField) {
    val values = Lists.newArrayList(((JqlArrayValue) jqlField.getValue()).get());
    values.remove(MISSING_VALUE);

    if (values.isEmpty()) {
      return Optional.empty();
    }

    val newJqlValue = new JqlArrayValue(values);
    val newJqlField = new JqlField(jqlField.getName(), jqlField.getOperation(), newJqlValue, jqlField.getPrefix());

    return Optional.of(createInFilter(newJqlField));
  }

  private static String createFilterByValueType(JqlField jqlField) {
    return jqlField.getValue().isArray() ? createInFilter(jqlField) : createEqFilter(jqlField);
  }

  private static String createEqFilter(JqlField jqlField) {
    val fieldValue = jqlField.getValue().get();
    val value = isString(fieldValue) ? asString(fieldValue) : fieldValue;

    return jqlField.getOperation() == IS ?
        format(EQ_TEMPLATE, parseFieldName(jqlField), value) :
        format(NE_TEMPLATE, parseFieldName(jqlField), value);
  }

  private static String createInFilter(JqlField jqlField) {
    val arrayField = (JqlArrayValue) jqlField.getValue();

    return format(IN_TEMPLATE, parseFieldName(jqlField), arrayField.textValue());
  }

  private static String parseFieldName(JqlField jqlField) {
    if (jqlField.getName().endsWith("Nested")) {
      return jqlField.getName();
    }

    return format("%s.%s", jqlField.getPrefix(), jqlField.getName());
  }

}
