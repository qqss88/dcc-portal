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
package org.dcc.portal.pql.qe;

import static org.dcc.portal.pql.es.utils.Nodes.getOptionalChild;

import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.CountNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Visitors;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.sort.SortOrder;

@Slf4j
@RequiredArgsConstructor
public class EsRequestBuilder {

  private static final String[] EMPTY_EXCLUDE_FIELDS = null;

  @NonNull
  private final Client client;

  public SearchRequestBuilder buildSearchRequest(@NonNull ExpressionNode esAst, @NonNull QueryContext queryContext) {
    SearchRequestBuilder result = client
        .prepareSearch(queryContext.getIndex())
        .setTypes(queryContext.getType().getId());

    if (getOptionalChild(esAst, CountNode.class).isPresent()) {
      log.debug("Setting search type to count");
      result = result.setSearchType(SearchType.COUNT);
    }

    for (val child : esAst.getChildren()) {
      if (child instanceof FilterNode) {
        result.setPostFilter(child.accept(Visitors.filterBuilderVisitor(), Optional.of(queryContext)));
      } else if (child instanceof QueryNode) {
        val queryBuilder = child.accept(Visitors.createQueryBuilderVisitor(), Optional.of(queryContext));
        result.setQuery(queryBuilder);
      } else if (child instanceof AggregationsNode) {
        addAggregations(child, result, queryContext);
      } else if (child instanceof FieldsNode) {
        val fieldsNode = (FieldsNode) child;
        String[] children = fieldsNode.getFields().toArray(new String[fieldsNode.getFields().size()]);
        result.addFields(children);
      } else if (child instanceof SourceNode) {
        val sourceNode = (SourceNode) child;
        String[] children = sourceNode.getFields().toArray(new String[sourceNode.getFields().size()]);
        result.setFetchSource(children, EMPTY_EXCLUDE_FIELDS);
      } else if (child instanceof LimitNode) {
        val limitNode = (LimitNode) child;
        result.setFrom(limitNode.getFrom());
        result.setSize(limitNode.getSize());
      } else if (child instanceof SortNode) {
        val sortNode = (SortNode) child;
        for (val entry : sortNode.getFields().entrySet()) {
          result.addSort(entry.getKey(), SortOrder.valueOf(entry.getValue().toString()));
        }
      }
    }

    return result;
  }

  private void addAggregations(ExpressionNode aggregationsNode, SearchRequestBuilder result, QueryContext queryContext) {
    log.debug("Adding aggregations for AggregationsNode\n{}", aggregationsNode);
    for (val child : aggregationsNode.getChildren()) {
      val aggregationBuilder = child.accept(Visitors.createAggregationBuilderVisitor(), Optional.of(queryContext));
      result.addAggregation(aggregationBuilder);
    }
  }

}
