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
package org.dcc.portal.pql.qe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.utils.TestingHelpers.createParseTree;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.meta.DonorCentricTypeModel;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ExistsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FacetsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.InArrayContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.MissingContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NestedContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrderContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.RangeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.SelectContext;
import org.junit.Test;

@Slf4j
public class PqlParseTreeVisitorTest {

  private static final PqlParseTreeVisitor VISITOR = new PqlParseTreeVisitor(new DonorCentricTypeModel());

  @Test
  public void visitOrTest_simple() {
    val query = "or(gt(ageAtDiagnosis, 10), lt(ageAtEnrollment, 50), eq(gender, 'male'))";
    val parseTree = createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0);
    val orNode = (OrNode) VISITOR.visitOr(orContext);
    assertThat(orNode.childrenCount()).isEqualTo(3);

    // gt(ageAtDiagnosis, 10)
    RangeNode rangeNode = (RangeNode) orNode.getFirstChild();
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(ageAtEnrollment, 50)
    rangeNode = (RangeNode) orNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_enrollment");
    val ltNode = (LessThanNode) rangeNode.getFirstChild();
    assertThat(ltNode.getValue()).isEqualTo(50);

    // eq(gender, 'male')
    val termNode = (TermNode) orNode.getChild(2);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");
  }

  @Test
  public void visitOrTest_withAnd() {
    val query = "or(eq(gender, 'male'), and(gt(ageAtDiagnosis, 10), lt(ageAtEnrollment, 50)))";
    val parseTree = createParseTree(query);
    val orContext = (OrContext) parseTree.getChild(0);
    val orNode = (OrNode) VISITOR.visitOr(orContext);
    assertThat(orNode.childrenCount()).isEqualTo(2);

    // eq(gender, 'male')
    val termNode = (TermNode) orNode.getFirstChild();
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");

    val andNode = (AndNode) orNode.getChild(1);
    assertThat(andNode.childrenCount()).isEqualTo(2);

    // gt(ageAtDiagnosis, 10)
    RangeNode rangeNode = (RangeNode) andNode.getFirstChild();
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(ageAtEnrollment, 50)
    rangeNode = (RangeNode) andNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_enrollment");
    val ltNode = (LessThanNode) rangeNode.getFirstChild();
    assertThat(ltNode.getValue()).isEqualTo(50);
  }

  @Test
  public void visitValueTest_string_single_quote() {
    val query = "eq(gender, 'male')";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo("male");
  }

  @Test
  public void visitValueTest_string_double_quote() {
    val query = "eq(sex, \"male\")";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo("male");
  }

  @Test
  public void visitValueTest_int() {
    val query = "eq(age, 20)";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo(20);
  }

  @Test
  public void visitValueTest_double() {
    val query = "eq(age, 20.555)";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val valueContext = eqContext.eq().value();
    val termNode = (TerminalNode) VISITOR.visitValue(valueContext);
    assertThat(termNode.getValue()).isEqualTo(20.555);
  }

  @Test
  public void visitAndTest_simple() {
    val query = "and(gt(ageAtDiagnosis, 10), lt(ageAtEnrollment, 50), eq(gender, 'male'))";
    val parseTree = createParseTree(query);
    val andContext = (AndContext) parseTree.getChild(0);
    val andNode = (AndNode) VISITOR.visitAnd(andContext);
    assertThat(andNode.getChildren().size()).isEqualTo(3);

    // gt(ageAtDiagnosis, 10)
    RangeNode rangeNode = (RangeNode) andNode.getFirstChild();
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(ageAtEnrollment, 50)
    rangeNode = (RangeNode) andNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_enrollment");
    val ltNode = (LessThanNode) rangeNode.getFirstChild();
    assertThat(ltNode.getValue()).isEqualTo(50);

    // eq(gender, 'male')
    val termNode = (TermNode) andNode.getChild(2);
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");
  }

  @Test
  public void visitAndTest_withOr() {
    val query = "and(eq(gender, 'male'), or(gt(ageAtDiagnosis, 10), lt(ageAtEnrollment, 50)))";
    val parseTree = createParseTree(query);
    val andContext = (AndContext) parseTree.getChild(0);
    val andNode = (AndNode) VISITOR.visitAnd(andContext);
    assertThat(andNode.getChildren().size()).isEqualTo(2);

    // eq(gender, 'male')
    val termNode = (TermNode) andNode.getFirstChild();
    assertThat(termNode.childrenCount()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("male");

    val orNode = (OrNode) andNode.getChild(1);
    assertThat(orNode.childrenCount()).isEqualTo(2);

    // gt(ageAtDiagnosis, 10)
    RangeNode rangeNode = (RangeNode) orNode.getFirstChild();
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(10);

    // lt(ageAtEnrollment, 50)
    rangeNode = (RangeNode) orNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_enrollment");
    val ltNode = (LessThanNode) rangeNode.getFirstChild();
    assertThat(ltNode.getValue()).isEqualTo(50);
  }

  @Test
  public void visitEqualTest() {
    val query = "eq(ageAtDiagnosis, 10.1)";
    val parseTree = createParseTree(query);
    val eqContext = (EqualContext) parseTree.getChild(0);
    val termNode = (TermNode) VISITOR.visitEqual(eqContext);

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_age_at_diagnosis");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10.1);
  }

  @Test
  public void visitNotEqualTest() {
    val query = "ne(gender, 10)";
    val parseTree = createParseTree(query);
    val notEqContext = (NotEqualContext) parseTree.getChild(0);
    val notNode = (NotNode) VISITOR.visitNotEqual(notEqContext);
    val termNode = (TermNode) notNode.getFirstChild();

    assertThat(termNode.getChildren().size()).isEqualTo(2);
    assertThat(termNode.getNameNode().getValue()).isEqualTo("donor_sex");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(10);
  }

  @Test
  public void visitGreaterEqualTest() {
    val query = "ge(ageAtDiagnosis, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = createParseTree(query);
    // program - query
    val geContext = (GreaterEqualContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGreaterEqual(geContext);

    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");

    val geNode = (GreaterEqualNode) rangeNode.getFirstChild();
    assertThat(geNode.getChildren().size()).isEqualTo(1);
    assertThat(geNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) geNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitGreaterThanTest() {
    val query = "gt(ageAtDiagnosis, 10)";
    // root - postFilter - bool - must - range - from
    val parseTree = createParseTree(query);
    // program - query
    val gtContext = (GreaterThanContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitGreaterThan(gtContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");

    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getChildren().size()).isEqualTo(1);
    assertThat(gtNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) gtNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLessEqualTest() {
    // root - postFilter - bool - must - range - from
    val parseTree = createParseTree("le(ageAtDiagnosis, 10)");
    // program - query
    val leContext = (LessEqualContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLessEqual(leContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");

    val leNode = (LessEqualNode) rangeNode.getFirstChild();
    assertThat(leNode.getChildren().size()).isEqualTo(1);
    assertThat(leNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) leNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitLessThanTest() {
    val parseTree = createParseTree("lt(ageAtDiagnosis, 10)");
    val ltContext = (LessThanContext) parseTree.getChild(0);
    val rangeNode = (RangeNode) VISITOR.visitLessThan(ltContext);
    assertThat(rangeNode.getChildren().size()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_diagnosis");

    val toNode = (LessThanNode) rangeNode.getFirstChild();
    assertThat(toNode.getChildren().size()).isEqualTo(1);
    assertThat(toNode.getValue()).isEqualTo(10);

    val terminalNode = (TerminalNode) toNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(10);
  }

  @Test
  public void visitSelectTest() {
    val parseTree = createParseTree("select(ageAtDiagnosis, gender)");
    val selectContext = (SelectContext) parseTree.getChild(0);
    val fieldsNode = (FieldsNode) VISITOR.visitSelect(selectContext);
    assertThat(fieldsNode.childrenCount()).isEqualTo(2);
    assertThat(fieldsNode.getFields()).containsOnly("donor_age_at_diagnosis", "donor_sex");
  }

  @Test
  public void visitLimitTest() {
    val parseTree = createParseTree("select(gender),limit(1, 5)");
    val rangeContext = (RangeContext) parseTree.getChild(2);
    val limitNode = (LimitNode) VISITOR.visitRange(rangeContext);
    assertThat(limitNode.getFrom()).isEqualTo(1);
    assertThat(limitNode.getSize()).isEqualTo(5);
  }

  @Test
  public void visitSortTest() {
    val parseTree = createParseTree("select(gender),sort(-ageAtDiagnosis, ageAtEnrollment)");
    val orderContext = (OrderContext) parseTree.getChild(2);
    val sortNode = (SortNode) VISITOR.visitOrder(orderContext);
    assertThat(sortNode.getFields().size()).isEqualTo(2);
    assertThat(sortNode.getFields().get("donor_age_at_diagnosis")).isEqualTo(Order.DESC);
    assertThat(sortNode.getFields().get("donor_age_at_enrollment")).isEqualTo(Order.ASC);
  }

  @Test
  public void visitInTest() {
    val parseTree = createParseTree("in(gender, 'male', 'female')");
    val inContext = (InArrayContext) parseTree.getChild(0);
    val termsNode = (TermsNode) VISITOR.visitInArray(inContext);
    assertThat(termsNode.getField()).isEqualTo("donor_sex");
    assertThat(termsNode.childrenCount()).isEqualTo(2);
    val maleNode = (TerminalNode) termsNode.getFirstChild();
    assertThat(maleNode.getValue()).isEqualTo("male");
    val femaleNode = (TerminalNode) termsNode.getChild(1);
    assertThat(femaleNode.getValue()).isEqualTo("female");
  }

  @Test
  public void visitNestedTest() {
    val parseTree = createParseTree("nested(gene, eq(id, 'D01'), gt(ageAtEnrollment, 50000))");
    val nestedContext = (NestedContext) parseTree.getChild(0);
    val nestedNode = (NestedNode) VISITOR.visitNested(nestedContext);
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("gene");
    val boolNode = (BoolNode) nestedNode.getFirstChild();
    assertThat(boolNode.childrenCount()).isEqualTo(1);
    val mustNode = (MustBoolNode) boolNode.getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    val termNode = (TermNode) mustNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_donor_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("D01");

    val rangeNode = (RangeNode) mustNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo("donor_age_at_enrollment");
    val gtNode = (GreaterThanNode) rangeNode.getFirstChild();
    assertThat(gtNode.getValue()).isEqualTo(50000);
  }

  @Test
  public void visitFacetsTest() {
    val parseTree = createParseTree("facets(gender, id)");
    val facetsContext = (FacetsContext) parseTree.getChild(0);
    val facetsNode = (AggregationsNode) VISITOR.visitFacets(facetsContext);
    log.debug("Facets node: {}", facetsNode);

    assertThat(facetsNode.childrenCount()).isEqualTo(2);
    TermsAggregationNode termsFacetNode = (TermsAggregationNode) facetsNode.getFirstChild();
    assertThat(termsFacetNode.getFieldName()).isEqualTo("donor_sex");

    termsFacetNode = (TermsAggregationNode) facetsNode.getChild(1);
    assertThat(termsFacetNode.getFieldName()).isEqualTo("_donor_id");
  }

  @Test
  public void visitNotTest() {
    val parseTree = createParseTree("not(in(id, 'DO1'))");
    val notContext = (NotContext) parseTree.getChild(0);
    val notNode = (NotNode) VISITOR.visitNot(notContext);
    log.debug("Not node: \n{}", notNode);

    assertThat(notNode.childrenCount()).isEqualTo(1);

    val termsNode = (TermsNode) notNode.getFirstChild();
    assertThat(termsNode.childrenCount()).isEqualTo(1);
    assertThat(termsNode.getField()).isEqualTo("_donor_id");

    val terminalNode = (TerminalNode) termsNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo("DO1");
  }

  @Test
  public void visitExistsTest() {
    val parseTree = createParseTree("exists(id)");
    val existsContext = (ExistsContext) parseTree.getChild(0);
    val existsNode = (ExistsNode) VISITOR.visitExists(existsContext);
    log.debug("Exists node: \n{}", existsNode);

    assertThat(existsNode.childrenCount()).isEqualTo(0);
    assertThat(existsNode.getField()).isEqualTo("_donor_id");
  }

  @Test
  public void visitMissingTest() {
    val parseTree = createParseTree("missing(id)");
    val missingContext = (MissingContext) parseTree.getChild(0);
    val missingNode = (MissingNode) VISITOR.visitMissing(missingContext);
    log.debug("Missing node: \n{}", missingNode);

    assertThat(missingNode.childrenCount()).isEqualTo(0);
    assertThat(missingNode.getField()).isEqualTo("_donor_id");
  }

}
