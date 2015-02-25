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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * Represents the result from a set analysis.
 */
@Data
@ApiModel(value = "UnionAnalysisResult")
@JsonInclude(NON_NULL)
public class UnionAnalysisResult implements Identifiable<UUID> {

  @NonNull
  private final UUID id;
  @NonNull
  private State state;
  @NonNull
  private final BaseEntitySet.Type type;

  private final long timestamp = new Date().getTime();

  private List<UnionUnitWithCount> result;

  public UnionAnalysisResult updateStateToInProgress() {
    this.state = State.IN_PROGRESS;
    return this;
  }

  public UnionAnalysisResult updateStateToFinished(@NonNull List<UnionUnitWithCount> result) {
    this.state = State.FINISHED;
    this.result = result;
    return this;
  }

  public UnionAnalysisResult updateStateToError() {
    this.state = State.ERROR;
    return this;
  }

  private final static class JsonPropertyName {

    final static String id = "id";
    final static String state = "state";
    final static String result = "result";
    final static String type = "type";
  }

  @JsonCreator
  public UnionAnalysisResult(
      @JsonProperty(JsonPropertyName.id) final UUID id,
      @JsonProperty(JsonPropertyName.state) final State state,
      @JsonProperty(JsonPropertyName.type) final BaseEntitySet.Type type,
      @JsonProperty(JsonPropertyName.result) final List<UnionUnitWithCount> result) {
    this.id = id;
    this.state = state;
    this.type = type;
    this.result = result;
  }

  // static constructors
  public static UnionAnalysisResult createForNewlyCreated(final BaseEntitySet.Type entityType) {
    return new UnionAnalysisResult(
        UUID.randomUUID(),
        State.PENDING,
        entityType,
        null);
  }

  public static UnionAnalysisResult createForInProgress(final UUID id, final BaseEntitySet.Type entityType) {
    return new UnionAnalysisResult(
        id,
        State.IN_PROGRESS,
        entityType,
        null);
  }

  public static UnionAnalysisResult createWithResult(final UUID id, final BaseEntitySet.Type entityType,
      @NonNull List<UnionUnitWithCount> result) {
    Preconditions.checkArgument(!result.isEmpty(), "The 'result' argument must not be empty.");

    return new UnionAnalysisResult(
        id,
        State.FINISHED,
        entityType,
        result);
  }

  @RequiredArgsConstructor
  @Getter
  public enum State {

    PENDING("pending"),
    IN_PROGRESS("in progess"),
    FINISHED("finished"),
    ERROR("error");

    private final String name;
  }
}
