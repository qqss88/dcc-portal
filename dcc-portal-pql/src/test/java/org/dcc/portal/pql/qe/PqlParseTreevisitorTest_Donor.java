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
import static org.dcc.portal.pql.utils.TestingHelpers.createParseTree;
import lombok.val;

import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.meta.DonorCentricTypeModel;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterEqualContext;
import org.junit.Test;

public class PqlParseTreevisitorTest_Donor {

  private static final PqlParseTreeVisitor VISITOR = new PqlParseTreeVisitor(new DonorCentricTypeModel());

  @Test
  public void visitEqualTest() {
    val query = "eq(ageAtDiagnosis, 10.1)";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val termNode = (TermNode) VISITOR.visitEqual(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_age_at_diagnosis");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10.1);
  }

  @Test
  public void visitEqualTest_gene() {
    val query = "eq(gene.id, 'T1')";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val termNode = (TermNode) VISITOR.visitEqual(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("gene._gene_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("T1");
  }

  @Test
  public void visitEqualTest_mutation() {
    val query = "eq(mutation.consequenceType, 'T1')";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val termNode = (TermNode) VISITOR.visitEqual(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("gene.ssm.consequence.consequence_type");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("T1");
  }

  @Test
  public void visitGreaterEqualTest_mutation() {
    val query = "ge(mutation.consequenceType, 'T1')";
    val parseTree = createParseTree(query);
    val eqContext = (GreaterEqualContext) parseTree.getChild(0);
    val termNode = (RangeNode) VISITOR.visitGreaterEqual(eqContext);

    assertThat(termNode.getFieldName()).isEqualTo("gene.ssm.consequence.consequence_type");
  }

}
