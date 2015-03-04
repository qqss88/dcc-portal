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
import org.dcc.portal.pql.es.visitor.score.ScoreQueryVisitor;
import org.dcc.portal.pql.meta.AbstractTypeModel;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.MutationCentricTypeModel;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel.Type;

@NoArgsConstructor(access = PRIVATE)
public class Visitors {

  private static final NodeVisitor<String> TO_STRING_VISITOR = new ToStringVisitor();
  private static final NodeVisitor<ExpressionNode> CLONE_VISITOR = new CloneNodeVisitor();

  private static final CreateAggregationBuilderVisitor MUTATION_AGGREGATION_BUILDER_VISITOR =
      new CreateAggregationBuilderVisitor(IndexModel.getMutationCentricTypeModel());

  private static final FilterBuilderVisitor MUTATION_FILTER_VISITOR = new FilterBuilderVisitor(
      IndexModel.getMutationCentricTypeModel());

  private static final RemoveAggregationFilterVisitor REMOVE_AGGS_FILTER_VISITOR = new RemoveAggregationFilterVisitor();

  /*
   * ScoreQueryVisitors
   */
  private static final DefaultScoreQueryVisitor DEFAULT_SCORE_QUERY_VISITOR = new DefaultScoreQueryVisitor();
  private static final DonorScoreQueryVisitor DONOR_SCORE_QUERY_VISITOR = new DonorScoreQueryVisitor();

  // FIXME: implement

  public static void createFacetBuilderVisitor() {
  }

  public static CreateFilterBuilderVisitor createFilterBuilderVisitor(Client client, AbstractTypeModel typeModel) {
    return new CreateFilterBuilderVisitor(client, typeModel);
  }

  public static void createEmptyNodesCleanerVisitor() {
  }

  public static void createFacetFiltersVisitor() {
  }

  public static void createFacetsResolverVisitor() {
  }

  public static NodeVisitor<String> createToStringVisitor() {
    return TO_STRING_VISITOR;
  }

  public static NodeVisitor<ExpressionNode> createCloneNodeVisitor() {
    return CLONE_VISITOR;
  }

  public static CreateAggregationBuilderVisitor createAggregationBuilderVisitor(AbstractTypeModel typeModel) {
    if (typeModel instanceof MutationCentricTypeModel) {
      return MUTATION_AGGREGATION_BUILDER_VISITOR;
    }

    // FIXME: Add the other typeModels

    throw new IllegalArgumentException();
  }

  public static FilterBuilderVisitor filterBuilderVisitor(AbstractTypeModel typeModel) {
    if (typeModel instanceof MutationCentricTypeModel) {
      return MUTATION_FILTER_VISITOR;
    }

    throw new IllegalArgumentException();
  }

  public static RemoveAggregationFilterVisitor createRemoveAggregationFilterVisitor() {
    return REMOVE_AGGS_FILTER_VISITOR;
  }

  public static ScoreQueryVisitor createScoreQueryVisitor(Type type) {
    switch (type) {
    case DONOR_CENTRIC:
      return DONOR_SCORE_QUERY_VISITOR;
    default:
      return DEFAULT_SCORE_QUERY_VISITOR;
    }
  }

}
