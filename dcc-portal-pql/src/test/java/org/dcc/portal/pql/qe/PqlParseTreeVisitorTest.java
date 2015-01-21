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

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.dcc.portal.pql.es.ast.AndNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.LessEqualNode;
import org.dcc.portal.pql.es.ast.LessThanNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.OrNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.utils.Order;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrderContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.RangeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.SelectContext;
import org.junit.Test;

public class PqlParseTreeVisitorTest {

  private static final PqlParseTreeVisitor VISITOR = new PqlParseTreeVisitor();

  @Test
  public void visitOrTest_simple() {
    val query = "or(gt(weight, 10), lt(age, 50), eq(sex, 'male'))";
    val parseTree = ParseTrees.createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0);
    val orNode = (OrNode) VISITOR.visitOr(orContext);
    assertThat(orNode.getChildren().size()).isEqualTo(3);

    // gt(weight, 10)
    RangeNode rangeNode = (RangeNode) orNode.getChild(0);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(age, 50)
    rangeNode = (RangeNode) orNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val ltNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(ltNode.getValue()).isEqualTo(50);

    // eq(sex, 'male')
    val termNode = (TermNode) orNode.getChild(2);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");
  }

  @Test
  public void visitOrTest_withAnd() {
    val query = "or(eq(sex, 'male'), and(gt(weight, 10), lt(age, 50)))";
    val parseTree = ParseTrees.createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0);
    val orNode = (OrNode) VISITOR.visitOr(orContext);
    assertThat(orNode.getChildren().size()).isEqualTo(2);

    // eq(sex, 'male')
    val termNode = (TermNode) orNode.getChild(0);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");

    val andNode = (AndNode) orNode.getChild(1);
    assertThat(andNode.childrenCount()).isEqualTo(2);

    // gt(weight, 10)
    RangeNode rangeNode = (RangeNode) andNode.getChild(0);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(age, 50)
    rangeNode = (RangeNode) andNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val ltNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(ltNode.getValue()).isEqualTo(50);
  }

  @Test
  public void visitValueTest_string_single_quote() {
    val query = "eq(sex, 'male')";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo("male");
  }

  @Test
  public void visitValueTest_string_double_quote() {
    val query = "eq(sex, \"male\")";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo("male");
  }

  @Test
  public void visitValueTest_int() {
    val query = "eq(age, 20)";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo(20);
  }

  @Test
  public void visitValueTest_double() {
    val query = "eq(age, 20.555)";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo(20.555);
  }

  @Test
  public void visitAndTest_simple() {
    val query = "and(gt(weight, 10), lt(age, 50), eq(sex, 'male'))";
    val parseTree = ParseTrees.createParseTree(query);
    val andContext = (AndContext) parseTree.getChild(0);
    val andNode = (AndNode) VISITOR.visitAnd(andContext);
    assertThat(andNode.getChildren().size()).isEqualTo(3);

    // gt(weight, 10)
    RangeNode rangeNode = (RangeNode) andNode.getChild(0);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(age, 50)
    rangeNode = (RangeNode) andNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val ltNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(ltNode.getValue()).isEqualTo(50);

    // eq(sex, 'male')
    val termNode = (TermNode) andNode.getChild(2);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");
  }

  @Test
  public void visitAndTest_withOr() {
    val query = "and(eq(sex, 'male'), or(gt(weight, 10), lt(age, 50)))";
    val parseTree = ParseTrees.createParseTree(query);
    val andContext = (AndContext) parseTree.getChild(0);
    val andNode = (AndNode) VISITOR.visitAnd(andContext);
    assertThat(andNode.getChildren().size()).isEqualTo(2);

    // eq(sex, 'male')
    val termNode = (TermNode) andNode.getChild(0);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");

    val orNode = (OrNode) andNode.getChild(1);
    assertThat(orNode.childrenCount()).isEqualTo(2);

    // gt(weight, 10)
    RangeNode rangeNode = (RangeNode) orNode.getChild(0);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(age, 50)
    rangeNode = (RangeNode) orNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val ltNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(ltNode.getValue()).isEqualTo(50);
  }

  @Test
  public void visitEqualTest() {
    val query = "eq(weight, 10.1)";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val termNode = (TermNode) VISITOR.visitEqual(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("weight");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10.1);
  }

  @Test
  public void visitNotEqualTest() {
    val query = "ne(age, 10)";
    val parseTree = ParseTrees.createParseTree(query);
    val notEqContext = (NotEqualContext) parseTree.getChild(0);
    val notNode = (NotNode) VISITOR.visitNotEqual(notEqContext);
    val termNode = (TermNode) notNode.getChild(0);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("age");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10);
  }

  @Test
  public void visitGreaterEqualTest() {
    val query = "ge(weight, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree(query);
    // program - query
    val geContext = (GreaterEqualContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGreaterEqual(geContext);

    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val geNode = (GreaterEqualNode) rangeNode.getChild(0);
    assertThat(geNode.getChildren().size()).isEqualTo(1);
    assertThat(geNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) geNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitGreaterThanTest() {
    val query = "gt(weight, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree(query);
    // program - query
    val gtContext = (GreaterThanContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGreaterThan(gtContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getChildren().size()).isEqualTo(1);
    assertThat(gtNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) gtNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLessEqualTest() {
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree("le(weight, 10)");
    // program - query
    val leContext = (LessEqualContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLessEqual(leContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val leNode = (LessEqualNode) rangeNode.getChild(0);
    assertThat(leNode.getChildren().size()).isEqualTo(1);
    assertThat(leNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) leNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLessThanTest() {
    val parseTree = ParseTrees.createParseTree("lt(weight, 10)");
    val ltContext = (LessThanContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLessThan(ltContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val toNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(toNode.getChildren().size()).isEqualTo(1);
    assertThat(toNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) toNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitSelectTest() {
    val parseTree = ParseTrees.createParseTree("select(age, gender)");
    val selectContext = (SelectContext) parseTree.getChild(0);
    val fieldsNode = (FieldsNode) VISITOR.visitSelect(selectContext);
    assertThat(fieldsNode.childrenCount()).isEqualTo(2);
    assertThat(fieldsNode.getFields()).containsOnly("age", "gender");
  }

  @Test
  public void visitLimitTest() {
    val parseTree = ParseTrees.createParseTree("count(),limit(1, 5)");
    val rangeContext = (RangeContext) parseTree.getChild(2);
    val limitNode = (LimitNode) VISITOR.visitRange(rangeContext);
    assertThat(limitNode.getFrom()).isEqualTo(1);
    assertThat(limitNode.getSize()).isEqualTo(5);
  }

  @Test
  public void visitSortTest() {
    val parseTree = ParseTrees.createParseTree("count(),sort(-age, weight)");
    val orderContext = (OrderContext) parseTree.getChild(2);
    val sortNode = (SortNode) VISITOR.visitOrder(orderContext);
    assertThat(sortNode.getFields().size()).isEqualTo(2);
    assertThat(sortNode.getFields().get("age")).isEqualTo(Order.DESC);
    assertThat(sortNode.getFields().get("weight")).isEqualTo(Order.ASC);
  }

}
