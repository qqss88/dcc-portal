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

import static org.icgc.dcc.common.core.util.FormatUtils._;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.visitor.NodeVisitor;

@Getter
@EqualsAndHashCode(callSuper = true)
public class TermsFacetNode extends ExpressionNode {

  private static final String GLOBAL = "global";
  private static final String NON_GLOBAL = "non-global";

  @NonNull
  private final String field;
  private boolean global;

  public TermsFacetNode(@NonNull String field) {
    this.field = field;
    this.global = false;
  }

  public TermsFacetNode(@NonNull String field, boolean global, ExpressionNode... children) {
    super(children);
    this.field = field;
    this.global = global;
  }

  public void setGlobal() {
    global = true;
  }

  @Override
  public <T> T accept(NodeVisitor<T> visitor) {
    return visitor.visitTermsFacet(this);
  }

  @Override
  public String toString() {
    val scope = global ? GLOBAL : NON_GLOBAL;

    return _("[%s Scope: %s]", field, scope) + super.toString();
  }

}
