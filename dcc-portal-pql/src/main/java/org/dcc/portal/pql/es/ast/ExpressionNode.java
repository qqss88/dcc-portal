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
package org.dcc.portal.pql.es.ast;

import java.util.Collection;
import java.util.List;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Lists;

public abstract class ExpressionNode implements Node {

  protected Node parent;
  protected List<ExpressionNode> children;

  public ExpressionNode(ExpressionNode... children) {
    this.children = Lists.newArrayList(children);
    assignParent(this.children, this);
  }

  private static void assignParent(Collection<ExpressionNode> children, ExpressionNode parent) {
    for (val child : children) {
      child.setParent(parent);
    }
  }

  @Override
  public Node getParent() {
    return parent;
  }

  public void setParent(@NonNull ExpressionNode parent) {
    this.parent = parent;
  }

  @Override
  public abstract <T> T accept(NodeVisitor<T> visitor);

  public void addChildren(@NonNull ExpressionNode... children) {
    val childrenList = Lists.newArrayList(children);
    this.children.addAll(childrenList);
    assignParent(childrenList, this);
  }

  @Override
  public List<ExpressionNode> getChildren() {
    return children;
  }

  @Override
  public ExpressionNode getChild(int index) {
    return children.get(index);
  }

  @Override
  public String toString() {
    val stringBuffer = new StringBuffer();
    stringBuffer.append(this.getClass().getSimpleName() + " ( ");

    for (int i = 0; i < children.size(); i++) {
      stringBuffer.append(children.get(i).toString());
      if (i < children.size() - 1) {
        stringBuffer.append(", ");
      }
    }
    stringBuffer.append(")");

    return stringBuffer.toString();
  }

}
