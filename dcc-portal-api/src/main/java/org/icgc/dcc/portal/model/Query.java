/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.util.JsonUtils;
import org.icgc.dcc.portal.util.ObjectNodeDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Builder(chain = true, fluent = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Query {

  private Integer defaultLimit = 100;

  @JsonDeserialize(using = ObjectNodeDeserializer.class)
  ObjectNode filters;
  @JsonDeserialize(using = ObjectNodeDeserializer.class)
  ObjectNode scoreFilters;

  List<String> fields;
  List<String> includes;
  int from;
  int size;
  Integer limit;
  String sort;
  String order;
  String score;
  String query;

  public ObjectNode getFilters() {
    return hasFilters() ? filters.deepCopy() : JsonUtils.MAPPER.createObjectNode();
  }

  public boolean hasFilters() {
    return filters != null && filters.size() > 0;
  }

  public boolean hasScoreFilters() {
    return scoreFilters != null && scoreFilters.size() > 0;
  }

  public boolean hasFields() {
    return fields != null && !fields.isEmpty();
  }

  public boolean hasInclude(String include) {
    return includes != null && includes.contains(include);
  }

  public int getFrom() {
    // Save as 0-base index where 0 and 1 are 0
    return from < 2 ? 0 : from - 1;
  }

  public int getSize() {
    if (limit != null) return size > limit ? limit : size;
    else
      return size > defaultLimit ? defaultLimit : size;
  }

  public SortOrder getOrder() {
    return SortOrder.valueOf(order.toUpperCase());
  }

}
