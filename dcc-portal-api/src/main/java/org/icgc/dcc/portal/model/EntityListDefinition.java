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
package org.icgc.dcc.portal.model;

import static com.google.common.base.Strings.isNullOrEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * TODO
 */
@Value
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EntityListDefinition")
public class EntityListDefinition extends BaseEntityList {

  @NonNull
  private final FiltersParam filters;

  @NonNull
  private final String sortBy;

  @NonNull
  private final SortOrder sortOrder;

  private final static class JsonPropertyName {

    final static String filters = "filters";
    final static String sortBy = "sortBy";
    final static String sortOrder = "sortOrder";
    final static String name = "name";
    final static String description = "description";
    final static String type = "type";
  }

  @JsonCreator
  public EntityListDefinition(
      @JsonProperty(JsonPropertyName.filters) final FiltersParam filters,
      @JsonProperty(JsonPropertyName.sortBy) final String sortBy,
      @JsonProperty(JsonPropertyName.sortOrder) final SortOrder sortOrder,
      @NonNull @JsonProperty(JsonPropertyName.name) final String name,
      @JsonProperty(JsonPropertyName.description) final String description,
      @NonNull @JsonProperty(JsonPropertyName.type) final Type type) {

    super(name, description, type);

    if (isNullOrEmpty(sortBy)) {
      throw new IllegalArgumentException("The sortBy argument must contain a valid expression.");
    }
    this.sortBy = sortBy;
    this.filters = filters;
    this.sortOrder = sortOrder;
  }

  @RequiredArgsConstructor
  @Getter
  public enum SortOrder {

    ASCENDING("asc"),
    DESCENDING("desc");

    private final String name;
  }
}
