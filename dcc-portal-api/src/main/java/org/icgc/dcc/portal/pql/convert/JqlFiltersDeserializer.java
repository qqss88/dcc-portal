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

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.icgc.dcc.portal.model.IndexModel.Type.DONOR;
import static org.icgc.dcc.portal.model.IndexModel.Type.GENE;
import static org.icgc.dcc.portal.model.IndexModel.Type.MUTATION;
import static org.icgc.dcc.portal.model.IndexModel.Type.PROJECT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.pql.convert.model.JqlField;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.pql.convert.model.JqlSingleValue;
import org.icgc.dcc.portal.pql.convert.model.JqlValue;
import org.icgc.dcc.portal.pql.convert.model.Operation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class JqlFiltersDeserializer extends JsonDeserializer<JqlFilters> {

  private static final List<String> VALID_TYPES = ImmutableList.of(
      DONOR.getId(),
      GENE.getId(),
      MUTATION.getId(),
      PROJECT.getId());

  @Override
  public JqlFilters deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    configureJsonParser(jp);

    JsonNode node = jp.getCodec().readTree(jp);
    log.debug("Deserializing node: {}", node);
    validateTypes(node);
    val elements = new ImmutableMap.Builder<String, List<JqlField>>();

    val nodeFields = node.fields();
    while (nodeFields.hasNext()) {
      elements.putAll(parseType(nodeFields.next()));
    }

    return new JqlFilters(elements.build());
  }

  private static Map<String, List<JqlField>> parseType(Entry<String, JsonNode> entry) {
    val type = entry.getKey();
    log.debug("Parsing fields for type '{}'. Fields: {}", type, entry.getValue());

    val typeFields = new ImmutableList.Builder<JqlField>();
    val nodeFields = entry.getValue().fields();
    while (nodeFields.hasNext()) {
      typeFields.add(parseField(type, nodeFields.next()));
    }

    return singletonMap(type, typeFields.build());
  }

  private static JqlField parseField(String type, Entry<String, JsonNode> next) {
    val fieldName = next.getKey();
    val fieldValue = next.getValue();
    log.debug("Parsing field {} - {}", fieldName, fieldValue);

    if (fieldName.startsWith("has")) {
      return new JqlField(fieldName, Operation.HAS, parseSingleValue(fieldValue), type);
    }

    return new JqlField(fieldName, parseOperation(fieldValue), parseValue(fieldValue), type);
  }

  private static JqlValue parseValue(JsonNode fieldValue) {
    log.debug("Parsing value for {}", fieldValue);
    val nodeValue = getFirstValue(fieldValue);
    if (nodeValue.isArray()) {
      return parseArrayValue(nodeValue);
    }

    return parseSingleValue(nodeValue);
  }

  private static JqlValue parseArrayValue(JsonNode node) {
    log.debug("Parsing array value for {}", node);
    val result = new ImmutableList.Builder<Object>();
    val elements = node.elements();
    while (elements.hasNext()) {
      result.add(parseJsonNodeValue(elements.next()));
    }

    return new JqlArrayValue(result.build());
  }

  private static JqlValue parseSingleValue(JsonNode node) {
    log.debug("Parsing single value for {}", node);
    return new JqlSingleValue(parseJsonNodeValue(node));
  }

  private static Operation parseOperation(JsonNode fieldValue) {
    validateOperation(fieldValue);
    log.debug("Parsing operation for {}", fieldValue);

    return Operation.byId(getFirstFieldName(fieldValue));
  }

  private static void configureJsonParser(JsonParser parser) {
    parser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    parser.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
  }

  private static void validateTypes(JsonNode node) {
    val fieldNames = node.fieldNames();
    while (fieldNames.hasNext()) {
      checkState(VALID_TYPES.contains(fieldNames.next()), "Node has no valid types. %s", node);
    }
  }

  private static void validateOperation(JsonNode fieldValue) {
    log.debug("Validating operation for {}", fieldValue);
    checkState(fieldValue.size() == 1, "More than one operation detected. %s", fieldValue);
    val operation = getFirstFieldName(fieldValue);
    checkState(Operation.operations().contains(operation), "Invalid operation '%s'", operation);
  }

  private static void checkState(boolean expression, String template, Object... args) {
    checkState(expression, format(template, args));
  }

  private static void checkState(boolean expression, String message) {
    if (!expression) {
      throw new IllegalStateException(message);
    }
  }

  private static String getFirstFieldName(JsonNode fieldValue) {
    return fieldValue.fieldNames().next();
  }

  private static JsonNode getFirstValue(@NonNull JsonNode jsonNode) {
    val entry = jsonNode.fields().next();

    return entry.getValue();
  }

  private static Object parseJsonNodeValue(JsonNode node) {
    if (node.isInt()) {
      return node.asInt();
    }

    if (node.isLong()) {
      return node.asLong();
    }

    if (node.isDouble()) {
      return node.asDouble();
    }

    if (node.isBoolean()) {
      return node.asBoolean();
    }

    return node.textValue();
  }

}
