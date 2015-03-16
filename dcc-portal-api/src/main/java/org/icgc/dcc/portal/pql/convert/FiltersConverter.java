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

import java.util.List;
import java.util.Map.Entry;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.pql.convert.model.JqlField;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.pql.convert.model.Operation;

@Slf4j
public class FiltersConverter {

  private final static String QUERY_SEPARATOR = ",";
  private final static String NOT_TEMPLATE = "not(%s)";
  private final static String IN_TEMPLATE = "in(%s,%s)";
  private final static String EQ_TEMPLATE = "eq(%s,%s)";
  private final static String NE_TEMPLATE = "ne(%s,%s)";
  private static String EXISTS_TEMPLATE = "exists(%s)";
  private static String MISSING_TEMPLATE = "missing(%s)";

  public String convertFilters(JqlFilters filters) {
    val lastType = getLast(filters.getTypeValues().keySet());
    val result = new StringBuilder();

    for (val type : filters.getTypeValues().entrySet()) {
      result.append(convertType(type));
      if (!lastType.equals(type.getKey())) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static String convertType(Entry<String, List<JqlField>> type) {
    log.debug("Converting type '{}' {}", type.getKey(), type.getValue());
    val result = new StringBuilder();

    val typePrefix = type.getKey();
    for (int i = 0; i < type.getValue().size(); i++) {
      val jqlField = type.getValue().get(i);
      result.append(createFilter(typePrefix, jqlField));
      if (i != type.getValue().size() - 1) {
        result.append(QUERY_SEPARATOR);
      }
    }

    return result.toString();
  }

  private static String createFilter(String typePrefix, JqlField jqlField) {
    if (jqlField.getOperation() == Operation.HAS) {
      return jqlField.getValue().get() == Boolean.TRUE ?
          format(EXISTS_TEMPLATE, jqlField.getName()) :
          format(MISSING_TEMPLATE, jqlField.getName());
    }

    val filterType = parseFilterType(typePrefix, jqlField);
    if (jqlField.getOperation() == NOT && jqlField.getValue().isArray()) {
      return (format(NOT_TEMPLATE, filterType));
    }

    return (filterType);
  }

  private static String parseFilterType(String prefix, JqlField jqlField) {
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
