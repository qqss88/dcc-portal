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

import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FunctionScoreQueryNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * Adds score queries to DonorCentric and GeneCentric type models.
 */
public abstract class ScoreQueryVisitor extends NodeVisitor<ExpressionNode, QueryContext> {

  private static final ScoreMode SCORE_MODE = ScoreMode.TOTAL;

  protected ExpressionNode visitRoot(RootNode node, String script, String path) {
    val queryNode = Nodes.getOptionalChild(node, QueryNode.class);
    if (queryNode.isPresent()) {
      visitQueryNode(queryNode.get(), script, path);
    } else {
      createQueryNode(node, script, path);
    }

    return node;
  }

  private void visitQueryNode(QueryNode queryNode, String script, String path) {
    val children = removeChildren(queryNode);
    val nestedNode = createNodesStructure(script, path, children);
    nestedNode.setParent(queryNode);
    queryNode.addChildren(nestedNode);
  }

  private void createQueryNode(RootNode rootNode, String script, String path) {
    val nestedNode = createNodesStructure(script, path);
    val queryNode = new QueryNode(nestedNode);
    nestedNode.setParent(queryNode);
    rootNode.addChildren(queryNode);
  }

  private static ExpressionNode[] removeChildren(QueryNode queryNode) {
    ExpressionNode[] children = new ExpressionNode[queryNode.childrenCount()];
    for (int i = queryNode.childrenCount() - 1; i >= 0; i--) {
      children[i] = queryNode.getChild(i);
      queryNode.removeChild(i);
    }

    return children;
  }

  private static NestedNode createNodesStructure(String script, String path, ExpressionNode... children) {
    val functionScoreQueryNode = new FunctionScoreQueryNode(script, children);
    val nestedNode = new NestedNode(path, SCORE_MODE, functionScoreQueryNode);

    return nestedNode;
  }

}
