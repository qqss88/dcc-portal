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

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * This visitor removes a {@link FilterAggregationNode} that has an empty filter and moves it's child (
 * {@link TermsAggregationNode}) one level up.
 */
@Slf4j
public class RemoveAggregationFilterVisitor extends NodeVisitor<ExpressionNode, QueryContext> {

  private static final ExpressionNode SKIP_NODE = null;

  @Override
  public ExpressionNode visitRoot(RootNode node, Optional<QueryContext> context) {
    val aggsNode = Nodes.getOptionalChild(node, AggregationsNode.class);
    if (aggsNode.isPresent()) {
      aggsNode.get().accept(this, Optional.empty());
    }

    return node;
  }

  @Override
  public ExpressionNode visitAggregations(AggregationsNode node, Optional<QueryContext> context) {
    for (int i = 0; i < node.childrenCount(); i++) {
      val child = node.getChild(i);
      val visitResult = child.accept(this, Optional.empty());
      if (visitResult != SKIP_NODE) {
        node.setChild(i, visitResult);
      }
    }

    return node;
  }

  @Override
  public ExpressionNode visitFilterAggregation(FilterAggregationNode node, Optional<QueryContext> context) {
    if (node.getFilters().childrenCount() == 0) {
      log.debug("FilterAggregationNode has no filters. Requesting to remove.");
      return node.getFirstChild();
    }

    return SKIP_NODE;
  }

  @Override
  public ExpressionNode visitTermsAggregation(TermsAggregationNode node, Optional<QueryContext> context) {
    return SKIP_NODE;
  }

}
