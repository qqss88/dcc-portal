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
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.node.BoolExpressionNode;
import org.dcc.portal.pql.es.node.MustExpressionNode;
import org.dcc.portal.pql.es.node.TermExpressionNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import com.google.common.collect.Iterables;

@Slf4j
public class ExpressionNodeVisitorImpl implements ExpressionNodeVisitor<FilterBuilder> {

  /*
   * (non-Javadoc)
   * 
   * @see org.dcc.portal.pql.es.visitor.ExpressionNodeVisitor#visitBool(org.dcc.portal.pql.es.node.BoolExpressionNode)
   */
  @Override
  public FilterBuilder visitBool(BoolExpressionNode node) {
    BoolFilterBuilder resultBuilder = FilterBuilders.boolFilter();
    val mustNode = getMustNode(node);
    for (val child : mustNode.getChildren()) {
      val termNode = child.accept(this);
      resultBuilder = resultBuilder.must(termNode);
    }

    return resultBuilder;
  }

  @Override
  public FilterBuilder visitTerm(TermExpressionNode node) {
    val name = node.getName().getPayload().toString();
    val value = node.getValue().getPayload();
    log.info("Visiting term. Name: '{}', Value: '{}'", name, value);

    // FIXME: Make terminal node generic so a correct type could be applied
    return FilterBuilders.termFilter(name, value);
  }

  @Override
  public FilterBuilder visitMust(MustExpressionNode node) {
    return null;
  }

  private static MustExpressionNode getMustNode(BoolExpressionNode boolExpression) {
    val mustNodes = Lists.newArrayList(Iterables.filter(boolExpression.getChildren(), MustExpressionNode.class));
    checkState(mustNodes.size() == 1, "A BoolExpressionNode can contain only a single node of type MustExpressionNode");

    return mustNodes.get(0);
  }

}
