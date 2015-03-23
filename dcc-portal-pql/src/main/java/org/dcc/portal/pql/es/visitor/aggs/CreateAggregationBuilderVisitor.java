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

import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.AbstractTypeModel;
import org.dcc.portal.pql.qe.QueryContext;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

@Slf4j
public class CreateAggregationBuilderVisitor extends NodeVisitor<AbstractAggregationBuilder, QueryContext> {

  private static int DEFAULT_FACETS_SIZE = 1000;

  @Override
  public AbstractAggregationBuilder visitTermsAggregation(TermsAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);
    val fieldName = node.getFieldName();
    AbstractAggregationBuilder result = AggregationBuilders
        .terms(node.getAggregationName())
        .size(DEFAULT_FACETS_SIZE)
        .field(fieldName);

    return nest(result, node, context.get());
  }

  @Override
  public AbstractAggregationBuilder visitMissingAggregation(MissingAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);
    val fieldName = node.getFieldName();
    AbstractAggregationBuilder result = AggregationBuilders
        .missing(node.getAggregationName())
        .field(fieldName);

    return nest(result, node, context.get());
  }

  @Override
  public AbstractAggregationBuilder visitFilterAggregation(FilterAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);
    log.debug("Visiting FilterAggregationNode: \n{}", node);
    log.debug("Filters: {}", node.getFilters());

    val filterAggregationBuilder = AggregationBuilders.filter(node.getAggregationName())
        .filter(resolveFilters(node, context))
        .subAggregation(node.getFirstChild().accept(this, context));

    return AggregationBuilders
        .global(node.getAggregationName())
        .subAggregation(filterAggregationBuilder);
  }

  private static AbstractAggregationBuilder nest(AbstractAggregationBuilder builder, ExpressionNode node,
      QueryContext context) {
    val aggName = getAggName(node);
    val fieldName = getFieldName(node);

    val typeModel = context.getTypeModel();
    if (typeModel.isNested(fieldName)) {
      builder = createNestedAggregation(builder, aggName, fieldName, typeModel);
    }

    // If this node is not a child of a FilterAggregationNode set scope of the aggregation to global
    if (node.getParent() instanceof AggregationsNode) {
      return AggregationBuilders
          .global(aggName)
          .subAggregation(builder);
    }

    return builder;
  }

  private static AbstractAggregationBuilder createNestedAggregation(AbstractAggregationBuilder termsAggregation,
      String aggName, String fieldName, AbstractTypeModel typeModel) {
    return AggregationBuilders.nested(aggName)
        .path(typeModel.getNestedPath(fieldName))
        .subAggregation(termsAggregation);
  }

  private static FilterBuilder resolveFilters(FilterAggregationNode parent, Optional<QueryContext> context) {
    return parent.getFilters().accept(Visitors.filterBuilderVisitor(), context);
  }

  private static String getFieldName(ExpressionNode node) {
    if (node instanceof MissingAggregationNode) {
      return ((MissingAggregationNode) node).getFieldName();
    } else {
      return ((TermsAggregationNode) node).getFieldName();
    }
  }

  private static String getAggName(ExpressionNode node) {
    if (node instanceof MissingAggregationNode) {
      return ((MissingAggregationNode) node).getAggregationName();
    } else {
      return ((TermsAggregationNode) node).getAggregationName();
    }
  }

}
