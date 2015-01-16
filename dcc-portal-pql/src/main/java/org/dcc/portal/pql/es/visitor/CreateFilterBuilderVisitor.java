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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.dcc.portal.pql.es.ast.BoolNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldNameNode;
import org.dcc.portal.pql.es.ast.FromNode;
import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.MustNotBoolNode;
import org.dcc.portal.pql.es.ast.Node;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.PostFilterNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RangeNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.ast.ToNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.qe.QueryContext;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import com.google.common.collect.Lists;

@RequiredArgsConstructor
public class CreateFilterBuilderVisitor implements NodeVisitor<FilterBuilder> {

  private final QueryContext queryContext;
  private final Client client;

  @Override
  public FilterBuilder visitBool(BoolNode node) {
    BoolFilterBuilder resultBuilder = FilterBuilders.boolFilter();
    val mustNode = getChild(node, MustBoolNode.class);
    if (mustNode != null) {
      resultBuilder = resultBuilder.must(visitChildren(mustNode));
    }

    val shouldNode = getChild(node, ShouldBoolNode.class);
    if (shouldNode != null) {
      resultBuilder = resultBuilder.should(visitChildren(shouldNode));
    }

    val mustNotNode = getChild(node, MustNotBoolNode.class);
    if (mustNotNode != null) {
      resultBuilder = resultBuilder.mustNot(visitChildren(mustNotNode));
    }

    return resultBuilder;
  }

  private FilterBuilder[] visitChildren(ExpressionNode node) {
    val result = Lists.<FilterBuilder> newArrayList();
    for (val child : node.getChildren()) {
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
  public FilterBuilder visitQuery(QueryNode node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterBuilder visitPostFilter(PostFilterNode node) {
    return visitBool((BoolNode) node.getChild(0));
  }

  @Override
  public FilterBuilder visitShouldBool(ShouldBoolNode node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterBuilder visitRootFilter(RootNode node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchRequestBuilder visit(Node node) {
    // FIXME: get index from QueryContext
    val result = client.prepareSearch("");
    for (val child : node.getChildren()) {
      if (child instanceof PostFilterNode) {
        val boolFilter = child.accept(this);
        result.setFilter(boolFilter);
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
  public FilterBuilder visitMustNotBool(MustNotBoolNode node) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.dcc.portal.pql.es.visitor.NodeVisitor#visitRange(org.dcc.portal.pql.es.ast.RangeNode)
   */
  @Override
  public FilterBuilder visitRange(RangeNode node) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.dcc.portal.pql.es.visitor.NodeVisitor#visitFrom(org.dcc.portal.pql.es.ast.FromNode)
   */
  @Override
  public FilterBuilder visitFrom(FromNode node) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.dcc.portal.pql.es.visitor.NodeVisitor#visitTo(org.dcc.portal.pql.es.ast.ToNode)
   */
  @Override
  public FilterBuilder visitTo(ToNode node) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.dcc.portal.pql.es.visitor.NodeVisitor#visitFieldName(org.dcc.portal.pql.es.ast.FieldNameNode)
   */
  @Override
  public FilterBuilder visitFieldName(FieldNameNode node) {
    // TODO Auto-generated method stub
    return null;
  }

}
