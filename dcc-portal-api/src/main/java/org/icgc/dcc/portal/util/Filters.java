/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.util;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import com.fasterxml.jackson.databind.node.ObjectNode;

@NoArgsConstructor(access = PRIVATE)
public final class Filters {

  public static ObjectNode uploadedGeneListFilter(@NonNull String geneListId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("uploadedGeneList", is(geneListId));

    return geneFilter;
  }

  public static ObjectNode pathwayFilter() {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("hasPathway", true);

    return geneFilter;
  }

  public static ObjectNode geneSetFilter(@NonNull String geneSetId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("geneSetId", geneSetId);

    return geneFilter;
  }

  public static ObjectNode goTermFilter(@NonNull String goTermId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("hasGoTerm", true).put("goTermId", goTermId);

    return geneFilter;
  }

  public static ObjectNode geneFilter() {
    return entityFilter("gene");
  }

  public static ObjectNode enrichmentAnalysisFilter(@NonNull UUID analysisId) {
    val analysisFilter = geneFilter();
    analysisFilter.put("analysisId", analysisId.toString());

    return analysisFilter;
  }

  public static ObjectNode entityFilter(@NonNull String entityName) {
    val entityFilter = MAPPER.createObjectNode();
    entityFilter.with(entityName);

    return entityFilter;
  }

  private static ObjectNode is(@NonNull String value) {
    val is = MAPPER.createObjectNode();
    is.withArray("is").add(value);

    return is;
  }

}
