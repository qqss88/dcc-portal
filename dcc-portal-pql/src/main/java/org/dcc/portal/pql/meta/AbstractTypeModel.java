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
package org.dcc.portal.pql.meta;

import static java.lang.String.format;
import static org.dcc.portal.pql.meta.Constants.FIELD_SEPARATOR;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.exception.SemanticException;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.visitor.CreateAliasVisitor;
import org.dcc.portal.pql.meta.visitor.CreateFullyQualifiedNameVisitor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@Slf4j
public abstract class AbstractTypeModel {

  /**
   * Following public constants used to resolve the special cases in the API.
   */
  public static final String MOLECULAR_FUNCTION = "go_term.molecular_function";
  public static final String BIOLOGICAL_PROCESS = "go_term.biological_process";
  public static final String CELLULAR_COMPONENT = "go_term.cellular_component";

  public static final String GENE_PATHWAY_ID = "gene.pathwayId";
  public static final String GENE_SET_ID = "gene.geneSetId";
  public static final String GENE_GO_TERM_ID = "gene.goTermId";
  public static final String GENE_CURATED_SET_ID = "gene.curatedSetId";

  public static final String GENE_LOCATION = "gene.location";
  public static final String MUTATION_LOCATION = "mutation.location";

  public static final String ENTITY_SET_ID = "entitySetId";
  public static final String DONOR_ENTITY_SET_ID = format("%s.%s", "donor", ENTITY_SET_ID);
  public static final String GENE_ENTITY_SET_ID = format("%s.%s", "gene", ENTITY_SET_ID);
  public static final String MUTATION_ENTITY_SET_ID = format("%s.%s", "mutation", ENTITY_SET_ID);

  public static final String SCORE = "_score";

  public static final String LOOKUP_PATH = "lookup.path";
  public static final String LOOKUP_INDEX = "lookup.index";
  public static final String LOOKUP_TYPE = "lookup.type";

  protected final Map<String, FieldModel> fieldsByFullPath;
  protected final Map<String, String> fieldsByAlias;
  protected final Map<String, String> fieldsByInternalAlias;

  public AbstractTypeModel(@NonNull List<? extends FieldModel> fields, @NonNull Map<String, String> internalAliases) {
    fieldsByFullPath = initFieldsByFullPath(fields);
    log.debug("FieldsByFullPath Map: {}", fieldsByFullPath);
    fieldsByAlias = initFieldsByAlias(fields);
    log.debug("FieldsByAlias Map: {}", fieldsByAlias);
    this.fieldsByInternalAlias = defineInternalAliases(internalAliases);
  }

  public abstract String getType();

  /**
   * Checks if {@code field} is nested field.
   * 
   * @param field - fully qualified name. Not field alias.
   */
  public final boolean isNested(String field) {
    val fullName = getFullName(field);
    val tokens = split(fullName);
    log.debug("Tokens: {}", tokens);
    for (val token : tokens) {
      log.debug("Processing token: {}", token);
      val tokenByFullPath = fieldsByFullPath.get(token);
      if (tokenByFullPath.isNested()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns fully qualified name of the field that has {@code alias} defined.
   * @throws NoSuchElementException if there is a field with such an alias.
   */
  public final String getField(@NonNull String field) {
    val alias = fieldsByAlias.get(field);
    if (alias == null) {
      throw new SemanticException("Field %s is not defined in the type model", field);
    }

    return alias;
  }

  /**
   * Returns fully qualified name by an internal alias.
   * @throws NoSuchElementException if there is a field with such an alias.
   */
  public final String getInternalField(@NonNull String internalAlias) {
    val result = fieldsByInternalAlias.get(internalAlias);
    if (result == null) {
      throw new SemanticException("Field %s is not defined in the type model", internalAlias);
    }

    return result;
  }

  @Override
  public String toString() {
    val builder = new StringBuilder();
    val newLine = System.getProperty("line.separator");
    for (val entity : fieldsByFullPath.entrySet()) {
      val value = entity.getValue();
      builder.append(format("Path: %s, Type: %s, Nested: %s", entity.getKey(), value.getType(), value.isNested()));
      builder.append(newLine);
    }

    return builder.toString();
  }

  private List<String> split(String fullName) {
    val result = new ImmutableList.Builder<String>();
    val list = Splitter.on(FIELD_SEPARATOR).splitToList(fullName);
    String prefix = "";
    for (int i = 0; i < list.size(); i++) {
      result.add(prefix + list.get(i));
      prefix = prefix + list.get(i) + FIELD_SEPARATOR;
    }

    return result.build().reverse();
  }

  public final String getNestedPath(@NonNull String field) {
    val fullName = getFullName(field);
    for (val token : split(fullName)) {
      val tokenByFullPath = fieldsByFullPath.get(token);
      if (tokenByFullPath.isNested()) {
        return token;
      }
    }

    throw new IllegalArgumentException("Can't get nested path for a non-nested field");
  }

  private String getFullName(String path) {
    val uiAlias = fieldsByAlias.get(path);

    return uiAlias == null ? path : uiAlias;

  }

  /**
   * Defines common aliases and adds type specific ones.
   */
  private Map<String, String> defineInternalAliases(Map<String, String> internalAliases) {
    return new ImmutableMap.Builder<String, String>()
        .put(LOOKUP_INDEX, "terms-lookup")
        .put(LOOKUP_PATH, "values")
        .putAll(internalAliases)
        .build();
  }

  private static Map<String, FieldModel> initFieldsByFullPath(List<? extends FieldModel> fields) {
    val result = Maps.<String, FieldModel> newHashMap();
    val visitor = new CreateFullyQualifiedNameVisitor();
    for (val field : fields) {
      result.putAll(field.accept(visitor));
    }

    return result;
  }

  private static Map<String, String> initFieldsByAlias(List<? extends FieldModel> fields) {
    val result = new ImmutableMap.Builder<String, String>();
    val visitor = new CreateAliasVisitor();
    for (val field : fields) {
      result.putAll(field.accept(visitor));
    }

    return result.build();
  }

}
