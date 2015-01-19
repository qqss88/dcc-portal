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
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.LessEqualNode;
import org.dcc.portal.pql.es.ast.LessThanNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.junit.Test;

@Slf4j
public class PqlParseTreeVisitorTest {

  private static final PqlParseTreeVisitor VISITOR = new PqlParseTreeVisitor();

  @Test
  public void visitOrTest() {
    val query = "or(eq(age, 10), eq(grade, 20))";
    val parseTree = ParseTrees.createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0).getChild(0);
    val shouldBoolNode = VISITOR.visitOr(orContext);
    assertThat(shouldBoolNode.getChildren().size()).isEqualTo(2);

    val leftChild = (TermNode) shouldBoolNode.getChildren().get(0);
    assertThat(leftChild.getNameNode().getValue()).isEqualTo("age");
    assertThat(leftChild.getValueNode().getValue()).isEqualTo(10);

    val rightChild = (TermNode) shouldBoolNode.getChildren().get(1);
    assertThat(rightChild.getNameNode().getValue()).isEqualTo("grade");
    assertThat(rightChild.getValueNode().getValue()).isEqualTo(20);
  }

  @Test
  public void visitEqTest() {
    val query = "eq(weight, 10.1)";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqContext) parseTree.getChild(0).getChild(0);
    val termNode = (TermNode) VISITOR.visitEq(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("weight");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10.1);
  }

  @Test
  public void visitGeTest() {
    val query = "ge(weight, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree(query);
    // program - query
    val geContext = (GeContext) parseTree.getChild(0).getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGe(geContext);
    log.info("RangeNode: {}", rangeNode);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val geNode = (GreaterEqualNode) rangeNode.getChild(0);
    assertThat(geNode.getChildren().size()).isEqualTo(1);
    assertThat(geNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) geNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitGtTest() {
    val query = "gt(weight, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree(query);
    // program - query
    val gtContext = (GtContext) parseTree.getChild(0).getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGt(gtContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getChildren().size()).isEqualTo(1);
    assertThat(gtNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) gtNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLeTest() {
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree("le(weight, 10)");
    // program - query
    val leContext = (LeContext) parseTree.getChild(0).getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLe(leContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val leNode = (LessEqualNode) rangeNode.getChild(0);
    assertThat(leNode.getChildren().size()).isEqualTo(1);
    assertThat(leNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) leNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLtTest() {
    // root - postFilter - bool - must - range - from
    val parseTree = ParseTrees.createParseTree("lt(weight, 10)");
    // program - query
    val ltContext = (LtContext) parseTree.getChild(0).getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLt(ltContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getName()).isEqualTo("weight");

    val toNode = (LessThanNode) rangeNode.getChild(0);
    assertThat(toNode.getChildren().size()).isEqualTo(1);
    assertThat(toNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) toNode.getChild(0);
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

}
