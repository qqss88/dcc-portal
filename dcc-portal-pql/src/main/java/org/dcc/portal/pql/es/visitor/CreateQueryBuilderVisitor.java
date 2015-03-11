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

import java.util.Optional;

import lombok.val;

import org.dcc.portal.pql.es.ast.FunctionScoreQueryNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.qe.QueryContext;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

/**
 * Creates QueryBuilder by visiting {@link QueryNode}
 */
public class CreateQueryBuilderVisitor extends NodeVisitor<QueryBuilder, QueryContext> {

  @Override
  public QueryBuilder visitQuery(QueryNode node, Optional<QueryContext> context) {
    return node.getFirstChild().accept(this, context);
  }

  @Override
  public QueryBuilder visitNested(NestedNode node, Optional<QueryContext> context) {
    val functionScoreQuery = node.getFirstChild().accept(this, context);

    return QueryBuilders
        .nestedQuery(node.getPath(), functionScoreQuery)
        .scoreMode(node.getScoreMode().getId());
  }

  @Override
  public QueryBuilder visitFunctionScoreQuery(FunctionScoreQueryNode node, Optional<QueryContext> context) {
    val scoreFunctionBuilder = ScoreFunctionBuilders.scriptFunction(node.getScript());

    val filterNode = Nodes.getOptionalChild(node, FilterNode.class);
    if (filterNode.isPresent()) {
      val filteredQuery = filterNode.get().accept(this, context);

      return QueryBuilders.functionScoreQuery(filteredQuery, scoreFunctionBuilder);
    }

    return QueryBuilders.functionScoreQuery(scoreFunctionBuilder);
  }

  @Override
  public QueryBuilder visitFilter(FilterNode node, Optional<QueryContext> context) {
    val filterBuilder = node.accept(Visitors.filterBuilderVisitor(), context);

    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder);
  }

}
