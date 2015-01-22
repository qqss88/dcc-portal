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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Stack;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.AndNode;
import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.GreaterThanNode;
import org.dcc.portal.pql.es.ast.LessEqualNode;
import org.dcc.portal.pql.es.ast.LessThanNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.Node;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.OrNode;
import org.dcc.portal.pql.es.ast.PostFilterNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.TermsNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.RequestType;
import org.dcc.portal.pql.qe.QueryContext;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.collect.Lists;

@Slf4j
@RequiredArgsConstructor
public class CreateFilterBuilderVisitor implements NodeVisitor<FilterBuilder> {

  private final Client client;

  private final Stack<FilterBuilder> stack = new Stack<FilterBuilder>();

  @Override
  public FilterBuilder visitBool(BoolNode node) {
    BoolFilterBuilder resultBuilder = FilterBuilders.boolFilter();
    val mustNode = getChild(node, MustBoolNode.class);
    if (mustNode != null) {
      resultBuilder = resultBuilder.must(visitChildren(mustNode));
    }

    return resultBuilder;
  }

  private FilterBuilder[] visitChildren(ExpressionNode node) {
    log.debug("Visiting Bool child: {}", node);
    val result = Lists.<FilterBuilder> newArrayList();
    for (val child : node.getChildren()) {
      log.debug("Sub-child: {}", child);
      result.add(child.accept(this));
    }

    return result.toArray(new FilterBuilder[result.size()]);
  }

  @Override
  public FilterBuilder visitTerm(TermNode node) {
    val name = node.getNameNode().getValue().toString();
    val value = node.getValueNode().getValue();

    return FilterBuilders.termFilter(name, value);
  }

  @Override
  public FilterBuilder visitMustBool(MustBoolNode node) {
    throw new UnsupportedOperationException();
  }

  private static <T> T getChild(BoolNode boolNode, Class<T> type) {
    val children = Nodes.filterChildren(boolNode, type);
    checkState(children.size() < 2, "A BoolExpressionNode can contain only a single node of type %s",
        type.getSimpleName());

    if (children.isEmpty()) {
      return null;
    } else {
      return children.get(0);
    }
  }

  @Override
  public FilterBuilder visitPostFilter(PostFilterNode node) {
    return visitBool((BoolNode) node.getChild(0));
  }

  @Override
  public SearchRequestBuilder visit(Node node, QueryContext queryContext) {
    // FIXME: get index from QueryContext
    SearchRequestBuilder result = client.prepareSearch("dcc-release-etl-cli").setTypes("donor-centric");
    if (queryContext.getRequestType() == RequestType.COUNT) {
      log.debug("Setting search type to count");
      result = result.setSearchType(SearchType.COUNT);
    }

    for (val child : node.getChildren()) {
      if (child instanceof PostFilterNode) {
        val boolFilter = child.accept(this);
        result.setFilter(boolFilter);
      } else if (child instanceof FieldsNode) {
        val castedChild = (FieldsNode) child;
        String[] children = castedChild.getFields().toArray(new String[castedChild.getFields().size()]);
        result.addFields(children);
      } else if (child instanceof LimitNode) {
        val castedChild = (LimitNode) child;
        result.setFrom(castedChild.getFrom());
        result.setSize(castedChild.getSize());
      } else if (child instanceof SortNode) {
        val castedChild = (SortNode) child;
        for (val entry : castedChild.getFields().entrySet()) {
          result.addSort(entry.getKey(), SortOrder.valueOf(entry.getValue().toString()));
        }
      }
    }

    return result;
  }

  @Override
  public FilterBuilder visitNot(@NonNull NotNode node) {
    val childrenCount = node.getChildren().size();
    checkState(childrenCount == 1, "NotNode can have only one child. Found {}", childrenCount);

    return FilterBuilders.notFilter(node.getChild(0).accept(this));
  }

  @Override
  public FilterBuilder visitRange(@NonNull RangeNode node) {
    checkState(node.childrenCount() > 0, "RangeNode has no children");

    stack.push(FilterBuilders.rangeFilter(node.getName()));
    for (val child : node.getChildren()) {
      child.accept(this);
    }

    return stack.pop();
  }

  @Override
  public FilterBuilder visitGreaterEqual(GreaterEqualNode node) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gte(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitGreaterThan(GreaterThanNode node) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gt(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitLessEqual(LessEqualNode node) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lte(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitLessThan(LessThanNode node) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lt(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitAnd(AndNode node) {
    log.debug("Visiting And: {}", node);
    val childrenFilters = Lists.<FilterBuilder> newArrayList();
    for (val child : node.getChildren()) {
      childrenFilters.add(child.accept(this));
    }

    return FilterBuilders.andFilter(childrenFilters.toArray(new FilterBuilder[childrenFilters.size()]));
  }

  @Override
  public FilterBuilder visitOr(OrNode node) {
    log.debug("Visiting Or: {}", node);
    val childrenFilters = Lists.<FilterBuilder> newArrayList();
    for (val child : node.getChildren()) {
      childrenFilters.add(child.accept(this));
    }

    return FilterBuilders.orFilter(childrenFilters.toArray(new FilterBuilder[childrenFilters.size()]));
  }

  @Override
  public FilterBuilder visitTerms(TermsNode node) {
    val values = Lists.newArrayList();
    for (val child : node.getChildren()) {
      values.add(((TerminalNode) child).getValue());
    }

    return FilterBuilders.termsFilter(node.getField(), values);
  }

}
