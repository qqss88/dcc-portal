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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import lombok.NonNull;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.ExpressionType;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.TerminalType;
import org.dcc.portal.pql.es.builder.TermBuilder;
import org.dcc.portal.pql.es.internal.ast.ExpressionNodeImpl;
import org.dcc.portal.pql.es.internal.ast.TerminalNodeImpl;

public class TermBuilderImpl implements TermBuilder {

  private TerminalNode nameNode;
  private TerminalNode valueNode;
  private ExpressionNodeImpl root = new ExpressionNodeImpl(null, ExpressionType.TERM);

  @Override
  public TermBuilder name(@NonNull String name) {
    checkState(!name.isEmpty(), "Empty name");
    nameNode = new TerminalNodeImpl(TerminalType.NAME, root, name);
    root.addChildren(nameNode);

    return this;
  }

  @Override
  public TermBuilder value(@NonNull String value) {
    checkState(!value.isEmpty(), "Empty name");
    valueNode = new TerminalNodeImpl(TerminalType.VALUE, root, value);
    root.addChildren(valueNode);

    return this;
  }

  @Override
  public ExpressionNodeImpl build() {
    checkNotNull(nameNode, "Name node is not initialized");
    checkNotNull(valueNode, "Value node is not initialized");

    return root;
  }

  @Override
  public void parent(@NonNull ExpressionNode parent) {
    root.setParent(parent);
  }

}
