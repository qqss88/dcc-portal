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
package org.dcc.portal.pql.es.visitor.special;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.visitChildren;
import static org.dcc.portal.pql.meta.AbstractTypeModel.GENE_LOCATION;
import static org.dcc.portal.pql.meta.AbstractTypeModel.MUTATION_LOCATION;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.AbstractTypeModel;
import org.dcc.portal.pql.qe.QueryContext;

import com.google.common.base.Splitter;

/**
 * Resolves filtering by {@code gene.location} and {@code mutation.location}
 */
@Slf4j
public class LocationFilterVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  private static final String MUTATION_CHROMOSOME = "mutation.chromosome";
  private static final String GENE_CHROMOSOME = "gene.chromosome";

  private static final Pattern CHROMOSOME_REGEX = Pattern.compile("^chr\\p{Alnum}{1,2}$");
  private static final String GENE_START = "gene.start";
  private static final String GENE_END = "gene.end";
  private static final String MUTATION_START = "mutation.start";
  private static final String MUTATION_END = "mutation.end";

  private static final Splitter COLON_SPLITTER = Splitter.on(":");
  private static final Splitter DASH_SPLITTER = Splitter.on("-");

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val filterNode = Nodes.getOptionalChild(node, FilterNode.class);
    if (filterNode.isPresent()) {
      filterNode.get().accept(this, context);
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitAnd(@NonNull AndNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitOr(@NonNull OrNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNested(@NonNull NestedNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRange(@NonNull RangeNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitExists(@NonNull ExistsNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitMissing(@NonNull MissingNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = (String) node.getNameNode().getValue();
    log.debug("[visitTerm] Field: {}", field);
    if (!field.equals(GENE_LOCATION) && !field.equals(MUTATION_LOCATION)) {
      return Optional.empty();
    }

    val result = new AndNode();

    val locationValue = (String) node.getValueNode().getValue();
    val typeModel = context.get().getTypeModel();
    result.addChildren(createTermNode(field, typeModel, locationValue));
    result.addChildren(createGreaterEqualNode(resolveStartField(field, typeModel), parseStart(locationValue)));
    result.addChildren(createLessEqualNode(resolveEndField(field, typeModel), parseEnd(locationValue)));

    return Optional.of(result);
  }

  /**
   * {@code expression} looks like chr12:123-346
   * @return Chromosome end value. E.g. 346
   */
  private static long parseEnd(String expression) {
    val components = splitByColon(expression);
    val startEndComponents = splitByDash(components.get(1));

    return (Long.valueOf(startEndComponents.get(1))).longValue();
  }

  private static String resolveEndField(String field, AbstractTypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_END);
    }

    return typeModel.getField(MUTATION_END);
  }

  private static ExpressionNode createLessEqualNode(String field, long value) {
    val leNode = new LessEqualNode(value);

    return new RangeNode(field, leNode);
  }

  private static ExpressionNode createTermNode(String field, AbstractTypeModel typeModel, String location) {
    return new TermNode(resolveChromosomeField(field, typeModel), parseChromosome(location));
  }

  /**
   * {@code expression} looks like chr12:123-346
   * @return Chromosome start value. E.g. 123
   */
  private static long parseStart(String expression) {
    val components = splitByColon(expression);
    val startEndComponents = splitByDash(components.get(1));

    return (Long.valueOf(startEndComponents.get(0))).longValue();
  }

  /**
   * {@code expression} looks like 123-346
   */
  private static List<String> splitByDash(String expression) {
    val components = DASH_SPLITTER.splitToList(expression);
    checkState(components.size() == 2, format("Malformed location field '%s'", expression));

    return components;
  }

  private static ExpressionNode createGreaterEqualNode(String field, long value) {
    val geNode = new GreaterEqualNode(value);

    return new RangeNode(field, geNode);
  }

  private static String resolveStartField(String field, AbstractTypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_START);
    }

    return typeModel.getField(MUTATION_START);
  }

  private static String resolveChromosomeField(String field, AbstractTypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_CHROMOSOME);
    }

    return typeModel.getField(MUTATION_CHROMOSOME);
  }

  /**
   * {@code expression} looks like chr12:123-346
   * @return Chromosome value. E.g. 12
   */
  private static String parseChromosome(String expression) {
    val components = splitByColon(expression);
    val chromosome = components.get(0);
    checkChromosome(chromosome);

    return chromosome.substring(3);
  }

  private static List<String> splitByColon(String expression) {
    val components = COLON_SPLITTER.splitToList(expression);
    checkState(components.size() == 2, format("Malformed location field '%s'", expression));

    return components;
  }

  private static void checkChromosome(String chromosome) {
    val matcher = CHROMOSOME_REGEX.matcher(chromosome);
    checkState(matcher.find(), format("Malformed chromosome '%s'", chromosome));
  }

}
