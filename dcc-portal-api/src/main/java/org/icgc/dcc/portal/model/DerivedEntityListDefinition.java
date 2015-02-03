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

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * TODO
 */
@Value
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "DerivedEntityListDefinition")
public class DerivedEntityListDefinition extends BaseEntityList {

  @NonNull
  private final List<UnionUnit> union;

  // This flag indicates whether the resulting list is meant to be temporary.
  private boolean isTemp;

  private final static class JsonPropertyName {

    final static String union = "union";
    final static String name = "name";
    final static String description = "description";
    final static String type = "type";
    final static String isTemp = "isTemp";
  }

  @JsonCreator
  public DerivedEntityListDefinition(
      @JsonProperty(JsonPropertyName.union) final List<UnionUnit> union,
      @NonNull @JsonProperty(JsonPropertyName.name) final String name,
      @JsonProperty(JsonPropertyName.description) final String description,
      @NonNull @JsonProperty(JsonPropertyName.type) final Type type,
      @JsonProperty(JsonPropertyName.isTemp) final boolean isTemp) {

    super(name, description, type);

    validateUnion(union);
    this.union = union;
    this.isTemp = isTemp;
  }

  /**
   * @param union
   */
  private void validateUnion(final List<UnionUnit> union) {
    if (union.isEmpty()) {
      throw new IllegalArgumentException("The union argument must not be empty.");
    }
    else {
      for (val unionUnit : union) {
        if (!unionUnit.isValid()) {
          throw new IllegalArgumentException("Not all union units in the union argument are valid.");
        }
      }
    }
  }
}
