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

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.meta.IndexModel;

/**
 * Scores queries made against the MutationCentric type.<br>
 * <br>
 * Creates a NestedNode with a ConstantScoreNode child. Sets scoring mode on the nested on 'ssm_occurrence' queries to
 * 'total'. The ConstantScoreNode has boost 1. If the original QueryNode has a FilterNode then the FilterNode is added
 * as a child of the ConstantScoreNode.<br>
 * <br>
 * <b>NB:</b> This visitor must be run as the latest one, after all the other processing rules applied.
 */
public class MutatationScoreQueryVisitor extends ScoreQueryVisitor {

  private static final String NESTED_PATH = "ssm_occurrence";
  private static final ScoreMode DEFAULT_SCORE_MODE = ScoreMode.TOTAL;
  private static final String SCRIPT = "1";

  public MutatationScoreQueryVisitor() {
    super(createNestedNode(), IndexModel.getMutationCentricTypeModel());
  }

  private static NestedNode createNestedNode() {
    return new NestedNode(NESTED_PATH, DEFAULT_SCORE_MODE, createFunctionScoreNode());
  }

  private static ExpressionNode createFunctionScoreNode() {
    return new FunctionScoreNode(SCRIPT);
  }

}
