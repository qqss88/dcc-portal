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
package org.dcc.portal.pql.es.internal.ast;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.ExpressionType;
import org.dcc.portal.pql.es.ast.Node;

import com.google.common.collect.Lists;

public class ExpressionNodeImpl implements ExpressionNode {

  @Setter
  @Getter
  private ExpressionNode parent;

  @Getter
  private ExpressionType type;
  private List<Node> children = Lists.newArrayList();

  public ExpressionNodeImpl(ExpressionNode parent, @NonNull ExpressionType type) {
    this.parent = parent;
    this.type = type;
  }

  @Override
  public int getChildrenCount() {
    return children.size();
  }

  @Override
  public Object getPayload() {
    return type;
  }

  @Override
  public Node getChild(int index) {
    return children.get(index);
  }

  public void addChildren(@NonNull Node node) {
    children.add(node);
  }

}
