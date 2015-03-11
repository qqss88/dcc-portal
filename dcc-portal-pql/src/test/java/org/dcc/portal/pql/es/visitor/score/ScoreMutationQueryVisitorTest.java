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

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;
import static org.icgc.dcc.portal.model.IndexModel.Type.GENE_CENTRIC;
import static org.icgc.dcc.portal.model.IndexModel.Type.MUTATION_CENTRIC;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ConstantScoreNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.QueryNode;
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
    queryContext = new QueryContext();
    queryContext.setType(MUTATION_CENTRIC);
  }

  @Test
  public void wrongTypeTest() {
    queryContext.setType(GENE_CENTRIC);
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
  public void withQueryNodeTest_noNested() {
    val root = createAst("eq(id, 'ID1')");
    root.addChildren(new QueryNode(root.getFirstChild()));

    log.debug("Before visitor: {}", root);
    val result = visit(root);
    log.debug("{}", result);

    fail("Implement");
  }

  @Test
  public void withQueryNodeTest_withNested() {
    val root = createAst("nested(ssm_occurrence, eq(id, 'ID1'))");
    root.addChildren(new QueryNode(root.getFirstChild()));
    val result = visit(root);
    log.debug("{}", result);

    // Filter below nested node (BoolNode)
    val originalNesteNodeChild = Nodes.cloneNode(getNestedNodeFromQuery(root).getFirstChild());
    log.debug("originalNesteNodeChild: \n{}", originalNesteNodeChild);

    // QueryNode - FilterNode - BoolNode - MustBoolNode - NestedNode
    val nestedNode = getNestedNodeFromQuery(result);
    log.debug("NestedNode: \n{}", nestedNode);
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("ssm_occurrence");
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.TOTAL);

    val constantScoreNode = (ConstantScoreNode) nestedNode.getFirstChild();
    assertThat(constantScoreNode.childrenCount()).isEqualTo(1);
    assertThat(constantScoreNode.getFirstChild()).isEqualTo(originalNesteNodeChild);
  }

  private static NestedNode getNestedNodeFromQuery(ExpressionNode root) {

    val queryNode = Nodes.getOptionalChild(root, QueryNode.class);
    checkState(queryNode.isPresent());

    // QueryNode - FilterNode - BoolNode - MustBoolNode - NestedNode
    return (NestedNode) queryNode.get().getFirstChild().getFirstChild().getFirstChild().getFirstChild();
  }

  private static ExpressionNode createAst(@NonNull String pql) {
    return createEsAst(pql, MUTATION_CENTRIC);
  }

  private ExpressionNode visit(ExpressionNode root) {
    return root.accept(visitor, Optional.of(queryContext)).get();
  }

}
