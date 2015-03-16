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
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.query.ConstantScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.qe.QueryContext;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class ScoreMutationQueryVisitorTest {

  ScoreMutatationQueryVisitor visitor = new ScoreMutatationQueryVisitor();
  QueryContext queryContext;

  @Before
  public void setUp() {
    queryContext = new QueryContext("", MUTATION_CENTRIC);
  }

  @Test
  public void wrongTypeTest() {
    queryContext = new QueryContext("", GENE_CENTRIC);
    val root = createEsAst("select(id)", GENE_CENTRIC);
    val rootClone = Nodes.cloneNode(root);
    val result = visit(rootClone);
    log.debug("{}", result);

    assertThat(result).isEqualTo(root);
  }

  @Test
  public void noQueryNodeTest() {
    val root = createAst("select(id)");
    val result = visit(root);
    log.debug("{}", result);

    val queryNode = Nodes.getOptionalChild(result, QueryNode.class).get();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val nestedNode = (NestedNode) queryNode.getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("ssm_occurrence");
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.TOTAL);

    val constantScoreNode = (ConstantScoreNode) nestedNode.getFirstChild();
    assertThat(constantScoreNode.childrenCount()).isEqualTo(0);
    assertThat(constantScoreNode.getBoost()).isEqualTo(1f);
  }

  @Test
  public void withQueryNodeTest_withFilter() {
    val root = createAst("eq(id, 'ID1')");
    val originalFilter = Nodes.cloneNode(root.getFirstChild());
    root.addChildren(new QueryNode(root.getFirstChild()));
    val result = visit(root);
    log.debug("{}", result);

    // QueryNode - NestedNode - ConstantScore - FilterNode
    val nestedNode = (NestedNode) result.getChild(1).getFirstChild();
    log.debug("NestedNode: \n{}", nestedNode);
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("ssm_occurrence");
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.TOTAL);

    val constantScoreNode = (ConstantScoreNode) nestedNode.getFirstChild();
    assertThat(constantScoreNode.getBoost()).isEqualTo(1f);
    assertThat(constantScoreNode.childrenCount()).isEqualTo(1);
    assertThat(constantScoreNode.getFirstChild()).isEqualTo(originalFilter);
  }

  private static ExpressionNode createAst(@NonNull String pql) {
    return createEsAst(pql, MUTATION_CENTRIC);
  }

  private ExpressionNode visit(ExpressionNode root) {
    return root.accept(visitor, Optional.of(queryContext)).get();
  }

}
