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
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseVisitor;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;

public class PqlParseTreeVisitor extends PqlBaseVisitor<ExpressionNode> {

  /**
   * Index of value in a 'equal' or 'not equal' node
   */
  private static final int EQ_NE_NODE_VALUE_INDEX = 4;

  /**
   * Index of name in a 'equal' or 'not equal' node
   */
  private static final int EQ_NE_NODE_NAME_INDEX = 2;

  /**
   * Number of children in a valid 'equal' or 'not equal' parse tree node
   */
  private static final int EQ_NE_NODE_CHILDREN_COUNT = 6;

  /**
   * @return {@link ShouldBoolNode}
   */
  @Override
  public ExpressionNode visitOr(@NonNull OrContext context) {
    val shouldBoolNode = new ShouldBoolNode();
    for (val child : context.children) {
      if (child instanceof EqContext) {
        val termNode = visitEq((EqContext) child);
        shouldBoolNode.addChildren(termNode);
      } else if (child instanceof NeContext) {
        val termNode = visitNe((NeContext) child);
        shouldBoolNode.addChildren(termNode);
      }
    }

    return shouldBoolNode;
  }

  @Override
  public ExpressionNode visitEq(EqContext nodeContext) {
    return visitEqualityNodes(nodeContext);
  }

  @Override
  public ExpressionNode visitNe(NeContext nodeContext) {
    return visitEqualityNodes(nodeContext);
  }

  private ExpressionNode visitEqualityNodes(FilterContext nodeContext) {
    checkState(nodeContext.getChildCount() == EQ_NE_NODE_CHILDREN_COUNT,
        "Equal node is malformed. Expected {} children, but found {}", EQ_NE_NODE_CHILDREN_COUNT,
        nodeContext.getChildCount());
    val name = nodeContext.getChild(EQ_NE_NODE_NAME_INDEX).getText();
    val value = nodeContext.getChild(EQ_NE_NODE_VALUE_INDEX).getText();

    val nameNode = new TerminalNode(name);
    val valueNode = new TerminalNode(getTypeSafeValue(value));
    ExpressionNode result = new TermNode(nameNode, valueNode);

    if (nodeContext instanceof NeContext) {
      result = checkOrParent(result, (NeContext) nodeContext);
    }

    return result;
  }

  /**
   * If parent of the 'ne' node is 'or' wraps {@code result} in {@link NotNode}.
   */
  private ExpressionNode checkOrParent(ExpressionNode neNode, NeContext nodeContext) {
    if (nodeContext.parent instanceof OrContext) {
      return new NotNode(neNode);
    }

    return neNode;
  }

  // TODO: any lib exist?
  private static final Object getTypeSafeValue(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
    }

    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
    }

    return value;
  }

}
