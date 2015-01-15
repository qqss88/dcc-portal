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

import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.junit.Test;

public class PqlParseTreeVisitorTest {

  PqlParseTreeVisitor visitor = new PqlParseTreeVisitor();

  @Test
  public void visitOrTest() {
    val query = "or(eq(age, 10), eq(grade, 20))";
    val parseTree = ParseTrees.createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0).getChild(0);
    val shouldBoolNode = visitor.visitOr(orContext);
    assertThat(shouldBoolNode.getChildren().size()).isEqualTo(2);

    val leftChild = (TermNode) shouldBoolNode.getChildren().get(0);
    assertThat(leftChild.getNameNode().getPayload()).isEqualTo("age");
    assertThat(leftChild.getValueNode().getPayload()).isEqualTo(10);

    val rightChild = (TermNode) shouldBoolNode.getChildren().get(1);
    assertThat(rightChild.getNameNode().getPayload()).isEqualTo("grade");
    assertThat(rightChild.getValueNode().getPayload()).isEqualTo(20);
  }

  @Test
  public void visitEqTest() {
    // FIXME: how to pass a String?
    val query = "eq(weight, 10.1)";
    val parseTree = ParseTrees.createParseTree(query);
    val eqContext = (EqContext) parseTree.getChild(0).getChild(0);
    val termNode = (TermNode) visitor.visitEq(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getPayload()).isEqualTo("weight");
    assertThat(termNode.getValueNode().getPayload()).isEqualTo(10.1);
  }

}
