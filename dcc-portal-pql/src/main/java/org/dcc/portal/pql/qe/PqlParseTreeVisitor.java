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
package org.dcc.portal.pql.qe;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.meta.TypeModel;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseVisitor;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ExistsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FacetsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.InArrayContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.InContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.MissingContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NestedContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrderContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.RangeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.SelectContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ValueContext;

import com.google.common.base.Splitter;

@Slf4j
@AllArgsConstructor
public class PqlParseTreeVisitor extends PqlBaseVisitor<ExpressionNode> {

  private static final String SINGLE_QUOTE = "'";
  private static final String DOUBLE_QUOTE = "\"";
  private static final Splitter FIELD_SPLITTER = Splitter.on(".");

  @NonNull
  private final TypeModel typeModel;

  @Override
  public ExpressionNode visitOr(@NonNull OrContext context) {
    val shouldNode = new ShouldBoolNode();
    val result = new BoolNode(shouldNode);
    for (val filter : context.filter()) {
      shouldNode.addChildren(filter.accept(this));
    }
    log.debug("Result: {}", result);

    return result;
  }

  @Override
  public ExpressionNode visitAnd(@NonNull AndContext context) {
    val andNode = new MustBoolNode();
    val result = new BoolNode(andNode);
    for (val filter : context.filter()) {
      andNode.addChildren(filter.accept(this));
    }
    log.debug("Result: {}", result);

    return result;
  }

  @Override
  public ExpressionNode visitEqual(@NonNull EqualContext nodeContext) {
    return nodeContext.eq().accept(this);
  }

  @Override
  public ExpressionNode visitEq(@NonNull EqContext context) {
    val nameNode = createNameNode(context.ID().getText());
    val valueNode = (TerminalNode) context.value().accept(this);
    val termNode = new TermNode(nameNode, valueNode);
    log.debug("TermNode: {}", termNode);

    return termNode;
  }

  @Override
  public ExpressionNode visitValue(@NonNull ValueContext context) {
    if (context.STRING() != null) {
      return new TerminalNode(cleanString(context.STRING().getText()));
    } else if (context.FLOAT() != null) {
      val value = Double.parseDouble(context.FLOAT().getText());
      return new TerminalNode(value);
    } else {
      val value = Integer.parseInt(context.INT().getText());
      return new TerminalNode(value);
    }
  }

  private static String cleanString(@NonNull String original) {
    checkState(
        original.startsWith(SINGLE_QUOTE) && original.endsWith(SINGLE_QUOTE) ||
            original.startsWith(DOUBLE_QUOTE) && original.endsWith(DOUBLE_QUOTE),
        "Incorrectly quoted string: %s", original);

    return original.substring(1, original.length() - 1);

  }

  @Override
  public ExpressionNode visitNotEqual(@NonNull NotEqualContext nodeContext) {
    return nodeContext.ne().accept(this);
  }

  @Override
  public ExpressionNode visitNe(@NonNull NeContext context) {
    val nameNode = createNameNode(context.ID().getText());
    val valueNode = (TerminalNode) context.value().accept(this);
    val notNode = new NotNode(new TermNode(nameNode, valueNode));
    log.debug("NotNode: {}", notNode);

    return notNode;
  }

  @Override
  public ExpressionNode visitGreaterEqual(@NonNull GreaterEqualContext nodeContext) {
    return nodeContext.ge().accept(this);
  }

  @Override
  public ExpressionNode visitGe(@NonNull GeContext context) {
    val value = context.value().accept(this);
    val field = getField(context.ID().getText());
    val rangeNode = new RangeNode(field, new GreaterEqualNode(value));
    log.debug("RangeNode: {}", rangeNode);

    return rangeNode;
  }

  @Override
  public ExpressionNode visitGreaterThan(@NonNull GreaterThanContext nodeContext) {
    return nodeContext.gt().accept(this);
  }

  @Override
  public ExpressionNode visitGt(@NonNull GtContext context) {
    val value = context.value().accept(this);
    val field = getField(context.ID().getText());
    val rangeNode = new RangeNode(field, new GreaterThanNode(value));
    log.debug("RangeNode: {}", rangeNode);

    return rangeNode;
  }

  @Override
  public ExpressionNode visitLessEqual(@NonNull LessEqualContext nodeContext) {
    return nodeContext.le().accept(this);
  }

  @Override
  public ExpressionNode visitLe(@NonNull LeContext context) {
    val value = context.value().accept(this);
    val field = getField(context.ID().getText());
    val rangeNode = new RangeNode(field, new LessEqualNode(value));
    log.debug("RangeNode: {}", rangeNode);

    return rangeNode;
  }

  @Override
  public ExpressionNode visitLessThan(@NonNull LessThanContext nodeContext) {
    return nodeContext.lt().accept(this);
  }

  @Override
  public ExpressionNode visitLt(@NonNull LtContext context) {
    val value = context.value().accept(this);
    val field = getField(context.ID().getText());
    val rangeNode = new RangeNode(field, new LessThanNode(value));
    log.debug("RangeNode: {}", rangeNode);

    return rangeNode;
  }

  @Override
  public ExpressionNode visitSelect(@NonNull SelectContext nodeContext) {
    if (nodeContext.ASTERISK() != null) {
      return addAllFields();
    }

    val fieldsNode = new FieldsNode();
    for (val field : nodeContext.ID()) {
      fieldsNode.addChildren(new TerminalNode(getField(field.getText())));
    }

    log.debug("FieldsNode: {}", fieldsNode);

    return fieldsNode;
  }

  @Override
  public ExpressionNode visitRange(@NonNull RangeContext nodeContext) {
    int size = Integer.parseInt(nodeContext.INT(0).getText());
    int from = 0;
    if (nodeContext.INT().size() == 2) {
      from = Integer.parseInt(nodeContext.INT(0).getText());
      size = Integer.parseInt(nodeContext.INT(1).getText());
    }
    val limitNode = new LimitNode(from, size);
    log.debug("{}", limitNode);

    return limitNode;
  }

  @Override
  public ExpressionNode visitOrder(@NonNull OrderContext nodeContext) {
    val sortNode = new SortNode();
    for (val id : nodeContext.ID()) {
      val order = getOrderAt(nodeContext, id.getSymbol().getCharPositionInLine() - 1);
      if (order != null) {
        sortNode.addField(getField(id.getText()), order);
      } else {
        sortNode.addField(getField(id.getText()), Order.ASC);
      }
    }
    log.debug("{}", sortNode);

    return sortNode;
  }

  private static Order getOrderAt(OrderContext parent, int position) {
    for (val sign : parent.SIGN()) {
      if (sign.getSymbol().getCharPositionInLine() == position) {
        return Order.bySign(sign.getText());
      }
    }

    return null;
  }

  @Override
  public ExpressionNode visitInArray(@NonNull InArrayContext nodeContext) {
    return nodeContext.in().accept(this);
  }

  @Override
  public ExpressionNode visitIn(@NonNull InContext nodeContext) {
    val termsNode = new TermsNode(getField(nodeContext.ID().getText()));
    for (val child : nodeContext.value()) {
      termsNode.addChildren(child.accept(this));
    }
    log.debug("{}", termsNode);

    return termsNode;
  }

  @Override
  public ExpressionNode visitNested(@NonNull NestedContext nodeContext) {
    val mustNode = new MustBoolNode();
    val nestedNode = new NestedNode(nodeContext.ID().getText(), new BoolNode(mustNode));
    for (val child : nodeContext.filter()) {
      mustNode.addChildren(child.accept(this));
    }
    log.debug("{}", nestedNode);

    return nestedNode;
  }

  /**
   * Visiting facets but creating {@link AggregationsNode}.
   */
  @Override
  public ExpressionNode visitFacets(@NonNull FacetsContext nodeContext) {
    if (nodeContext.ASTERISK() != null) {
      return addAllFacets();
    }

    val aggsNode = new AggregationsNode();
    for (val child : nodeContext.ID()) {
      aggsNode.addChildren(new TermsAggregationNode(child.getText(), getField(child.getText())));
    }

    log.debug("{}", aggsNode);

    return aggsNode;
  }

  @Override
  public ExpressionNode visitNot(@NonNull NotContext nodeContext) {
    val childNode = nodeContext.filter().accept(this);
    val notNode = new NotNode(childNode);
    log.debug("{}", notNode);

    return notNode;
  }

  @Override
  public ExpressionNode visitExists(@NonNull ExistsContext nodeContext) {
    val field = getField(nodeContext.ID().getText());
    val existsNode = new ExistsNode(field);
    log.debug("{}", existsNode);

    return existsNode;
  }

  @Override
  public ExpressionNode visitMissing(@NonNull MissingContext nodeContext) {
    val field = getField(nodeContext.ID().getText());
    val missingNode = new MissingNode(field);
    log.debug("{}", missingNode);

    return missingNode;
  }

  private TerminalNode createNameNode(@NonNull String field) {
    return new TerminalNode(getField(field));
  }

  private String getField(String alias) {
    return typeModel.getField(resolveAlias(alias));
  }

  private String resolveAlias(String alias) {
    if (TypeModel.SPECIAL_CASES_FIELDS.contains(alias)) {
      return alias;
    }

    val components = splitFields(alias);
    if (components.size() == 1 || !typeModel.prefix().equals(components.get(0))) {
      return alias;
    }

    return components.get(1);
  }

  private static List<String> splitFields(String fullyQualifiedFieldName) {
    val fields = FIELD_SPLITTER.splitToList(fullyQualifiedFieldName);

    return fields;
  }

  private ExpressionNode addAllFields() {
    val fieldsNode = new FieldsNode();
    for (val field : typeModel.getFields()) {
      fieldsNode.addChildren(new TerminalNode(field));
    }

    log.debug("FieldsNode: {}", fieldsNode);

    return fieldsNode;
  }

  private ExpressionNode addAllFacets() {
    val aggsNode = new AggregationsNode();
    for (val child : typeModel.getFacets()) {
      aggsNode.addChildren(new TermsAggregationNode(child, getField(child)));
    }

    log.debug("{}", aggsNode);

    return aggsNode;
  }

}
