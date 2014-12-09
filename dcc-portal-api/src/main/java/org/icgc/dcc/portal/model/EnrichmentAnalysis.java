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
package org.icgc.dcc.portal.model;

import java.util.List;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "EnrichmentAnalysis")
@Accessors(chain = true)
public class EnrichmentAnalysis implements Identifiable<UUID> {

  /**
   * Constants.
   */
  public static final int MAX_INPUT_GENES = 1000;
  public static final int MAX_OUTPUT_GENE_SETS = 100;
  public static final float DEFAULT_FDR = 0.05f;
  public static final float MIN_FDR = 0.005f;
  public static final float MAX_FDR = 0.5f;

  /**
   * Resource identifier
   */
  UUID id;

  /**
   * State of the analysis
   */
  State state;

  /**
   * Input from Advanced Search
   */
  @NotNull
  Query query;

  @NotNull
  Params params;

  Overview overview;

  List<Result> results;

  @Data
  public static class Params {

    /**
     * UI: "# Input genes"
     */
    @Max(MAX_INPUT_GENES)
    int maxGeneCount;

    /**
     * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
     */
    @Max(MAX_OUTPUT_GENE_SETS)
    int maxGeneSetCount;

    /**
     * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
     */
    float fdr = DEFAULT_FDR;

    /**
     * Background
     */
    Universe universe;

  }

  @Data
  @Accessors(chain = true)
  public static class Overview {

    /**
     * UI: "#Gene sets in overlap"
     */
    int overlapGeneSetCount;

    /**
     * UI: "#Gene sets in Universe"
     */
    int universeGeneSetCount;

    /**
     * UI: "#Genes in overlap"
     */
    int overlapGeneCount;

    /**
     * UI: "#Genes in Universe"
     */
    int universeGeneCount;

  }

  @Data
  @Accessors(chain = true)
  public static class Result {

    /**
     * UI: "ID"
     */
    String geneSetId;

    /**
     * UI: "Name"
     */
    String geneSetName;

    /**
     * UI: "#Genes"
     */
    int geneCount;

    /**
     * UI: "#Genes in overlap"
     */
    int overlapGeneCount;

    /**
     * UI: "#Donors in overlap"
     */
    int overlapDonorCount;

    /**
     * UI: "#Mutations"
     */
    int overlapMutationCount;

    /**
     * UI: "Expected"
     */
    double expectedValue;

    /**
     * UI: "P-Value"
     */
    double pValue;

    /**
     * UI: "Adjusted P-Value"
     */
    double adjustedPValue;

  }

  public enum State {

    EXECUTING,
    FINISHED;

  }

}
