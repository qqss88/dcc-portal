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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Iterables.filter;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.FormatUtils._;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@NoArgsConstructor(access = PRIVATE)
public class Nodes {

  public static <T> List<T> filterChildren(@NonNull ExpressionNode node, @NonNull Class<T> childType) {
    val children = filter(node.getChildren(), childType);

    return Lists.<T> newArrayList(children);
  }

  /**
   * Get an {@link Optional} of type {@code childType}. Ensures that the {@code parent} has only a single child of that
   * type if any.
   */
  public static <T extends ExpressionNode> Optional<T> getChildOptional(@NonNull ExpressionNode parent,
      @NonNull Class<T> childType) {

    val childrenList = filterChildren(parent, childType);
    if (childrenList.isEmpty()) {
      return absent();
    } else if (childrenList.size() > 1) {
      throw new IllegalStateException(_("RootNode contains more that one child of type %s. %s", childType, parent));
    }

    return fromNullable(childrenList.get(0));
  }

}
