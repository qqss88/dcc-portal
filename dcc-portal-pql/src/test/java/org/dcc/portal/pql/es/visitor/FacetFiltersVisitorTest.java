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

import org.dcc.portal.pql.es.ast.AndNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.OrNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TermsNode;
import org.junit.Before;
import org.junit.Test;

public class FacetFiltersVisitorTest {

  private static final ExpressionNode REMOVE_CHILD = null;
  private static final String FACET_FIELD = "gender";
  FacetFiltersVisitor visitor;

  @Before
  public void setUp() {
    visitor = new FacetFiltersVisitor(FACET_FIELD);
  }

  @Test
  public void visitTerm_noMatch() {
    val original = (TermNode) getMustNode("eq(age, 60)").getChild(0);
    val clone = cloneNode(original);
    val result = visitor.visitTerm(original);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerm_match() {
    val original = (TermNode) getMustNode("eq(gender, 'male')").getChild(0);
    val result = visitor.visitTerm(original);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitRange_noMatch() {
    val original = (RangeNode) getMustNode("gt(age, 60)").getChild(0);
    val clone = cloneNode(original);
    val result = visitor.visitRange(original);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitRange_match() {
    val original = (RangeNode) getMustNode("gt(gender, 70)").getChild(0);
    val result = visitor.visitRange(original);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitTerms_noMatch() {
    val original = (TermsNode) getMustNode("in(age, 60, 70)").getChild(0);
    val clone = cloneNode(original);
    val result = visitor.visitTerms(original);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerms_match() {
    val original = (TermsNode) getMustNode("in(gender, 'male', 'female')").getChild(0);
    val result = visitor.visitTerms(original);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitAnd_noMatch() {
    val original = (AndNode) getMustNode("or(and(gt(age, 60), eq(gender, 70)), eq(a, 1))").getChild(0).getChild(0);
    val clone = cloneNode(original);
    val result = visitor.visitAnd(original);
    assertThat(clone).isNotEqualTo(result);
    assertThat(result.childrenCount()).isEqualTo(1);

    val rangeNode = (RangeNode) result.getChild(0);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(60);
  }

  @Test
  public void visitOr_noMatch() {
    val original = (OrNode) getMustNode("or(gt(age, 60), eq(gender, 70))").getChild(0);
    val clone = cloneNode(original);
    val result = visitor.visitOr(original);
    assertThat(result).isNotEqualTo(clone);
    assertThat(result.childrenCount()).isEqualTo(1);

    val rangeNode = (RangeNode) result.getChild(0);
    assertThat(rangeNode.getName()).isEqualTo("age");
    val gtNode = (GreaterThanNode) rangeNode.getChild(0);
    assertThat(gtNode.getValue()).isEqualTo(60);
  }

  private static ExpressionNode getMustNode(String query) {
    val root = (RootNode) createEsAst(query);

    return root.getChild(0).getChild(0).getChild(0);
  }

}
