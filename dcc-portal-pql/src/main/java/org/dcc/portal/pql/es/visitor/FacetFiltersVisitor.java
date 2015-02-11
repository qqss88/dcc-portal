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

import static com.google.common.base.Preconditions.checkState;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.AndNode;
import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FilterNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.OrNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TermsFacetNode;
import org.dcc.portal.pql.es.ast.TermsNode;

/**
 * Visits filter nodes. And removes those that match provided facet field.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class FacetFiltersVisitor extends NodeVisitor<ExpressionNode> {

  private static final ExpressionNode REMOVE_CHILD = null;

  private String facetField;

  public ExpressionNode visit(String facetField, FilterNode filterNode) {
    log.debug("Visiting Facet Filters. TermsFacetField: '{}'. Filters: '{}'", facetField, filterNode);
    this.facetField = facetField;
    val result = filterNode.accept(this);

    return result;
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node) {
    visitChild(node);

    return node;
  }

  @Override
  public ExpressionNode visitBool(BoolNode node) {
    visitChild(node);

    return node;
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node) {
    return processCommon(node);
  }

  @Override
  public ExpressionNode visitTerm(TermNode node) {
    return node.getNameNode().getValue().equals(facetField) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node) {
    return visitChild(node);
  }

  @Override
  public ExpressionNode visitRange(RangeNode node) {
    return node.getName().equals(facetField) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node) {
    return node.getField().equals(facetField) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitAnd(AndNode node) {
    return processCommon(node);
  }

  @Override
  public ExpressionNode visitOr(OrNode node) {
    return processCommon(node);
  }

  private ExpressionNode visitChild(ExpressionNode parent) {
    return parent.getFirstChild().accept(this);
  }

  // FIXME: give some more meaningful name
  private ExpressionNode processCommon(ExpressionNode node) {
    for (int i = 0; i < node.childrenCount(); i++) {
      val child = node.getChild(i);
      log.debug("Visiting child: {}", child);
      if (child.accept(this) == REMOVE_CHILD) {
        log.debug("Removing the child from the filters and setting scope of the facet to global.");
        node.removeChild(i);
        setGlobal(node);
      }
    }

    return node;
  }

  private void setGlobal(ExpressionNode node) {
    getTermsFacetNode(node).setGlobal();
  }

  private TermsFacetNode getTermsFacetNode(ExpressionNode node) {
    TermsFacetNode result = null;
    ExpressionNode currentNode = node;
    while (currentNode != null) {
      if (currentNode instanceof TermsFacetNode) {
        result = (TermsFacetNode) currentNode;
        break;
      }
      currentNode = currentNode.getParent();
    }

    checkState(result != null, "Couldn't find TermsFacetNode parent in node %s", node);

    return result;
  }

}
