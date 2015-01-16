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

import java.util.Collection;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.antlr.v4.runtime.tree.ParseTree;
import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.MustNotBoolNode;
import org.dcc.portal.pql.es.ast.PostFilterNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.ShouldBoolNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseListener;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ProgramContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.QueryContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Slf4j
@Getter
public class PqlParseListener extends PqlBaseListener {

  private static final PqlParseTreeVisitor PQL_VISITOR = new PqlParseTreeVisitor();

  private final ExpressionNode esAst = createRootNode();

  @Override
  public void exitProgram(ProgramContext context) {
    log.debug("Starting to process query - {}", context.toStringTree());
    val filterNodes = getFilterNodes(context);
    log.debug("Found {} filter nodes", filterNodes.size());

    val boolNode = new BoolNode();

    processOrFilters(filterNodes, boolNode);
    processAndFilters(filterNodes, boolNode);
    val postFilterNode = esAst.getChild(0);
    postFilterNode.addChildren(boolNode);
  }

  private static void processOrFilters(Collection<ParseTree> filterNodes, BoolNode parent) {
    val orFilters = filter(filterNodes, OrContext.class);
    val shouldNode = new ShouldBoolNode();
    parent.addChildren(shouldNode);
    visitCommonChildren(shouldNode, orFilters);
    shouldNode.addChildren(visitChildren(orFilters, NeContext.class));
  }

  private static ExpressionNode createRootNode() {
    val root = new RootNode();
    val postFilter = new PostFilterNode();
    root.addChildren(postFilter);

    return root;
  }

  /**
   * Visits all the nodes of type {@link AndContext} and processes them.
   * @param filterNodes - {@link FilterContext} nodes that represent a filter expression
   * @param parent - parent for all the nodes to be processed
   */
  private static void processAndFilters(Collection<ParseTree> filterNodes, BoolNode parent) {
    val andFilters = filter(filterNodes, AndContext.class);
    val mustNode = new MustBoolNode();
    parent.addChildren(mustNode);
    visitCommonChildren(mustNode, andFilters);

    val neTerms = visitChildren(andFilters, NeContext.class);
    if (neTerms.length > 0) {
      val mustNotNode = new MustNotBoolNode();
      parent.addChildren(mustNotNode);
      mustNotNode.addChildren(neTerms);
    }
  }

  /**
   * Visits children with common processing rules. E.g. those that are the same for queries like and(...), or(...), and
   * adds results of all the processed node to {@code parent}.
   */
  private static void visitCommonChildren(ExpressionNode parent, Iterable<ParseTree> filters) {
    parent.addChildren(visitChildren(filters, EqContext.class));
    parent.addChildren(visitChildren(filters, GeContext.class));
    parent.addChildren(visitChildren(filters, GtContext.class));
    parent.addChildren(visitChildren(filters, LeContext.class));
    parent.addChildren(visitChildren(filters, LtContext.class));
  }

  /**
   * Finds children of every {@code filters} of type {@code type}, and visits those children.
   * @return built children nodes
   */
  private static <T extends FilterContext> ExpressionNode[] visitChildren(Iterable<ParseTree> filters, Class<T> type) {
    val childrenTerms = processChildren(filters, type);

    return Iterables.toArray(childrenTerms, ExpressionNode.class);
  }

  /**
   * Filters children who have filter nodes as their children.
   */
  private Collection<ParseTree> getFilterNodes(ProgramContext context) {
    val result = ImmutableList.<ParseTree> builder();
    val queryNodes = Iterables.filter(context.children, QueryContext.class);

    for (val queryNode : queryNodes) {
      val childrenNum = queryNode.children.size();
      checkState(childrenNum == 1, "QueryNode has incorrect number of children. Should be 1, found " + childrenNum);
      val queryNodeChild = queryNode.children.get(0);
      if (queryNodeChild instanceof FilterContext) {
        result.add(queryNodeChild);
      }
    }

    return result.build();
  }

  /**
   * 
   */
  private static <T> Collection<ExpressionNode> processChildren(Iterable<ParseTree> booleanFilters, Class<T> type) {
    val result = ImmutableList.<ExpressionNode> builder();
    for (val item : booleanFilters) {
      val childrenByType = filter(ParseTrees.getChildren(item), type);
      for (val child : childrenByType) {
        result.add(child.accept(PQL_VISITOR));
      }
    }

    return result.build();
  }

  private static <T> Iterable<ParseTree> filter(Iterable<ParseTree> items, Class<T> type) {
    val result = ImmutableList.<ParseTree> builder();
    for (val item : items) {
      if (type.isAssignableFrom(item.getClass())) {
        result.add(item);
      }
    }

    return result.build();
  }

}
