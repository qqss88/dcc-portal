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

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TermsNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * Visits filter nodes. And removes those that match provided aggregation field name.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AggregationFiltersVisitor extends NodeVisitor<ExpressionNode> {

  private static final ExpressionNode REMOVE_CHILD = null;

  private String termsFieldName;

  public ExpressionNode visit(String fieldName, FilterNode filterNode) {
    log.debug("Visiting Aggregation Filters. Terms Aggregation Field: '{}'. Filters: '{}'", fieldName, filterNode);
    this.termsFieldName = fieldName;
    val result = filterNode.accept(this, Optional.empty());

    return result;
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node, Optional<QueryContext> context) {
    visitChild(node);

    return node;
  }

  @Override
  public ExpressionNode visitBool(BoolNode node, Optional<QueryContext> context) {
    visitChild(node);

    return node;
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node, Optional<QueryContext> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitTerm(TermNode node, Optional<QueryContext> context) {
    return node.getNameNode().getValue().equals(termsFieldName) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node, Optional<QueryContext> context) {
    return visitChild(node);
  }

  @Override
  public ExpressionNode visitRange(RangeNode node, Optional<QueryContext> context) {
    return node.getFieldName().equals(termsFieldName) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node, Optional<QueryContext> context) {
    return node.getField().equals(termsFieldName) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitAnd(AndNode node, Optional<QueryContext> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitOr(OrNode node, Optional<QueryContext> context) {
    return processCommonCases(node);
  }

  private ExpressionNode visitChild(ExpressionNode parent) {
    return parent.getFirstChild().accept(this, Optional.empty());
  }

  private ExpressionNode processCommonCases(ExpressionNode node) {
    for (int i = 0; i < node.childrenCount(); i++) {
      val child = node.getChild(i);
      log.debug("Visiting child: {}", child);
      if (child.accept(this, Optional.empty()) == REMOVE_CHILD) {
        log.debug("Removing the child from the filters.");
        node.removeChild(i);
      }
    }

    return node;
  }

}
