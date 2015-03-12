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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
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
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.qe.QueryContext;
import org.icgc.dcc.portal.model.IndexModel.Type;

/**
 * Scores queries made against the MutationCentric type.<br>
 * <br>
 * Creates a NestedNode with a ConstantScoreNode child. Sets scoring mode on the nested on 'ssm_occurrence' queries to
 * 'total'. The ConstantScoreNode has boost 1. If the original QueryNode has a FilterNode then the FilterNode is added
 * as a child of the ConstantScoreNode.<br>
 * <br>
 * <b>NB:</b> This visitor must be run as the latest one, after all the other processing rules applied.
 */
@Slf4j
public class ScoreMutatationQueryVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

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
    verifyQueryNodeStructure(queryNode);
    val filterNode = Nodes.cloneNode(queryNode.getFirstChild());
    val constantScoreNode = new ConstantScoreNode(BOOST, filterNode);
    val nestedNode = new NestedNode(NESTED_PATH, ScoreMode.TOTAL, constantScoreNode);
    queryNode.setChild(0, nestedNode);
  }

  private void verifyQueryNodeStructure(QueryNode queryNode) {
    checkState(queryNode.childrenCount() == 1 && queryNode.getFirstChild() instanceof FilterNode,
        format("Malformed QueryNode. At this stage a QueryNode can have a FilterNode child only. %s", queryNode));
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
