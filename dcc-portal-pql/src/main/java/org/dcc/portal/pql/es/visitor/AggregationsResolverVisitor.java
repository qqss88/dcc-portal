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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.Nodes.getOptionalChild;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.icgc.dcc.portal.model.IndexModel.Type;

import com.google.common.base.Optional;

/**
 * Processes aggregations. Moves FilterNode to a QueryNode. Copies the FilterNode to facet's filter node except filters
 * for facets being calculated.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AggregationsResolverVisitor extends NodeVisitor<ExpressionNode> {

  private Type type;
  private final AggregationFiltersVisitor aggregationFilterVisitor = new AggregationFiltersVisitor();

  public ExpressionNode resolveAggregations(@NonNull ExpressionNode sourceAst, @NonNull Type type) {
    checkArgument(sourceAst instanceof RootNode, "Source AST must be an instance of RootNode");
    this.type = type;
    val result = sourceAst.accept(this);
    log.debug("Resolved Aggregations to: \n{}", result);

    return result;
  }

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode rootNode) {
    log.debug("Resolving aggregations for type {}. Source AST: {}", type.getId(), rootNode);

    if (!hasAggregations(rootNode)) {
      log.debug("The source AST does not contain a AggregationsNode. Returning the original source AST back.");
      return rootNode;
    }

    val filterNodeOpt = getFilterNodeOptional(rootNode);
    if (!filterNodeOpt.isPresent()) {
      log.debug("The source AST does not contain any FilterNodes. Returning the original source AST back.");
      return rootNode;
    }

    // FilterNode is altered during the aggregations processing. We need to clone the structure to have the original
    // filter.
    val filterNodeClone = Nodes.cloneNode(filterNodeOpt.get());
    val aggsNode = getAggregationsNodeOptional(rootNode).get();

    for (int i = 0; i < aggsNode.childrenCount(); i++) {
      val childAggsNode = aggsNode.getChild(i).accept(this);
      if (childAggsNode instanceof FilterAggregationNode) {
        aggsNode.setChild(i, childAggsNode);
      }
    }

    moveFiltersToQuery(rootNode, filterNodeClone);

    return rootNode;
  }

  @Override
  public ExpressionNode visitTermsAggregation(TermsAggregationNode node) {
    log.debug("Visiting TermsAggregationsNode. {}", node);
    val rootNode = node.getParent().getParent();
    val filtersNodeOpt = getOptionalChild(rootNode, FilterNode.class);
    if (filtersNodeOpt.isPresent()) {

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

    return node;
  }

  private ExpressionNode processFilters(String facetField, ExpressionNode filterNode) {
    return aggregationFilterVisitor.visit(facetField, (FilterNode) filterNode);
  }

  private Optional<AggregationsNode> getAggregationsNodeOptional(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class);
  }

  private boolean hasAggregations(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class).isPresent() ? true : false;
  }

  private static void moveFiltersToQuery(ExpressionNode rootNode, ExpressionNode filters) {
    val filterNodeIndex = getFilterNodeIndex(rootNode);
    rootNode.removeChild(filterNodeIndex);
    rootNode.addChildren(new QueryNode(filters));
  }

  private static int getFilterNodeIndex(ExpressionNode rootNode) {
    for (int i = 0; i < rootNode.childrenCount(); i++) {
      if (rootNode.getChild(i) instanceof FilterNode) {
        return i;
      }
    }

    throw new IllegalStateException(format("Could not find FilterNode in RootNode: %s", rootNode));
  }

  private static Optional<FilterNode> getFilterNodeOptional(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, FilterNode.class);
  }

}
