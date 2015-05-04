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
package org.dcc.portal.pql.query;

import static java.lang.String.format;

import java.util.Collection;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.CountNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseListener;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FunctionContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.StatementContext;

@Deprecated
@Slf4j
@Value
@EqualsAndHashCode(callSuper = false)
public class PqlParseListener extends PqlBaseListener {

  ExpressionNode esAst = new RootNode();
  PqlParseTreeVisitor pqlVisitor;

  public PqlParseListener(@NonNull QueryContext queryContext) {
    this.pqlVisitor = new PqlParseTreeVisitor(queryContext.getTypeModel());
  }

  @Override
  public void exitStatement(@NonNull StatementContext context) {
    log.debug("Starting to process query - {}", context.toStringTree());
    if (context.count() != null) {
      log.debug("Processing count() type query");
      esAst.addChildren(new CountNode());
    }

    val filters = context.filter();
    log.debug("Found {} filter nodes", filters.size());

    // process filters
    if (!filters.isEmpty()) {
      val parentNode = new QueryNode(new FilterNode(processFilters(filters)));
      esAst.addChildren(parentNode);
    }

    // process functions
    val functions = context.function();
    if (!functions.isEmpty()) {
      processFunctions(functions, esAst);
    }

    // process limit
    val rangeContext = context.range();
    if (rangeContext != null) {
      esAst.addChildren(rangeContext.accept(pqlVisitor));
    }

    // process sort
    val sortContext = context.order();
    if (sortContext != null) {
      esAst.addChildren(sortContext.accept(pqlVisitor));
    }

  }

  private void processFunctions(Collection<FunctionContext> functions, ExpressionNode rootNode) {
    for (val child : functions) {
      val visitResult = child.accept(pqlVisitor);
      if (visitResult instanceof FieldsNode) {
        resolveFields(visitResult, rootNode, FieldsNode.class);
      } else if (visitResult instanceof AggregationsNode) {
        resolveFields(visitResult, rootNode, AggregationsNode.class);
      } else {
        throw new IllegalArgumentException(format("Unrecognized function %s", visitResult));
      }
    }
  }

  private static <T extends ExpressionNode> void resolveFields(ExpressionNode child, ExpressionNode rootNode,
      Class<T> clazz) {
    val childOpt = Nodes.getOptionalChild(rootNode, clazz);
    if (childOpt.isPresent()) {
      childOpt.get().addChildren(child.getChildrenArray());
    } else {
      rootNode.addChildren(child);
    }
  }

  /**
   * Visits all the nodes of type {@link AndContext} and processes them.
   * @param filterNodes - {@link FilterContext} nodes that represent a filter expression
   * @param boolNode - parent for all the nodes to be processed
   */
  private BoolNode processFilters(Collection<FilterContext> filterNodes) {
    val boolNode = new BoolNode();
    val mustNode = new MustBoolNode();
    boolNode.addChildren(mustNode);

    for (val filter : filterNodes) {
      val expressionNode = filter.accept(pqlVisitor);
      log.debug("Filter {} generated {}", filter.toStringTree(), expressionNode);
      mustNode.addChildren(expressionNode);
    }

    return boolNode;
  }

}
