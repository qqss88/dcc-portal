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
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ProgramContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.QueryContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Getter
public class PqlParseListener extends PqlBaseListener {

  private static final PqlParseTreeVisitor PQL_VISITOR = new PqlParseTreeVisitor();

  private final ExpressionNode esAst = createRootNode();

  @Override
  public void exitProgram(ProgramContext context) {
    val filterNodes = getFilterNodes(context);
    val boolNode = new BoolNode();
    val shouldBoolNodes = getShouldBoolNodes(filterNodes);
    if (shouldBoolNodes.size() > 0) {
      boolNode.addChildren(mergeShouldBoolNodes(shouldBoolNodes));
    }

    processAndFilters(filterNodes, boolNode);
    val postFilterNode = esAst.getChild(0);
    postFilterNode.addChildren(boolNode);
  }

  private ShouldBoolNode mergeShouldBoolNodes(Collection<ExpressionNode> orFilterNodes) {
    val result = new ShouldBoolNode();
    for (val shouldNode : orFilterNodes) {
      ExpressionNode[] children = shouldNode.getChildren().toArray(new ExpressionNode[shouldNode.getChildren().size()]);
      result.addChildren(children);
    }

    return result;
  }

  private static ExpressionNode createRootNode() {
    val root = new RootNode();
    val postFilter = new PostFilterNode();
    root.addChildren(postFilter);

    return root;
  }

  private static void processAndFilters(Collection<ParseTree> filterNodes, BoolNode boolNode) {
    val andFilters = filter(filterNodes, AndContext.class);

    val mustNode = new MustBoolNode();
    boolNode.addChildren(mustNode);
    val eqTerms = processChildren(andFilters, EqContext.class);
    mustNode.addChildren(Iterables.toArray(eqTerms, ExpressionNode.class));

    val mustNotNode = new MustNotBoolNode();
    boolNode.addChildren(mustNotNode);
    val neTerms = processChildren(andFilters, NeContext.class);
    mustNotNode.addChildren(Iterables.toArray(neTerms, ExpressionNode.class));
  }

  private Collection<ExpressionNode> getShouldBoolNodes(Collection<ParseTree> filterNodes) {
    val result = ImmutableList.<ExpressionNode> builder();
    for (val child : filterNodes) {
      if (child instanceof OrContext) {
        val shouldNode = PQL_VISITOR.visit(child);
        result.add(shouldNode);
      }
    }

    return result.build();
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
