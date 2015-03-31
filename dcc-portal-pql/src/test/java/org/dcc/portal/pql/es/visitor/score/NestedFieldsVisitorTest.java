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
package org.dcc.portal.pql.es.visitor.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.meta.IndexModel;
import org.junit.Test;

@Slf4j
public class NestedFieldsVisitorTest {

  NestedFieldsVisitor visitor = new NestedFieldsVisitor();
  NestedNode nestedNode = ScoreQueryVisitor.createdFunctionScoreNestedNode("script", "gene");

  @Test
  public void noNestedFilterTest() {
    val esAst = new FilterNode(new TermNode("id", "1"));
    log.debug("ES AST: \n{}", esAst);

    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getDonorCentricTypeModel(), nestedNode);
    val nestedNode = (NestedNode) esAst.accept(visitor, Optional.of(requestContext)).get();
    log.debug("Result -\n{}", nestedNode);
    assertThat(nestedNode.getPath()).isEqualTo("gene");
    assertThat(nestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.TOTAL);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertThat(scoreNode.getScript()).isEqualTo("script");
  }

  @Test
  public void nestedNodeMultiChildrenTest() {
    val esAst = new FilterNode(new NestedNode("gene",
        new BoolNode(new MustBoolNode(
            new TermNode("gene.start", "1"),
            new TermNode("gene.end", "2")))));
    log.debug("ES AST: \n{}", esAst);
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getDonorCentricTypeModel(), nestedNode);
    val nestedNode = (NestedNode) esAst.accept(visitor, Optional.of(requestContext)).get();
    log.debug("Result -\n{}", nestedNode);

    assertThat(nestedNode.getPath()).isEqualTo("gene");
    assertThat(nestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.TOTAL);
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertThat(scoreNode.getScript()).isEqualTo("script");
    assertThat(scoreNode.childrenCount()).isEqualTo(1);

    // ScoreNode - FilterNode - BoolNode - MustBoolNode
    val mustNode = (MustBoolNode) scoreNode.getFirstChild().getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    val startNode = (TermNode) mustNode.getFirstChild();
    assertThat(startNode.getNameNode().getValueAsString()).isEqualTo("gene.start");
    assertThat(startNode.getValueNode().getValue()).isEqualTo("1");

    val endNode = (TermNode) mustNode.getChild(1);
    assertThat(endNode.getNameNode().getValueAsString()).isEqualTo("gene.end");
    assertThat(endNode.getValueNode().getValue()).isEqualTo("2");
  }

  @Test
  public void deepNestingTest() {
    val esAst = new FilterNode(new NestedNode("gene.ssm", new TermNode("mutation.id", "1")));
    log.debug("ES AST: \n{}", esAst);
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getDonorCentricTypeModel(), nestedNode);
    val nestedNode = (NestedNode) esAst.accept(visitor, Optional.of(requestContext)).get();
    log.debug("Result -\n{}", nestedNode);

    assertThat(nestedNode.getPath()).isEqualTo("gene");
    assertThat(nestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.TOTAL);
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertThat(scoreNode.getScript()).isEqualTo("script");
    assertThat(scoreNode.childrenCount()).isEqualTo(1);

    // ScoreNode - FilterNode - TermNode
    val filterNode = (FilterNode) scoreNode.getFirstChild();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    val ssmNestedNode = (NestedNode) filterNode.getFirstChild();
    assertThat(ssmNestedNode.childrenCount()).isEqualTo(1);
    assertThat(ssmNestedNode.getPath()).isEqualTo("gene.ssm");
    assertThat(ssmNestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.AVG);

    val termNode = (TermNode) ssmNestedNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("mutation.id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("1");
  }

}
