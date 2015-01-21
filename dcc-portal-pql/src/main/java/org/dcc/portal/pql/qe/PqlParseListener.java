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

import java.util.Collection;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.PostFilterNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseListener;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FunctionContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.StatementContext;

@Slf4j
@Getter
public class PqlParseListener extends PqlBaseListener {

  private static final PqlParseTreeVisitor PQL_VISITOR = new PqlParseTreeVisitor();

  private final ExpressionNode esAst = new RootNode();

  @Override
  public void exitStatement(StatementContext context) {
    log.debug("Starting to process query - {}", context.toStringTree());
    val filters = context.filter();
    log.debug("Found {} filter nodes", filters.size());

    // process filters
    if (!filters.isEmpty()) {
      val boolNode = new BoolNode();
      val postFilterNode = new PostFilterNode(boolNode);
      esAst.addChildren(postFilterNode);
      processFilters(filters, boolNode);
    }

    // process functions
    val functions = context.function();
    if (!functions.isEmpty()) {
      processFunctions(functions, esAst);
    }

    // process limit
    val rangeContext = context.range();
    if (rangeContext != null) {
      esAst.addChildren(rangeContext.accept(PQL_VISITOR));
    }

  }

  private static void processFunctions(Collection<FunctionContext> functions, ExpressionNode rootNode) {
    for (val child : functions) {
      rootNode.addChildren(child.accept(PQL_VISITOR));
    }
  }

  /**
   * Visits all the nodes of type {@link AndContext} and processes them.
   * @param filterNodes - {@link FilterContext} nodes that represent a filter expression
   * @param parent - parent for all the nodes to be processed
   */
  private static void processFilters(Collection<FilterContext> filterNodes, BoolNode parent) {
    val mustNode = new MustBoolNode();
    parent.addChildren(mustNode);

    for (val filter : filterNodes) {
      val expressionNode = filter.accept(PQL_VISITOR);
      log.debug("Filter {} generated {}", filter.toStringTree(), expressionNode);
      mustNode.addChildren(expressionNode);
    }
  }

}
