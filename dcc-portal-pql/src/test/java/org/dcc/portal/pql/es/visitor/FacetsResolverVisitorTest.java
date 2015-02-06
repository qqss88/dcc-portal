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
import static org.dcc.portal.pql.utils.TestingHelpers.cloneNode;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.FacetsNode;
import org.dcc.portal.pql.es.ast.FilterNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TermsFacetNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class FacetsResolverVisitorTest {

  FacetsResolverVisitor resolver;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    resolver = new FacetsResolverVisitor(Type.DONOR_CENTRIC);
  }

  @Test
  public void visitRoot_noFacetsNode() {
    val originalRoot = (RootNode) createEsAst("eq(gender, 'male')");
    val clone = cloneNode(originalRoot);
    val rootNode = resolver.visitRoot(originalRoot);
    assertThat(clone).isEqualTo(rootNode);
  }

  @Test
  public void visitRoot_noFilters() {
    val originalRoot = (RootNode) createEsAst("facets(gender)");
    val clone = cloneNode(originalRoot);
    val result = resolver.visitRoot(originalRoot);
    assertThat(clone).isEqualTo(result);
  }

  /**
   * Check that filters were moved to a {@link QueryNode}
   */
  @Test
  public void visitRoot_moveFilters() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(gender, 'male')");
    val originalFilterNode = (FilterNode) originalRoot.getChild(0);
    val rootNode = resolver.visitRoot(originalRoot);

    assertThat(rootNode.childrenCount()).isEqualTo(2);
    val facetsNode = (FacetsNode) rootNode.getChild(0);
    log.info("Facets Node: {}", facetsNode);
    assertThat(facetsNode.childrenCount()).isEqualTo(1);
    assertThat(facetsNode.getChild(0).getChild(0)).isEqualTo(originalFilterNode);

    val queryNode = (QueryNode) rootNode.getChild(1);
    assertThat(queryNode.getChild(0)).isEqualTo(originalFilterNode);
    assertThat(queryNode.childrenCount()).isEqualTo(1);
    assertThat(Nodes.getChildOptional(rootNode, FilterNode.class).isPresent()).isFalse();
  }

  /**
   * Facets field does not match filter. Filter should be copied to facet filter without modifications
   */
  @Test
  public void visitTermsFacet_noMatch() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(age, 60)");
    val facetsNodeOpt = Nodes.getChildOptional(originalRoot, FacetsNode.class);
    assertThat(facetsNodeOpt.isPresent()).isTrue();
    val originalFilterNode = (FilterNode) originalRoot.getChild(0);
    val clone = cloneNode(originalFilterNode);
    val termsFacet = resolver.visitTermsFacet((TermsFacetNode) facetsNodeOpt.get().getChild(0));
    assertThat(termsFacet.childrenCount()).isEqualTo(1);
    assertThat(termsFacet.getChild(0)).isEqualTo(clone);
  }

  /**
   * Facets field matches filter. Filter should be copied to facet filter without the matched part.
   */
  @Test
  public void visitTermsFacet_Match() {
    val originalRoot = (RootNode) createEsAst("facets(gender), eq(gender, 'male'), eq(age, 60)");
    val facetsNodeOpt = Nodes.getChildOptional(originalRoot, FacetsNode.class);
    assertThat(facetsNodeOpt.isPresent()).isTrue();
    val originalFilterNode = (FilterNode) originalRoot.getChild(0);
    val clone = cloneNode(originalFilterNode);
    val termsFacet = resolver.visitTermsFacet((TermsFacetNode) facetsNodeOpt.get().getChild(0));
    assertThat(termsFacet.childrenCount()).isEqualTo(1);
    assertThat(termsFacet.getChild(0)).isNotEqualTo(clone);
  }

  @Test
  public void resolveFacetsTest_malformedRootNode() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Source AST must be an instance of RootNode");
    val esAst = createEsAst("facets(gender)");
    resolver.resolveFacets(esAst.getChild(0), Type.DONOR_CENTRIC);
  }

}
