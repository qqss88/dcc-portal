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
package org.dcc.portal.pql.es.internal.builder;

import lombok.NonNull;

import org.dcc.portal.pql.es.ast.ExpressionType;
import org.dcc.portal.pql.es.builder.BoolBuilder;
import org.dcc.portal.pql.es.builder.TermBuilder;
import org.dcc.portal.pql.es.internal.ast.ExpressionNodeImpl;
import org.dcc.portal.pql.es.internal.utils.NodeUtils;

public class BoolBuilderImpl implements BoolBuilder {

  private ExpressionNodeImpl root = createRoot();
  private ExpressionNodeImpl mustNode;

  @Override
  public BoolBuilder mustTerm(@NonNull ExpressionNodeImpl termNode) {
    if (mustNode == null) {
      mustNode = NodeUtils.createExpressionNode(root, ExpressionType.MUST);
      root.addChildren(mustNode);
    }

    termNode.setParent(mustNode);
    mustNode.addChildren(termNode);

    return this;
  }

  @Override
  public TermBuilder shouldTerm() {
    return null;

  }

  @Override
  public TermBuilder shouldNotTerm() {
    return null;

  }

  @Override
  public ExpressionNodeImpl build() {
    return root;
  }

  private static ExpressionNodeImpl createRoot() {
    return new ExpressionNodeImpl(null, ExpressionType.BOOL);
  }

}
