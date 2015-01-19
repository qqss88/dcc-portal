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

import static org.dcc.portal.pql.es.utils.ParseTrees.getPair;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.LessEqualNode;
import org.dcc.portal.pql.es.ast.LessThanNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseVisitor;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;

public class PqlParseTreeVisitor extends PqlBaseVisitor<ExpressionNode> {

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
    val pair = getPair(nodeContext);

    val nameNode = new TerminalNode(pair.getKey());
    val valueNode = new TerminalNode(pair.getValue());
    ExpressionNode result = new TermNode(nameNode, valueNode);

    if (nodeContext instanceof NeContext) {
      result = checkOrParent(result, (NeContext) nodeContext);
    }

    return result;
  }

  @Override
  public ExpressionNode visitGe(GeContext nodeContext) {
    val pair = getPair(nodeContext);

    return new RangeNode(pair.getKey(), new GreaterEqualNode(pair.getValue()));
  }

  @Override
  public ExpressionNode visitGt(GtContext nodeContext) {
    val pair = getPair(nodeContext);

    return new RangeNode(pair.getKey(), new GreaterThanNode(pair.getValue()));
  }

  @Override
  public ExpressionNode visitLe(LeContext nodeContext) {
    val pair = getPair(nodeContext);

    return new RangeNode(pair.getKey(), new LessEqualNode(pair.getValue()));
  }

  @Override
  public ExpressionNode visitLt(LtContext nodeContext) {
    val pair = getPair(nodeContext);

    return new RangeNode(pair.getKey(), new LessThanNode(pair.getValue()));
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

}
