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
package org.dcc.portal.pql.es.visitor.score;

import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ConstantScoreNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.qe.QueryContext;
import org.icgc.dcc.portal.model.IndexModel.Type;

/**
 * Scores queries made against the MutationCentric type.<br>
 * <br>
 * Sets scoring mode on the nested on 'ssm_occurrence' queries to 'sum' and wraps the nested's query child query in a
 * constant score query with boost 1.<br>
 * <br>
 * If there is no nested query in the current request the class creates one.<br>
 * <br>
 * <b>NB:</b> This visitor must be run as the latest one, after all the other processing rules applied.
 */
@Slf4j
public class ScoreMutatationQueryVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  // Implementation details:
  // If any visited child returns an Optional<NestedNode> that means that the QueryNode contains a nested node on the
  // NESTED_PATH and the correct processing happened in the visitNested method.
  // If a method returns an empty Optional that means neither the child nor its children are nested node on the
  // NESTED_PATH. Thus, NestedNode with a ConstantScoreNode child must be created.

  private static final float BOOST = 1f;
  private static final String NESTED_PATH = "ssm_occurrence";
  private static final ScoreMode DEFAULT_SCORE_MODE = ScoreMode.TOTAL;

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, Optional<QueryContext> context) {
    checkOptional(context);
    if (context.get().getType() != Type.MUTATION_CENTRIC) {
      log.debug("Skipping processing for non mutation-centric type");

      return Optional.of(node);
    }

    val queryNodeOpt = Nodes.getOptionalChild(node, QueryNode.class);
    if (!queryNodeOpt.isPresent()) {
      log.debug("[visitRoot] No QueryNode found. Creating one.");

      return Optional.of(createQueryNode(node));
    }

    processQueryNode(queryNodeOpt.get(), context);

    return Optional.of(node);
  }

  private void processQueryNode(QueryNode queryNode, Optional<QueryContext> context) {
    for (val child : queryNode.getChildren()) {
      val visitResult = child.accept(this, context);
      if (visitResult.isPresent()) {
        // Nothing to do. There is a proper nested node which has been processed
        // FIXME: How correctly process nested fields which are nested on a deeper level (ssm_occurrence.observation)?
        // FIXME: How process multiple fields on the same nested level but which are not explicitly nested
        return;
      }
    }
  }

  private static ExpressionNode createQueryNode(RootNode node) {
    val nestedNode = new NestedNode(NESTED_PATH, DEFAULT_SCORE_MODE, createConstantScoreNode());
    val queryNode = new QueryNode(nestedNode);
    node.addChildren(queryNode);

    return node;
  }

  private static ExpressionNode createConstantScoreNode() {
    return new ConstantScoreNode(BOOST);
  }

}
