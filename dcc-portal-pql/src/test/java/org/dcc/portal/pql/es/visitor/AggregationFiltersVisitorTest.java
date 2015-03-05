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
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TermsNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.junit.Before;
import org.junit.Test;

public class AggregationFiltersVisitorTest {

  private static final ExpressionNode REMOVE_CHILD = null;
  private static final String AGGREGATION_FIELD = "donor_sex";
  AggregationFiltersVisitor visitor;

  @Before
  public void setUp() {
    visitor = new AggregationFiltersVisitor(AGGREGATION_FIELD);
  }

  @Test
  public void visitTerm_noMatch() {
    val original = (TermNode) getMustNode("eq(ageAtDiagnosis, 60)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitTerm(original, Optional.empty());
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerm_match() {
    val original = (TermNode) getMustNode("eq(gender, 'male')").getFirstChild();
    val result = visitor.visitTerm(original, Optional.empty());
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitRange_noMatch() {
    val original = (RangeNode) getMustNode("gt(ageAtDiagnosis, 60)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitRange(original, Optional.empty());
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitRange_match() {
    val original = (RangeNode) getMustNode("gt(gender, 70)").getFirstChild();
    val result = visitor.visitRange(original, Optional.empty());
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitTerms_noMatch() {
    val original = (TermsNode) getMustNode("in(ageAtDiagnosis, 60, 70)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitTerms(original, Optional.empty());
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerms_match() {
    val original = (TermsNode) getMustNode("in(gender, 'male', 'female')").getFirstChild();
    val result = visitor.visitTerms(original, Optional.empty());
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitAnd_match() {
    val andNode = (AndNode) getMustNode("or(and(gt(ageAtDiagnosis, 60), eq(gender, 70)), eq(ageAtEnrollment, 1))")
        .getFirstChild() // OrNode
        .getFirstChild();
    val andNodeClone = cloneNode(andNode);
    val andNodeResult = visitor.visitAnd(andNode, Optional.empty());

    assertThat(andNodeClone).isNotEqualTo(andNodeResult);
    // eq(gender, 70) removed
    assertThat(andNodeResult.childrenCount()).isEqualTo(1);

    val rangeNode = (RangeNode) andNodeResult.getFirstChild();
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(60);
  }

  @Test
  public void visitOr_match() {
    val orNode = (OrNode) getMustNode("or(gt(ageAtDiagnosis, 60), eq(gender, 70))").getFirstChild();
    val orNodeClone = cloneNode(orNode);
    val orNodeResult = visitor.visitOr(orNode, Optional.empty());

    assertThat(orNodeResult).isNotEqualTo(orNodeClone);
    // eq(gender, 70) removed
    assertThat(orNodeResult.childrenCount()).isEqualTo(1);

    val rangeNode = (RangeNode) orNodeResult.getFirstChild();
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(60);
  }

  private ExpressionNode getMustNode(String query) {
    ExpressionNode filterNode = createEsAst(query).getFirstChild();

    // FilterNode - BoolNode - MustBoolNode
    // Make a clean copy of the must node
    val mustNode = Nodes.cloneNode(filterNode.getFirstChild().getFirstChild());

    return mustNode;
  }

}
