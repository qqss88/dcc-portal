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
import static org.dcc.portal.pql.es.utils.Nodes.cloneNode;
import static org.dcc.portal.pql.es.utils.Nodes.getOptionalChild;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class AggregationsResolverVisitorTest {

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
    val rootNode = resolver.visitRoot(originalRoot, Optional.empty());
    assertThat(clone).isEqualTo(rootNode);
  }

  @Test
  public void visitRoot_noFilters() {
    val originalRoot = (RootNode) createEsAst("facets(gender)");
    val clone = cloneNode(originalRoot);
    val result = resolver.visitRoot(originalRoot, Optional.empty());
    assertThat(clone).isEqualTo(result);
  }

  /**
   * Check that filters were moved to a {@link QueryNode}
   */
  @Test
  public void visitRoot_moveFilters() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(ageAtDiagnosis, 60)");
    val filterNode = cloneNode(originalRoot.getFirstChild());
    val result = resolver.visitRoot(originalRoot, Optional.empty());

    // Children: Aggregations and Query
    assertThat(result.childrenCount()).isEqualTo(2);

    val aggsNode = (AggregationsNode) result.getFirstChild();
    log.debug("Aggregations Node: {}", aggsNode);
    assertThat(aggsNode.childrenCount()).isEqualTo(1);
    val filterAggsNode = (FilterAggregationNode) aggsNode.getFirstChild();
    assertThat(filterAggsNode.childrenCount()).isEqualTo(1);
    assertThat(filterAggsNode.getFirstChild()).isInstanceOf(TermsAggregationNode.class);
    assertThat(filterAggsNode.getFilters()).isEqualTo(filterNode);

    val queryNode = (QueryNode) result.getChild(1);
    log.debug("QueryNode: {}", queryNode);
    assertThat(queryNode.getFirstChild()).isEqualTo(filterNode);
    assertThat(queryNode.childrenCount()).isEqualTo(1);
    assertThat(getOptionalChild(result, FilterNode.class).isPresent()).isFalse();
  }

  /**
   * Facets field matches filter. Filter should be copied to facet filter without the matched part.
   */
  @Test
  public void visitTermsFacet_match() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(gender, 'male'), eq(ageAtDiagnosis, 60)");
    val aggsNodeOpt = getOptionalChild(originalRoot, AggregationsNode.class);
    assertThat(aggsNodeOpt.isPresent()).isTrue();

    val filterAgg = (FilterAggregationNode) resolver.visitTermsAggregation(
        (TermsAggregationNode) aggsNodeOpt.get().getFirstChild(), Optional.empty());
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    // FilterNode - BoolNode - MustNode
    val mustNode = filterNode.getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_age_at_diagnosis");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(60);
  }

  /**
   * Facets field does not match filter. TermsFacetNode should not contain filters
   */
  @Test
  public void visitTermsFacet_noMatch() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(ageAtDiagnosis, 60)");
    val aggsNodeOpt = getOptionalChild(originalRoot, AggregationsNode.class);
    assertThat(aggsNodeOpt.isPresent()).isTrue();

    val filterAgg = (FilterAggregationNode) resolver.visitTermsAggregation(
        (TermsAggregationNode) aggsNodeOpt.get().getFirstChild(), Optional.empty());
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    // FilterNode - BoolNode - MustNode
    val mustNode = filterNode.getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_age_at_diagnosis");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(60);
  }

}
