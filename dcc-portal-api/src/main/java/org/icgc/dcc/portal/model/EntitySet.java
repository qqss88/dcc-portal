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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Represents an entity set.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EntitySet")
@JsonInclude(NON_NULL)
public class EntitySet extends BaseEntitySet implements Identifiable<UUID> {

  @NonNull
  @ApiModelProperty(value = "ID of this entity set.", required = true)
  private final UUID id;

  @NonNull
  @ApiModelProperty(value = "The processing state for this entity set.", required = true)
  private State state;

  @ApiModelProperty(value = "Number of elements in this entity set.")
  private Long count;

  @NonNull
  @ApiModelProperty(value = "Sub-type for this entity set.", required = true)
  private SubType subtype = SubType.NORMAL;

  public EntitySet updateStateToInProgress() {
    this.state = State.IN_PROGRESS;
    return this;
  }

  public EntitySet updateStateToFinished(final long count) {
    this.state = State.FINISHED;
    this.count = count;
    return this;
  }

  public EntitySet updateStateToError() {
    this.state = State.ERROR;
    return this;
  }

  private final static class JsonPropertyName {

    final static String id = "id";
    final static String state = "state";
    final static String count = "count";
    final static String name = "name";
    final static String description = "description";
    final static String type = "type";
  }

  @JsonCreator
  public EntitySet(
      @JsonProperty(JsonPropertyName.id) final UUID id,
      @JsonProperty(JsonPropertyName.state) final State state,
      @JsonProperty(JsonPropertyName.count) final Long count,
      @JsonProperty(JsonPropertyName.name) final String name,
      @JsonProperty(JsonPropertyName.description) final String description,
      @JsonProperty(JsonPropertyName.type) final Type type) {
    super(name, description, type);

    this.id = id;
    this.state = state;
    this.count = count;
  }

  private EntitySet(final UUID id, @NonNull final String name, final String description,
      @NonNull final Type type, final State status, final Long count) {
    super(name, description, type);

    this.id = id;
    this.state = status;
    this.count = count;
  }

  // static constructors
  public static EntitySet createFromDefinition(@NonNull final BaseEntitySet definition) {
    return new EntitySet(
        UUID.randomUUID(),
        definition.getName(),
        definition.getDescription(),
        definition.getType(),
        State.PENDING,
        null);
  }

  public static EntitySet forNewlyCreated(@NonNull final String name, final String description, @NonNull final Type type) {
    return new EntitySet(
        UUID.randomUUID(),
        name,
        description,
        type,
        State.PENDING,
        null);
  }

  public static EntitySet createForStatusInProgress(final UUID id, @NonNull final String name,
      final String description, @NonNull final Type type) {
    return new EntitySet(
        id,
        name,
        description,
        type,
        State.IN_PROGRESS,
        null);
  }

  public static EntitySet createForStatusFinished(final UUID id, final String name, final String description,
      final Type type, final long count) {
    Preconditions.checkArgument(count >= 0, "The 'count' argument must be a positive number.");

    return new EntitySet(id, name, description, type, State.FINISHED, count);
  }

  @RequiredArgsConstructor
  @Getter
  @ApiModel(value = "State")
  public enum State {
    PENDING("pending"),
    IN_PROGRESS("in progess"),
    FINISHED("finished"),
    ERROR("error");

    @NonNull
    private final String name;
  }

  @RequiredArgsConstructor
  @Getter
  @ApiModel(value = "Subtype")
  public enum SubType {
    NORMAL("normal"),
    UPLOAD("upload"),
    ENRICHMENT("enrichment"),
    TRANSIENT("transient");

    @NonNull
    private final String name;
  }
}
