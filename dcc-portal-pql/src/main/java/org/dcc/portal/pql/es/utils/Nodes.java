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
package org.dcc.portal.pql.es.utils;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.List;

import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Nodes {

  /**
   * Gets a child by type {@code childType}.
   * @throws IllegalStateException if there are more than one child of type {@code childType}
   */
  public static <T> T filterChild(ExpressionNode node, Class<T> childType) {
    val children = filterChildren(node, childType);
    checkState(children.size() == 1, "The result contains more than one child. Size: %d", children.size());

    return Iterables.get(children, 0);
  }

  public static <T> List<T> filterChildren(ExpressionNode node, Class<T> childType) {
    val children = Iterables.filter(node.getChildren(), childType);

    return Lists.<T> newArrayList(children);
  }

  public static <T> T filterSingle(Collection<ExpressionNode> nodes, Class<T> childType) {
    val children = Iterables.filter(nodes, childType);
    val filtered = Lists.<T> newArrayList(children);
    if (filtered.isEmpty()) {
      return null;
    }

    checkState(filtered.size() == 1, "The result contains more than one child. Size: %d", filtered.size());

    return filtered.get(0);
  }

}
