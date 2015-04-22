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

import static org.dcc.portal.pql.es.utils.Nodes.cloneNode;
import static org.dcc.portal.pql.es.utils.Nodes.getOptionalChild;
import static org.dcc.portal.pql.es.utils.Visitors.createAggregationFiltersVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createResolveNestedFilterVisitor;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.TypeModel;

/**
 * Processes aggregations. Copies the FilterNode to facet's filter node except filters for facets being calculated.
 */
@Slf4j
public class AggregationsResolverVisitor extends NodeVisitor<Optional<ExpressionNode>, Context> {

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode rootNode, Optional<Context> context) {
    log.debug("Resolving aggregations. Source AST: {}", rootNode);

    if (!hasAggregations(rootNode)) {
      log.debug("The source AST does not contain a AggregationsNode. Returning the original source AST back.");

      return Optional.of(rootNode);
    }

    // The filters are enclosed in a FilterAggregationNode with is added to the AggregationsNode (parent
    // of the current TermsAggregationNode node). The current node then is added as a child to the
    // FilterAggregationNode.
    // Such a nodes order reflects how aggregations are built in ES

    // NB: If a Terms aggregation is created on a nested field and has at least 2 nested filters on different levels the
    // following processing structure should be created to correctly calculate Terms Aggregations:

    // 1) Create a FilterAggregationNode with filters resolved by processFilters() method. The filters contain all the
    // conditions selected on the UI except filters applied to the field this aggregation is being created for.

    // 2) Create a NestedAggregation on the path used to nest the field.

    // 3) Create a FilterAggregationNode with filters which are nested at the path used in the previous step, but remove
    // the NestedNode from the filters. This step is mandatory, because an ES query with filters from step 1. Will
    // return parent documents and they may contain nested children that do not satisfy the condition. That's why we
    // need to apply the nested filters one more time.

    // 4) Create Terms aggregation node on the field used for aggregation.

    // E.g. The described case is valid for the MUTATION-CENTRIC type which may have filters nested at 'transcript' and
    // 'ssm_occurrence' levels.

    val filterNodeOpt = getFilterNodeOptional(rootNode);
    val aggsNode = getAggregationsNodeOptional(rootNode).get();
    val typeModel = context.get().getTypeModel();

    for (int i = 0; i < aggsNode.childrenCount(); i++) {
      val child = (TermsAggregationNode) aggsNode.getChild(i);
      val field = child.getFieldName();

      Optional<ExpressionNode> nestedAggregation = Optional.empty();
      Optional<ExpressionNode> filtersAggregation = Optional.empty();

      if (typeModel.isNested(field)) {
        nestedAggregation =
            Optional.of(createNestedNode(filterNodeOpt, typeModel, (TermsAggregationNode) cloneNode(child)));
      }

      if (filterNodeOpt.isPresent()) {
        val filters = resolveFilters(child.getFieldName(), cloneNode(filterNodeOpt.get()), typeModel);
        if (filters.isPresent()) {
          filtersAggregation = Optional.of(createFilterAggregationNode(child.getAggregationName(), filters.get()));
        }
      }

      val resultOpt = createResultNode(filtersAggregation, nestedAggregation, cloneNode(child));
      if (resultOpt.isPresent()) {
        aggsNode.setChild(i, resultOpt.get());
      }
    }

    return Optional.of(rootNode);
  }

  private static Optional<ExpressionNode> createResultNode(Optional<ExpressionNode> filtersAggregation,
      Optional<ExpressionNode> nestedAggregation, ExpressionNode termsAggregation) {

    if (filtersAggregation.isPresent()) {
      val result = filtersAggregation.get();
      if (nestedAggregation.isPresent()) {
        result.addChildren(nestedAggregation.get());
      } else {
        result.addChildren(termsAggregation);
      }

      return filtersAggregation;
    }

    if (nestedAggregation.isPresent()) {
      return nestedAggregation;
    }

    return Optional.empty();
  }

  /**
   * Creates NestedAggregationNode with filters below the nested path if they are present.
   * 
   * @param filters - All filters except ones the Aggregation is being created for.
   */
  private static ExpressionNode createNestedNode(Optional<? extends ExpressionNode> filters, TypeModel typeModel,
      TermsAggregationNode node) {

    val field = node.getFieldName();
    val nestedPath = typeModel.getNestedPath(field);
    val result = new NestedAggregationNode(node.getAggregationName(), nestedPath);

    // NestedAggregation - TermsAggregation
    if (!filters.isPresent()) {
      result.addChildren(node);

      return result;
    }

    // NestedAggregation - TermsAggregation
    val nestedFiltersOpt = resolveNestedFilters(typeModel.getNestedPath(field), cloneNode(filters.get()), typeModel);
    if (!nestedFiltersOpt.isPresent()) {
      result.addChildren(node);

      return result;
    }

    // NestedAggregation - FilterAggragation(nested filters) - TermsAggregation
    val nestedFilters = nestedFiltersOpt.get();
    val nestedFilterAggregation = createFilterAggregationNode(node.getAggregationName(), nestedFilters);
    result.addChildren(nestedFilterAggregation);
    nestedFilterAggregation.addChildren(node);

    return result;
  }

  private static FilterAggregationNode createFilterAggregationNode(String aggregationName, ExpressionNode filters) {
    val result = new FilterAggregationNode(aggregationName, filters);
    filters.setParent(result);

    return result;
  }

  private static Optional<ExpressionNode> resolveNestedFilters(String nestedPath, ExpressionNode cloneNode,
      TypeModel typeModel) {
    val context = Optional.of(new VisitContext(nestedPath, typeModel));

    return cloneNode.accept(createResolveNestedFilterVisitor(), context);
  }

  private static Optional<ExpressionNode> resolveFilters(String facetField, ExpressionNode filterNode,
      TypeModel typeModel) {
    val resolvedFilters = filterNode.accept(createAggregationFiltersVisitor(), Optional.of(facetField));
    if (!hasNonNestedFilters(facetField, typeModel, resolvedFilters)) {
      return Optional.empty();
    }

    val result = resolvedFilters.accept(Visitors.createEmptyNodesCleanerVisitor(), Optional.empty());

    return result == null ? Optional.empty() : Optional.of(result);
  }

  private static Boolean hasNonNestedFilters(String facetField, TypeModel typeModel,
      ExpressionNode resolvedFilters) {
    if (!typeModel.isNested(facetField)) {
      return true;
    }

    val context = new VisitContext(typeModel.getNestedPath(facetField), typeModel);

    return resolvedFilters.accept(Visitors.createSearchNonNestedFieldsVisitor(), Optional.of(context));
  }

  private static Optional<AggregationsNode> getAggregationsNodeOptional(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class);
  }

  private static boolean hasAggregations(ExpressionNode rootNode) {
    return getOptionalChild(rootNode, AggregationsNode.class).isPresent();
  }

  private static Optional<FilterNode> getFilterNodeOptional(ExpressionNode rootNode) {
    val queryNode = getOptionalChild(rootNode, QueryNode.class);
    if (queryNode.isPresent()) {
      return getOptionalChild(queryNode.get(), FilterNode.class);
    }

    return Optional.empty();
  }

}
