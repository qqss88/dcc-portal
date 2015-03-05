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
import org.dcc.portal.pql.es.ast.FunctionScoreQueryNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.AggregationsResolverVisitor;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Test;

@Slf4j
public class DonorScoreQueryVisitorTest {

  DonorScoreQueryVisitor visitor = new DonorScoreQueryVisitor();
  private final AggregationsResolverVisitor facetsResolver = new AggregationsResolverVisitor();

  @Test
  public void visitRootTest_withQueryNode() {
    ExpressionNode esAst = createEsAst("facets(id), eq(id, 1)");
    esAst = facetsResolver.resolveAggregations(esAst, Type.DONOR_CENTRIC);
    val origFilterNode = Nodes.cloneNode(esAst.getChild(1).getFirstChild());

    val result = esAst.accept(visitor, Optional.empty());
    assertCorrectStructure(result);
    val filterNode = result.getChild(1).getFirstChild().getFirstChild().getFirstChild();
    assertThat(filterNode).isEqualTo(origFilterNode);
  }

  @Test
  public void visitRootTest_withoutQueryNode() {
    val esAst = createEsAst("select(id)");
    val result = esAst.accept(visitor, Optional.empty());
    log.info("{}", result);
    assertCorrectStructure(result);
  }

  private static void assertCorrectStructure(ExpressionNode root) {
    val queryNodes = Nodes.filterChildren(root, QueryNode.class);
    assertThat(queryNodes).hasSize(1);

    val queryNode = queryNodes.get(0);
    assertThat(queryNode.childrenCount()).isEqualTo(1);
    val netstedNode = (NestedNode) queryNode.getFirstChild();
    assertThat(netstedNode.childrenCount()).isEqualTo(1);
    assertThat(netstedNode.getPath()).isEqualTo("gene");
    assertThat(netstedNode.getScoreMode()).isEqualTo("total");

    val functionScoreNode = (FunctionScoreQueryNode) netstedNode.getFirstChild();
    assertThat(functionScoreNode.getScript()).isEqualTo(DonorScoreQueryVisitor.SCRIPT);
  }

}
