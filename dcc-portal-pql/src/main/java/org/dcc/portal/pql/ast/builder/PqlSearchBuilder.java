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
package org.dcc.portal.pql.ast.builder;

import static java.util.Collections.addAll;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.RootNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.es.model.Order;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@NoArgsConstructor(access = PRIVATE)
public class PqlSearchBuilder {

  private final Map<String, Order> sort = Maps.newHashMap();
  private final List<String> select = Lists.newArrayList();
  private final List<String> facets = Lists.newArrayList();
  private LimitNode limit;
  private FilterBuilder filterBuilder;

  public static PqlSearchBuilder create() {
    return new PqlSearchBuilder();
  }

  public PqlSearchBuilder select(@NonNull String... field) {
    addAll(select, field);

    return this;
  }

  public PqlSearchBuilder selectAll() {
    select.add(SelectNode.ALL_FIELDS);

    return this;
  }

  public PqlSearchBuilder filter(@NonNull FilterBuilder builder) {
    this.filterBuilder = builder;

    return this;
  }

  public PqlSearchBuilder sort(@NonNull String field, @NonNull Order order) {
    sort.put(field, order);

    return this;
  }

  public PqlSearchBuilder limit(int size) {
    this.limit = new LimitNode(0, size);

    return this;
  }

  public PqlSearchBuilder limit(int from, int size) {
    this.limit = new LimitNode(from, size);

    return this;
  }

  public PqlSearchBuilder facets(@NonNull String... facet) {
    addAll(facets, facet);

    return this;
  }

  public PqlSearchBuilder facetsAll() {
    facets.add(FacetsNode.ALL_FACETS);

    return this;
  }

  public RootNode build() {
    val result = new RootNode();

    if (!select.isEmpty()) {
      result.setSelect(new SelectNode(select));
    }

    if (limit != null) {
      result.setLimit(limit);
    }

    if (!facets.isEmpty()) {
      result.setFacets(new FacetsNode(facets));
    }

    if (filterBuilder != null) {
      result.setFilters(filterBuilder.build());
    }

    if (!sort.isEmpty()) {
      val sortNode = new SortNode();
      for (val entry : sort.entrySet()) {
        sortNode.addField(entry.getKey(), entry.getValue());
      }

      result.setSort(sortNode);
    }

    return result;
  }

}
