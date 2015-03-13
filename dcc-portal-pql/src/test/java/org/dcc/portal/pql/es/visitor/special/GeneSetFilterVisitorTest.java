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
package org.dcc.portal.pql.es.visitor.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.AbstractTypeModel.BIOLOGICAL_PROCESS;
import static org.dcc.portal.pql.meta.AbstractTypeModel.CELLULAR_COMPONENT;
import static org.dcc.portal.pql.meta.AbstractTypeModel.MOLECULAR_FUNCTION;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.meta.AbstractTypeModel;
import org.dcc.portal.pql.qe.QueryContext;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class GeneSetFilterVisitorTest {

  GeneSetFilterVisitor visitor = new GeneSetFilterVisitor();
  QueryContext context = new QueryContext();

  @Before
  public void setUp() {
    context.setType(DONOR_CENTRIC);
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
    assertGoTerm(orNode, getTypeModel(DONOR_CENTRIC), "GO:0003674");
  }

  @Test
  public void geneSetIdTest() {
    val root = createEsAst("in(gene.geneSetId, '123')");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    assertGeneSetId(nestedNode.getFirstChild(), getTypeModel(DONOR_CENTRIC), "123");
  }

  @Test
  public void existsCurratedSetTest() {
    val root = createEsAst("exists(gene.curatedSetId)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.curated_set");
  }

  @Test
  public void existsCurratedSetTest_gene() {
    val root = createEsAst("exists(gene.curatedSetId)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "curated_set");
  }

  @Test
  public void existsCurratedSetTest_mutation() {
    val root = createEsAst("exists(gene.curatedSetId)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.curated_set");
  }

  @Test
  public void existsPathwayIdTest() {
    val root = createEsAst("exists(gene.pathwayId)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.pathway");
  }

  @Test
  public void existsPathwayIdTest_gene() {
    val root = createEsAst("exists(gene.pathwayId)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "pathway");
  }

  @Test
  public void existsPathwayIdTest_mutation() {
    val root = createEsAst("exists(gene.pathwayId)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.pathways");
  }

  @Test
  public void existsGoTermTest() {
    val root = createEsAst("exists(gene.GoTerm)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.go_term");
  }

  @Test
  public void existsGoTermTest_gene() {
    val root = createEsAst("exists(gene.GoTerm)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "go_term");
  }

  @Test
  public void existsGoTermTest_mutation() {
    val root = createEsAst("exists(gene.GoTerm)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.go_term");
  }

  @Test
  public void goTermTest_gene() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - OrNode (3 TermsNode)
    val orNode = (OrNode) result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertGoTerm(orNode, getTypeModel(GENE_CENTRIC), "GO:0003674");
  }

  @Test
  public void curatedSetTest_gene() {
    val root = createEsAst("in(curatedSetId, 'ID1')", GENE_CENTRIC);
    assertPathwayAndCuratedSet(root, "curated_set", "ID1");
  }

  @Test
  public void pathwayIdTest_gene() {
    val root = createEsAst("in(pathwayId, 'REACT_6326')", GENE_CENTRIC);
    assertPathwayAndCuratedSet(root, "pathway", "REACT_6326");
  }

  @Test
  public void geneSetIdTest_gene() {
    val root = createEsAst("in(gene.geneSetId, '123')", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - OrNode (3 TermsNode)
    val orNode = (OrNode) result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertGeneSetId(orNode, getTypeModel(GENE_CENTRIC), "123");
  }

  @Test
  public void goTermTest_mutation() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();

    // FilterNode - BoolNode - MustBoolNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val orNode = (OrNode) nestedNode.getFirstChild();
    assertGoTerm(orNode, getTypeModel(MUTATION_CENTRIC), "GO:0003674");
  }

  @Test
  public void curatedSetTest_mutation() {
    val root = createEsAst("in(gene.curatedSetId, 'ID1')", MUTATION_CENTRIC);
    assertPathwayAndCuratedSet(root, "transcript.gene.curated_set", "ID1");
  }

  @Test
  public void pathwayIdTest_mutation() {
    val root = createEsAst("in(gene.pathwayId, 'REACT_6326')", MUTATION_CENTRIC);
    log.debug("After GeneSetFilterVisitor: {}", root);
    assertPathwayAndCuratedSet(root, "transcript.gene.pathways", "REACT_6326");
  }

  @Test
  public void geneSetIdTest_mutation() {
    val root = createEsAst("in(gene.geneSetId, '123')", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // FilterNode - BoolNode - MustBoolNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val orNode = (OrNode) nestedNode.getFirstChild();
    assertGeneSetId(orNode, getTypeModel(MUTATION_CENTRIC), "123");
  }

  private static void assertExists(ExpressionNode node, String value) {
    // FilterNode - BoolNode - MustBoolNode - ExistsNode
    val existsNode = (ExistsNode) node.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    assertThat(existsNode.getField()).isEqualTo(value);
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

  private static Optional<QueryContext> getGeneContextOptional() {
    val result = new QueryContext();
    result.setType(GENE_CENTRIC);

    return Optional.of(result);
  }

  private Optional<QueryContext> getMutationContextOptional() {
    val result = new QueryContext();
    result.setType(MUTATION_CENTRIC);

    return Optional.of(result);
  }

  private static void assertGeneSetId(ExpressionNode orNode, AbstractTypeModel typeModel, String value) {
    assertThat(orNode.childrenCount()).isEqualTo(5);

    assertTermsNode(orNode.getChild(0), typeModel.getInternalField(CELLULAR_COMPONENT), value);
    assertTermsNode(orNode.getChild(1), typeModel.getInternalField(BIOLOGICAL_PROCESS), value);
    assertTermsNode(orNode.getChild(2), typeModel.getInternalField(MOLECULAR_FUNCTION), value);
    assertTermsNode(orNode.getChild(3), typeModel.getField("gene.pathwayId"), value);
    assertTermsNode(orNode.getChild(4), typeModel.getField("gene.curatedSetId"), value);
  }

  private static void assertGoTerm(OrNode orNode, AbstractTypeModel typeModel, String value) {
    assertThat(orNode.childrenCount()).isEqualTo(3);

    assertTermsNode(orNode.getChild(0), typeModel.getInternalField(CELLULAR_COMPONENT), value);
    assertTermsNode(orNode.getChild(1), typeModel.getInternalField(BIOLOGICAL_PROCESS), value);
    assertTermsNode(orNode.getChild(2), typeModel.getInternalField(MOLECULAR_FUNCTION), value);
  }

}
