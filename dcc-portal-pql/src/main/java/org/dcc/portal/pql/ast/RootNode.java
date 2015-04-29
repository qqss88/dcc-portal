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
package org.dcc.portal.pql.ast;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.dcc.portal.pql.ast.filter.FilterNode;
import org.dcc.portal.pql.ast.function.CountNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.ast.visitor.PqlNodeVisitor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RootNode extends PqlNode {

  private SelectNode select;
  private FacetsNode facets;
  private CountNode count;
  private SortNode sort;
  private LimitNode limit;
  private FilterNode filters;

  public RootNode(@NonNull CountNode count) {
    this.count = count;
  }

  public boolean hasSelect() {
    return select != null;
  }

  public void setSelect(@NonNull SelectNode node) {
    canUpdateField();
    this.select = node;
    node.setParent(this);
    addChild(node);
  }

  public boolean hasFacets() {
    return facets != null;
  }

  public void setFacets(@NonNull FacetsNode node) {
    canUpdateField();
    this.facets = node;
    node.setParent(this);
    addChild(node);
  }

  public boolean isCount() {
    return count != null;
  }

  public boolean hasSort() {
    return sort != null;
  }

  public void setSort(@NonNull SortNode node) {
    canUpdateField();
    this.sort = node;
    node.setParent(this);
    addChild(node);
  }

  public boolean hasLimit() {
    return limit != null;
  }

  public void setLimit(@NonNull LimitNode node) {
    canUpdateField();
    this.limit = node;
    node.setParent(this);
    addChild(node);
  }

  public boolean hasFilters() {
    return filters != null;
  }

  public void setFilters(@NonNull FilterNode node) {
    this.filters = node;
    node.setParent(this);
    addChild(node);
  }

  @Override
  public Type type() {
    return Type.ROOT;
  }

  @Override
  public <T, A> T accept(@NonNull PqlNodeVisitor<T, A> visitor, @NonNull Optional<A> context) {
    return visitor.visitRoot(this, context);
  }

  @Override
  public RootNode toRootNode() {
    return this;
  }

  private void canUpdateField() {
    if (isCount()) {
      throw new IllegalArgumentException("This operation is not valid for a count request");
    }
  }

}
