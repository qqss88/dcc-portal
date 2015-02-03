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
import com.wordnik.swagger.annotations.ApiModel;

/**
 * TODO
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EntityList")
@JsonInclude(NON_NULL)
public class EntityList extends BaseEntityList implements Identifiable<UUID> {

  @NonNull
  private final UUID id;

  @NonNull
  private Status status;

  private Long count;

  @NonNull
  private SubType subtype = SubType.NORMAL;

  public EntityList inProgress() {
    this.status = Status.IN_PROGRESS;
    return this;
  }

  public EntityList finished(final long count) {
    this.status = Status.FINISHED;
    this.count = count;
    return this;
  }

  private final static class JsonPropertyName {

    final static String id = "id";
    final static String status = "status";
    final static String count = "count";
    final static String name = "name";
    final static String description = "description";
    final static String type = "type";
  }

  @JsonCreator
  public EntityList(
      @JsonProperty(JsonPropertyName.id) final UUID id,
      @JsonProperty(JsonPropertyName.status) final Status status,
      @JsonProperty(JsonPropertyName.count) final Long count,
      @JsonProperty(JsonPropertyName.name) final String name,
      @JsonProperty(JsonPropertyName.description) final String description,
      @JsonProperty(JsonPropertyName.type) final Type type) {

    super(name, description, type);

    this.id = id;
    this.status = status;
    this.count = count;
  }

  // static constructors
  private EntityList(final UUID id, @NonNull final String name, final String description,
      @NonNull final Type type, final Status status, final Long count) {

    super(name, description, type);

    this.id = id;
    this.status = status;
    this.count = count;
  }

  // static constructors
  public static EntityList createFromDefinition(
      @NonNull final BaseEntityList definition) {

    return new EntityList(
        UUID.randomUUID(),
        definition.getName(),
        definition.getDescription(),
        definition.getType(),
        Status.PENDING,
        null);
  }

  // Each enum value should have its own class constructor.
  public static EntityList forNewlyCreated(
      @NonNull final String name,
      final String description,
      @NonNull final Type type) {

    return new EntityList(
        UUID.randomUUID(),
        name,
        description,
        type,
        Status.PENDING,
        null);
  }

  public static EntityList forStatusInProgress(
      final UUID id,
      @NonNull final String name,
      final String description,
      @NonNull final Type type) {

    return new EntityList(
        id,
        name,
        description,
        type,
        Status.IN_PROGRESS,
        null);
  }

  public static EntityList forStatusFinished(final UUID id, final String name, final String description,
      final Type type, final long count) {

    if (count < 0) {
      throw new IllegalArgumentException("The count argument must be a positive number.");
    }
    return new EntityList(id, name, description, type, Status.FINISHED, count);
  }

  @RequiredArgsConstructor
  @Getter
  public enum Status {

    PENDING("pending"),
    IN_PROGRESS("in progess"),
    FINISHED("finished");

    private final String name;
  }

  @RequiredArgsConstructor
  @Getter
  public enum SubType {
    NORMAL("normal"),
    UPLOAD("upload"),
    ENRICHMENT("enrichment"),
    TEMP("temp");

    private final String name;
  }
}
