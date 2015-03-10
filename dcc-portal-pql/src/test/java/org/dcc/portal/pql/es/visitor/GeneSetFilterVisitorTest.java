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
package org.dcc.portal.pql.es.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.qe.QueryContext;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class GeneSetFilterVisitorTest {

  GeneSetFilterVisitor visitor = new GeneSetFilterVisitor();
  QueryContext context = new QueryContext();

  @Before
  public void setUp() {
    context.setType(Type.DONOR_CENTRIC);
  }

  @Test
  public void pathwayIdTest() {
    val root = createEsAst("in(gene.pathwayId, 'REACT_6326')");
    assertPathwayAndCuratedSet(root, "gene.pathway", "REACT_6326");
  }

  @Test
  public void curatedSetTest() {
    val root = createEsAst("in(gene.curatedSetId, 'ID1')");
    assertPathwayAndCuratedSet(root, "gene.curated_set", "ID1");
  }

  @Test
  public void goTermTest() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val orNode = (OrNode) nestedNode.getFirstChild();
    assertThat(orNode.childrenCount()).isEqualTo(3);

    assertTermsNode(orNode.getChild(0), "gene.go_term.cellular_component", "GO:0003674");
    assertTermsNode(orNode.getChild(1), "gene.go_term.biological_process", "GO:0003674");
    assertTermsNode(orNode.getChild(2), "gene.go_term.molecular_function", "GO:0003674");
  }

  @Test
  public void geneSetIdTest() {
    val root = createEsAst("in(gene.geneSetId, '123')");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val orNode = (OrNode) nestedNode.getFirstChild();
    assertThat(orNode.childrenCount()).isEqualTo(5);

    assertTermsNode(orNode.getChild(0), "gene.go_term.cellular_component", "123");
    assertTermsNode(orNode.getChild(1), "gene.go_term.biological_process", "123");
    assertTermsNode(orNode.getChild(2), "gene.go_term.molecular_function", "123");
    assertTermsNode(orNode.getChild(3), "gene.pathway", "123");
    assertTermsNode(orNode.getChild(4), "gene.curated_set", "123");
  }

  private static void assertTermsNode(ExpressionNode node, String fieldName, String value) {
    val child = getTermsNode(node);
    assertThat(child.getField()).isEqualTo(fieldName);
    assertThat(getTerminalNode(child).getValue()).isEqualTo(value);
  }

  private static void assertPathwayAndCuratedSet(ExpressionNode root, String fieldName, String value) {
    // FilterNode - BoolNode - MustBoolNode - TermsNode
    val termsNode = (TermsNode) root.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(termsNode.getField()).isEqualTo(fieldName);
    assertThat(termsNode.childrenCount()).isEqualTo(1);

    val terminalNode = (TerminalNode) termsNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(value);
  }

  private static TermsNode getTermsNode(ExpressionNode termsNode) {
    return (TermsNode) termsNode;
  }

  private static TerminalNode getTerminalNode(ExpressionNode termsNode) {
    return (TerminalNode) termsNode.getFirstChild();
  }

}
