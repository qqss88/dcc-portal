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
package org.dcc.portal.pql.utils;

import lombok.val;

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
import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Lists;

public class CloneNodeVisitor extends NodeVisitor<ExpressionNode> {

  @Override
  public ExpressionNode visitFilter(FilterNode node) {
    return new FilterNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitNested(NestedNode node) {
    return new NestedNode(node.getPath(), visitChildren(node));
  }

  @Override
  public ExpressionNode visitBool(BoolNode node) {
    return new BoolNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node) {
    return new MustBoolNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitTerm(TermNode node) {
    val nameNode = new TerminalNode(node.getNameNode().getValue());
    val valueNode = new TerminalNode(node.getValueNode().getValue());

    return new TermNode(nameNode, valueNode);
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node) {
    val result = new TermsNode(node.getField());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitNot(NotNode node) {
    return new NotNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitRoot(RootNode node) {
    return new RootNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitSort(SortNode node) {
    val result = new SortNode();
    for (val entry : node.getFields().entrySet()) {
      result.addField(entry.getKey(), entry.getValue());
    }

    return result;
  }

  @Override
  public ExpressionNode visitTerminal(TerminalNode node) {
    return new TerminalNode(node.getValue());
  }

  @Override
  public ExpressionNode visitRange(RangeNode node) {
    return new RangeNode(node.getFieldName(), visitChildren(node));
  }

  @Override
  public ExpressionNode visitGreaterEqual(GreaterEqualNode node) {
    return new GreaterEqualNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitGreaterThan(GreaterThanNode node) {
    return new GreaterThanNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLessEqual(LessEqualNode node) {
    return new LessEqualNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLessThan(LessThanNode node) {
    return new LessThanNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLimit(LimitNode node) {
    return new LimitNode(node.getFrom(), node.getSize());
  }

  @Override
  public ExpressionNode visitAnd(AndNode node) {
    return new AndNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitOr(OrNode node) {
    return new OrNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitFacets(FacetsNode node) {
    return new FacetsNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitFields(FieldsNode node) {
    return new FieldsNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitQuery(QueryNode node) {
    return new QueryNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitTermsFacet(TermsFacetNode node) {
    val result = new TermsFacetNode(node.getFacetName(), node.getField(), node.isGlobal());
    result.addChildren(visitChildren(node));

    return result;
  }

  private ExpressionNode[] visitChildren(ExpressionNode parent) {
    val result = Lists.<ExpressionNode> newArrayList();
    for (val child : parent.getChildren()) {
      result.add(child.accept(this));
    }

    return result.toArray(new ExpressionNode[result.size()]);
  }

}
