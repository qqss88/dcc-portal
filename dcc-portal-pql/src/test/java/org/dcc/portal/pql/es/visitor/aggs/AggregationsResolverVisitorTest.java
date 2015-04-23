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
package org.dcc.portal.pql.es.visitor.aggs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.es.utils.Nodes.cloneNode;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.utils.TestingHelpers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class AggregationsResolverVisitorTest {

  private static Optional<Context> CONTEXT = createContext();
  AggregationsResolverVisitor resolver;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    resolver = new AggregationsResolverVisitor();
  }

  @Test
  public void visitRoot_noAggregationsNode() {
    val originalRoot = (RootNode) createEsAst("eq(gender, 'male')");
    val clone = cloneNode(originalRoot);
    val rootNode = resolver.visitRoot(originalRoot, Optional.empty()).get();
    assertThat(clone).isEqualTo(rootNode);
  }

  @Test
  public void visitRoot_noFilters() {
    val originalRoot = (RootNode) createEsAst("facets(id)", Type.MUTATION_CENTRIC);
    val clone = cloneNode(originalRoot);
    val result = originalRoot.accept(resolver, CONTEXT).get();
    assertThat(clone).isEqualTo(result);
  }

  @Test
  public void visitTermsFacet_match() {
    val root = (RootNode) createEsAst("facets(id), eq(id, 'MU1'), eq(start, 60)", Type.MUTATION_CENTRIC);
    val result = root.accept(resolver, CONTEXT).get();
    log.debug("Result: {}", result);

    val filterAgg = (FilterAggregationNode) result.getChild(1).getFirstChild();
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    // FilterNode - BoolNode - MustNode
    val mustNode = filterNode.getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("chromosome_start");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(60);
  }

  /**
   * Facets field does not match filter. TermsFacetNode should not contain filters
   */
  @Test
  public void visitTermsFacet_noMatch() {
    val root = (RootNode) createEsAst("facets(chromosome), eq(id, 60)", Type.MUTATION_CENTRIC);
    val result = root.accept(resolver, CONTEXT).get();

    val filterAgg = (FilterAggregationNode) result.getChild(1).getFirstChild();
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    // FilterNode - BoolNode - MustNode
    val mustNode = filterNode.getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_mutation_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(60);
  }

  //

  @Test
  public void visitRoot_removedFilterTest() {
    val root = (RootNode) createEsAst("facets(id), eq(id, 60)", Type.MUTATION_CENTRIC);
    val result = root.accept(resolver, CONTEXT).get();
    val aggsNode = Nodes.getOptionalChild(result, AggregationsNode.class).get();
    log.debug("Result - \n{}", aggsNode);

    assertThat(aggsNode.childrenCount()).isEqualTo(1);
    val termsAggNode = (TermsAggregationNode) aggsNode.getFirstChild();
    assertThat(termsAggNode.getAggregationName()).isEqualTo("id");
    assertThat(termsAggNode.getFieldName()).isEqualTo("_mutation_id");
  }

  @Test
  public void nestedFieldTest() {
    val result = visit("facets(transcriptId)");
    val nestedAggr = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("transcriptId");
    assertThat(nestedAggr.getPath()).isEqualTo("transcript");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("transcript.id");
  }

  @Test
  public void nestedFieldWithNonNestedFilterTest() {
    val result = visit("facets(transcriptId),eq(id, 'M')");
    val filterAggr = (FilterAggregationNode) result.getFirstChild();
    val mustNode = TestingHelpers.assertBoolAndGetMustNode(filterAggr.getFilters().getFirstChild());
    assertThat(mustNode.childrenCount()).isEqualTo(1);

    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_mutation_id");

    val nestedAggr = (NestedAggregationNode) filterAggr.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("transcriptId");
    assertThat(nestedAggr.getPath()).isEqualTo("transcript");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("transcript.id");
  }

  @Test
  public void nestedAggregationWithNestedFilterTest() {
    val result = visit("facets(consequenceTypeNested),eq(transcriptId, 'T1')");

    val nestedNode = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedNode.getPath()).isEqualTo("transcript");

    val filterNode = (FilterAggregationNode) nestedNode.getFirstChild();
    val filter = filterNode.getFilters();
    val mustNode = TestingHelpers.assertBoolAndGetMustNode(filter.getFirstChild());
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("transcript.id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("T1");

    val termAggregationNode = (TermsAggregationNode) filterNode.getFirstChild();
    assertThat(termAggregationNode.getFieldName()).isEqualTo("transcript.consequence.consequence_type");
  }

  private ExpressionNode visit(String pql) {
    val root = createEsAst(pql, Type.MUTATION_CENTRIC);
    val result = root.accept(resolver, CONTEXT).get();
    log.debug("Result - \n{}", result);

    return Nodes.getOptionalChild(result, AggregationsNode.class).get();
  }

  private static Optional<Context> createContext() {
    return Optional.of(new Context(null, IndexModel.getMutationCentricTypeModel()));
  }

}
