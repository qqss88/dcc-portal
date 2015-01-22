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

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import lombok.Data;
import lombok.NonNull;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * TODO
 */
@Data
public class UnionAnalysisRequest {

  private static final int REQUIRED_ELEMENT_COUNT = 2;

  @NonNull
  private final ImmutableSet<UUID> lists;

  @NonNull
  private final BaseEntityList.Type type;

  private final static class JsonPropertyName {

    final static String lists = "lists";
    final static String type = "type";
  }

  @JsonCreator
  public UnionAnalysisRequest(
      @NonNull @JsonProperty(JsonPropertyName.lists) final Collection<UUID> lists,
      @NonNull @JsonProperty(JsonPropertyName.type) final BaseEntityList.Type type) {

    val uniqueItems = ImmutableSet.copyOf(lists);

    if (uniqueItems.size() < REQUIRED_ELEMENT_COUNT) {

      throw new IllegalArgumentException(
          "The entityLists argument must contain at least " +
              REQUIRED_ELEMENT_COUNT +
              " unique elements.");
    }
    this.lists = uniqueItems;
    this.type = type;
  }

  /*
   * Build a data structure to represent all the possible "smallest" combinations from a set.
   */
  public ImmutableList<UnionUnit> toUnionSets() {

    val setSize = lists.size();
    val subsets = Sets.powerSet(this.lists);

    val predicate = new Predicate<Collection<?>>() {

      @Override
      public boolean apply(Collection<?> s) {

        val size = s.size();
        return (size > 0) && (size < setSize);
      }
    };
    val filteredSubsets = Sets.filter(subsets, predicate);
    val resultSize = filteredSubsets.size();

    val result = new ArrayList<UnionUnit>(resultSize + 1);

    for (val subset : filteredSubsets) {

      val unionUnit = new UnionUnit(Sets.difference(this.lists, subset), subset);
      result.add(unionUnit);
    }
    result.add(UnionUnit.noExclusionInstance(this.lists));

    return ImmutableList.copyOf(result);
  }
}
