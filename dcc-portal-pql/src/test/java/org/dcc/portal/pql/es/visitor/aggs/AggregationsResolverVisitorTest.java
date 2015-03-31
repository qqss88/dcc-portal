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

import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
   * Facets field matches filter. Filter should be copied to facet filter without the matched part.
   */
  @Test
  public void visitTermsFacet_match() {
    val root = (RootNode) createEsAst("facets(gender), eq(gender, 'male'), eq(ageAtDiagnosis, 60)");
    val result = root.accept(resolver, Optional.empty());

    val filterAgg = (FilterAggregationNode) result.getChild(1).getFirstChild();
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
    val root = (RootNode) createEsAst("facets(gender), eq(ageAtDiagnosis, 60)");
    val result = root.accept(resolver, Optional.empty());

    val filterAgg = (FilterAggregationNode) result.getChild(1).getFirstChild();
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
