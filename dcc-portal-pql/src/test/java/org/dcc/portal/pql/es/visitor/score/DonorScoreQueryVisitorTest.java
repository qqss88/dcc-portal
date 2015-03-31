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
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.EsAstTransformator;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.qe.QueryContext;
import org.junit.Test;

@Slf4j
public class DonorScoreQueryVisitorTest {

  DonorScoreQueryVisitor visitor = new DonorScoreQueryVisitor();
  private EsAstTransformator transformator = new EsAstTransformator();
  private QueryContext context = new QueryContext("", Type.DONOR_CENTRIC);

  @Test
  public void noFilterTest() {
    ExpressionNode esAst = createEsAst("select(id)");
    esAst = transform(esAst);

    val result = esAst.accept(visitor, Optional.empty());
    log.debug("{}", result);
    val queryNode = Nodes.getOptionalChild(esAst, QueryNode.class).get();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val nestedNode = (NestedNode) queryNode.getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo(DonorScoreQueryVisitor.PATH);
    assertThat(nestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.TOTAL);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertThat(scoreNode.childrenCount()).isEqualTo(0);
    assertThat(scoreNode.getScript()).isEqualTo(DonorScoreQueryVisitor.SCRIPT);
  }

  @Test
  public void withFilterTest() {
    ExpressionNode esAst = createEsAst("select(id),eq(id, 'DO1')");
    esAst = transform(esAst);

    val result = esAst.accept(visitor, Optional.empty()).get();
    log.debug("{}", result);
    val queryNode = Nodes.getOptionalChild(esAst, QueryNode.class).get();
    assertThat(queryNode.childrenCount()).isEqualTo(1);
    BoolNode boolNode = (BoolNode) queryNode.getFirstChild();
    assertThat(boolNode.childrenCount()).isEqualTo(1);
    MustBoolNode mustNode = (MustBoolNode) boolNode.getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    val nestedNode = mustNode.getFirstChild();
    assertThat(nestedNode.getFirstChild().childrenCount()).isEqualTo(0);

    val filterNode = mustNode.getChild(1);
    assertThat(filterNode.childrenCount()).isEqualTo(1);
    boolNode = (BoolNode) filterNode.getFirstChild();
    assertThat(boolNode.childrenCount()).isEqualTo(1);
    mustNode = (MustBoolNode) boolNode.getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(1);

    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_donor_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("DO1");
  }

  @Test
  public void withNestedFilterTest() {
    ExpressionNode esAst = createEsAst("select(id),eq(gene.id, 'DO1')");
    esAst = transform(esAst);

    val result = esAst.accept(visitor, Optional.empty()).get();
    log.debug("{}", result);

    // QueryNode - Bool - Must
    val mustNode = result.getFirstChild() // Query
        .getFirstChild() // Bool
        .getFirstChild(); // Must

    // Nested = FunctionScore - FilterNode
    val nestedFilterNode = mustNode.getFirstChild() // Nested
        .getFirstChild() // FunctionScore
        .getFirstChild(); // Filter

    val termNode = (TermNode) nestedFilterNode.getFirstChild().getFirstChild().getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("gene._gene_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("DO1");

    val mustNode2 = mustNode.getChild(1).getFirstChild().getFirstChild();
    assertThat(mustNode2.childrenCount()).isEqualTo(0);
  }

  @Test
  public void filterTest_noNesting() {
    ExpressionNode esAst =
        createEsAst("select(id),eq(id, 'DO1'), nested(gene.ssm, eq(gene.type, 'protein')), gt(gene.id, 'G1')");
    esAst = transform(esAst);

    val result = esAst.accept(visitor, Optional.empty()).get();
    log.debug("{}", result);
  }

  @Test
  public void breaking() {
    ExpressionNode esAst =
        createEsAst(
            "nested(gene,and(nested(gene.ssm,and(nested(gene.ssm.consequence,in(mutation.consequenceType,'start_lost'),in(mutation.functionalImpact,'High')),in(mutation.type,'single base substitution'))),in(gene.id,'ENSG00000182909')))",
            Type.DONOR_CENTRIC);
    esAst = transform(esAst);

    val result = esAst.accept(visitor, Optional.empty()).get();
    log.debug("{}", result);
  }

  private ExpressionNode transform(ExpressionNode root) {
    ExpressionNode result = transformator.resolveSpecialCases(root, context);
    result = transformator.resolveFacets(result);
    log.debug("[transform] {}", result);

    return result;
  }
}
