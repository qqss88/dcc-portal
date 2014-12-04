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

import static org.icgc.dcc.portal.model.EnrichmentAnalysis.GeneSetType.GO_TERM;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.GeneSetType.PATHWAY;
import static org.icgc.dcc.portal.util.Filters.goTermFilter;
import static org.icgc.dcc.portal.util.Filters.pathwayFilter;

import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Data
@Accessors(chain = true)
public class EnrichmentAnalysis implements Identifiable<UUID> {

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
  Query query;

  Params params;

  Summary summary;

  List<Result> results;

  @Data
  public static class Params {

    /**
     * UI: "# Input genes"
     */
    int maxGeneCount;

    /**
     * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
     */
    int maxGeneSetCount;

    /**
     * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
     */
    float fdr;

    /**
     * Background
     */
    Universe universe;

  }

  @Data
  @Accessors(chain = true)
  public static class Summary {

    /**
     * UI: "#Gene sets in overlap"
     */
    int intersectionGeneSetCount;

    /**
     * UI: "#Gene sets in Universe"
     */
    int universeGeneSetCount;

    /**
     * UI: "#Genes in overlap"
     */
    int intersectionGeneCount;

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
    int intersectionGeneCount;

    /**
     * UI: "#Donors in overlap"
     */
    int intersectionDonorCount;

    /**
     * UI: "#Mutations"
     */
    int intersectionMutationCount;

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

  public enum GeneSetType {

    GO_TERM,
    PATHWAY;

  }

  @Getter
  public enum Universe {

    REACTOME_PATHWAYS(null, PATHWAY, "pathway") {

      @Override
      public ObjectNode getFilter() {
        return pathwayFilter();
      }

    },

    GO_MOLECULAR_FUNCTION("GO:0003674", GO_TERM, "go_term.molecular_function") {

      @Override
      public ObjectNode getFilter() {
        return goTermFilter(getGeneSetId());
      }

    },

    GO_BIOLOGICAL_PROCESS("GO:0008150", GO_TERM, "go_term.biological_process") {

      @Override
      public ObjectNode getFilter() {
        return goTermFilter(getGeneSetId());
      }

    },

    GO_CELLULAR_COMPONENT("GO:0005575", GO_TERM, "go_term.cellular_component") {

      @Override
      public ObjectNode getFilter() {
        return goTermFilter(getGeneSetId());
      }

    };

    /**
     * Metadata.
     */
    private final String geneSetId;
    @NonNull
    private final GeneSetType geneSetType;
    @NonNull
    private final String geneSetFacetName;

    /**
     * @return a query filter the targes this {@code Universe} type.
     */
    public abstract ObjectNode getFilter();

    // This seems to be needed for Lombok
    private Universe(String geneSetId, GeneSetType geneSetType, String geneSetFacetName) {
      this.geneSetId = geneSetId;
      this.geneSetType = geneSetType;
      this.geneSetFacetName = geneSetFacetName;
    }

  }

}
