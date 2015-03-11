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

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.score.DefaultScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.DonorScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.GeneScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.ScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.special.GeneSetFilterVisitor;
import org.icgc.dcc.portal.model.IndexModel.Type;

@NoArgsConstructor(access = PRIVATE)
public class Visitors {

  private static final NodeVisitor<String, Void> TO_STRING_VISITOR = new ToStringVisitor();
  private static final NodeVisitor<ExpressionNode, Void> CLONE_VISITOR = new CloneNodeVisitor();

  private static final CreateAggregationBuilderVisitor AGGREGATION_BUILDER_VISITOR =
      new CreateAggregationBuilderVisitor();

  private static final FilterBuilderVisitor FILTER_BUILDER_VISITOR = new FilterBuilderVisitor();

  private static final RemoveAggregationFilterVisitor REMOVE_AGGS_FILTER_VISITOR = new RemoveAggregationFilterVisitor();

  /*
   * ScoreQueryVisitors
   */
  private static final DefaultScoreQueryVisitor DEFAULT_SCORE_QUERY_VISITOR = new DefaultScoreQueryVisitor();
  private static final DonorScoreQueryVisitor DONOR_SCORE_QUERY_VISITOR = new DonorScoreQueryVisitor();
  private static final GeneScoreQueryVisitor GENE_SCORE_QUERY_VISITOR = new GeneScoreQueryVisitor();

  /*
   * QueryBuilderVisitor
   */
  private static final CreateQueryBuilderVisitor QUERY_BUILDER_VISITOR = new CreateQueryBuilderVisitor();

  private static final AggregationFiltersVisitor AGGREGATION_FILTER_VISITOR = new AggregationFiltersVisitor();
  private static final AggregationsResolverVisitor AGGREGATIONS_RESOLVER_VISITOR = new AggregationsResolverVisitor();

  private static final EmptyNodesCleanerVisitor EMPTY_NODES_CLEANER_VISITOR = new EmptyNodesCleanerVisitor();

  private static final GeneSetFilterVisitor GENE_SET_FILTER_VISITOR = new GeneSetFilterVisitor();

  public static GeneSetFilterVisitor createGeneSetFilterVisitor() {
    return GENE_SET_FILTER_VISITOR;
  }

  public static EmptyNodesCleanerVisitor createEmptyNodesCleanerVisitor() {
    return EMPTY_NODES_CLEANER_VISITOR;
  }

  public static AggregationsResolverVisitor createAggregationsResolverVisitor() {
    return AGGREGATIONS_RESOLVER_VISITOR;
  }

  public static AggregationFiltersVisitor createAggregationFiltersVisitor() {
    return AGGREGATION_FILTER_VISITOR;
  }

  public static CreateQueryBuilderVisitor createQueryBuilderVisitor() {
    return QUERY_BUILDER_VISITOR;
  }

  public static NodeVisitor<String, Void> createToStringVisitor() {
    return TO_STRING_VISITOR;
  }

  public static NodeVisitor<ExpressionNode, Void> createCloneNodeVisitor() {
    return CLONE_VISITOR;
  }

  public static CreateAggregationBuilderVisitor createAggregationBuilderVisitor() {
    return AGGREGATION_BUILDER_VISITOR;
  }

  public static FilterBuilderVisitor filterBuilderVisitor() {
    return FILTER_BUILDER_VISITOR;
  }

  public static RemoveAggregationFilterVisitor createRemoveAggregationFilterVisitor() {
    return REMOVE_AGGS_FILTER_VISITOR;
  }

  public static ScoreQueryVisitor createScoreQueryVisitor(Type type) {
    switch (type) {
    case DONOR_CENTRIC:
      return DONOR_SCORE_QUERY_VISITOR;
    case GENE_CENTRIC:
      return GENE_SCORE_QUERY_VISITOR;
    default:
      return DEFAULT_SCORE_QUERY_VISITOR;
    }
  }

}
