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

import static org.dcc.portal.pql.es.utils.Nodes.getOptionalChild;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

/**
 * Processes aggregations. Copies the FilterNode to facet's filter node except filters for facets being calculated.
 */
@Slf4j
public class AggregationsResolverVisitor extends NodeVisitor<ExpressionNode, FilterNode> {

  private final AggregationFiltersVisitor aggregationFilterVisitor = Visitors.createAggregationFiltersVisitor();

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode rootNode, Optional<FilterNode> context) {
    log.debug("Resolving aggregations. Source AST: {}", rootNode);

    if (!hasAggregations(rootNode)) {
      log.debug("The source AST does not contain a AggregationsNode. Returning the original source AST back.");
      return rootNode;
    }

    val filterNodeOpt = getFilterNodeOptional(rootNode);
    if (!filterNodeOpt.isPresent()) {
      log.debug("The source AST does not contain any FilterNodes. Returning the original source AST back.");
      return rootNode;
    }

    val aggsNode = getAggregationsNodeOptional(rootNode).get();
    for (int i = 0; i < aggsNode.childrenCount(); i++) {
      val childAggsNode = aggsNode.getChild(i).accept(this, filterNodeOpt);
      if (childAggsNode instanceof FilterAggregationNode) {
        aggsNode.setChild(i, childAggsNode);
      }
    }

    return rootNode;
  }

  @Override
  public ExpressionNode visitTermsAggregation(TermsAggregationNode node, Optional<FilterNode> filtersNodeOpt) {
    log.debug("Visiting TermsAggregationsNode. {}", node);

    // The filters are enclosed in a FilterAggregationNode with is added to the AggregationsNode (parent
    // of the current TermsAggregationNode node). The current node then is added as a child to the
    // FilterAggregationNode.
    // Such a nodes order reflects how aggregations are built in ES

    val newFilterNode = processFilters(node.getFieldName(), Nodes.cloneNode(filtersNodeOpt.get()));
    val filterAggregationNode = new FilterAggregationNode(node.getAggregationName(), newFilterNode);
    newFilterNode.setParent(filterAggregationNode);
    filterAggregationNode.addChildren(node);

    return filterAggregationNode;

  }

  private ExpressionNode processFilters(String facetField, ExpressionNode filterNode) {
    return filterNode.accept(aggregationFilterVisitor, Optional.of(facetField));
  }

  private Optional<AggregationsNode> getAggregationsNodeOptional(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class);
  }

  private boolean hasAggregations(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class).isPresent() ? true : false;
  }

  private static Optional<FilterNode> getFilterNodeOptional(ExpressionNode rootNode) {
    val queryNode = getOptionalChild(rootNode, QueryNode.class);
    if (queryNode.isPresent()) {
      return getOptionalChild(queryNode.get(), FilterNode.class);
    }

    return Optional.empty();
  }

}
