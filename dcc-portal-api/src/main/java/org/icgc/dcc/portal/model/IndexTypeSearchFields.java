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

import static com.google.common.collect.Iterables.transform;

import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * This represents a collection of raw field names grouped by the type of search analyzer (or lack of) defined by the
 * data model/mappings.
 */
@Value
@Builder
public class IndexTypeSearchFields {

  // These suffixes are defined and used in our elasticsearch index models.
  private static final String EXACT_MATCH_SUFFIX = ".raw";
  private static final String PARTIAL_MATCH_SUFFIX = ".analyzed";
  private static final String LOWERCASE_MATCH_SUFFIX = ".search";

  List<String> exactMatchFields;
  List<String> partialMatchFields;
  List<String> lowercaseMatchFields;

  public static IndexTypeSearchFieldsBuilder indexTypeSearchFields() {
    return builder();
  }

  public Iterable<String> toEsFieldNames(@NonNull Iterable<String> fields) {
    return transform(fields, field -> field + getSearchSuffix(field));
  }

  private static boolean contains(List<String> matchFields, String fieldName) {
    return matchFields != null && matchFields.contains(fieldName);
  }

  private String getSearchSuffix(String fieldName) {
    if (null == fieldName) {
      return "";
    }

    if (contains(exactMatchFields, fieldName)) {
      return EXACT_MATCH_SUFFIX;
    } else if (contains(partialMatchFields, fieldName)) {
      return PARTIAL_MATCH_SUFFIX;
    } else if (contains(lowercaseMatchFields, fieldName)) {
      return LOWERCASE_MATCH_SUFFIX;
    } else {
      // Pass-through
      return "";
    }
  }
}
