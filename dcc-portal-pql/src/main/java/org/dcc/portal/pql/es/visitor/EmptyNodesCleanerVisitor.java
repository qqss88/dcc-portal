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

import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.AndNode;
import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FacetsNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.FilterNode;
import org.dcc.portal.pql.es.ast.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.LessEqualNode;
import org.dcc.portal.pql.es.ast.LessThanNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.OrNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.TermsFacetNode;
import org.dcc.portal.pql.es.ast.TermsNode;

/**
 * Removes nodes that do not have children. E.g. MustBoolNode without children must be removed as it breaks an ES search
 * request.
 */
@Slf4j
public class EmptyNodesCleanerVisitor extends NodeVisitor<ExpressionNode> {

  public static final ExpressionNode REMOVE_NODE = null;

  @Override
  public ExpressionNode visitNested(NestedNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitTerm(TermNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitSort(SortNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitTerminal(TerminalNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitRange(RangeNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitGreaterEqual(GreaterEqualNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitGreaterThan(GreaterThanNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitLessEqual(LessEqualNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitLessThan(LessThanNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitLimit(LimitNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitAnd(AndNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitOr(OrNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitFacets(FacetsNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitFields(FieldsNode node) {
    return node;
  }

  @Override
  public ExpressionNode visitQuery(QueryNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitTermsFacet(TermsFacetNode node) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitRoot(RootNode node) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitBool(BoolNode node) {
    return defaultProcessing(node);
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node) {
    return defaultProcessing(node);
  }

  // TODO: find a better name
  private ExpressionNode defaultProcessing(ExpressionNode node) {
    log.debug("Processing {}", node);
    node = processChildren(node);
    if (node.childrenCount() == 0) {
      log.debug("Requesting to remove empty node {}", node);

      return REMOVE_NODE;
    }

    return node;
  }

  private ExpressionNode processChildren(ExpressionNode node) {
    for (int i = 0; i < node.childrenCount(); i++) {
      if (node.getChild(i).accept(this) == REMOVE_NODE) {
        node.removeChild(i);
      }
    }

    return node;
  }

}
