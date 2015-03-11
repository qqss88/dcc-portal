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

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;

import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.qe.QueryContext;

@NoArgsConstructor(access = PRIVATE)
public class VisitorHelpers {

  /**
   * Checks if {@code optional} has a reference.
   * @throws IllegalArgumentException
   */
  public static <T> void checkOptional(Optional<T> optional) {
    checkArgument(optional.isPresent(), "The optional does not contain any reference.");
  }

  /**
   * The methods visits children of {@code parent} with {@code visitor}. Each child returns an
   * {@code Optional<ExpressionNode>}. If the optional is not empty the child is replaced with the
   * {@link ExpressionNode} from the {@code Optional}.<br>
   * 
   * @param visitor applied to the {@code parent}
   * @param parent to be visited
   * @param context - query context
   */
  public static Optional<ExpressionNode> visitChildren(NodeVisitor<Optional<ExpressionNode>, QueryContext> visitor,
      ExpressionNode parent, Optional<QueryContext> context) {
    for (int i = 0; i < parent.childrenCount(); i++) {
      val child = parent.getChild(i);
      val childResult = child.accept(visitor, context);
      if (childResult.isPresent()) {
        parent.setChild(i, childResult.get());
      }
    }

    return Optional.empty();
  }

}
