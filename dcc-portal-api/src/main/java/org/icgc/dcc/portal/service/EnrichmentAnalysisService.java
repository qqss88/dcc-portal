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
package org.icgc.dcc.portal.service;

import static com.google.common.base.Preconditions.checkState;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.enrichment.EnrichmentAnalyzer;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvBeanWriter;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EnrichmentAnalysisService {

  /**
   * Constants.
   */
  private static final String[] REPORT_HEADERS =
      {
          "ID", "Name", "# Genes", "# Genes in overlap", "# Donors affected", "# Mutations", "Expected", "P-Value", "Adjusted P-Value"
      };
  private static final String[] REPORT_FIELD_NAMES =
      {
          "geneSetId", "geneSetName", "geneCount", "overlapGeneCount", "overlapDonorCount", "overlapMutationCount", "expectedValue", "pValue", "adjustedPValue"
      };

  /**
   * Dependencies.
   */
  @NonNull
  private final EnrichmentAnalyzer analyzer;
  @NonNull
  private final EnrichmentAnalysisRepository analysisRepository;

  public EnrichmentAnalysis getAnalysis(@NonNull UUID analysisId) {
    val analysis = analysisRepository.find(analysisId);
    if (analysis == null) {
      throw new NotFoundException("enrichment analysis", analysisId.toString());
    }

    return analysis;
  }

  public void submitAnalysis(@NonNull EnrichmentAnalysis analysis) {
    analysis.setId(createAnalysisId());

    // Ensure persisted for polling
    log.info("Saving analysis '{}'...", analysis.getId());
    val insertCount = analysisRepository.save(analysis);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    // Execute asynchronously
    log.info("Executing analysis '{}'...", analysis.getId());
    analyzer.analyze(analysis);
  }

  public void reportAnalysis(EnrichmentAnalysis analysis, OutputStream outputStream) throws IOException {
    val results = analysis.getResults();
    if (results == null) {
      log.info("No results to report for analysis id '{}'", analysis.getId());
      return;
    }

    @Cleanup
    val writer = new CsvBeanWriter(new OutputStreamWriter(outputStream), TAB_PREFERENCE);
    writer.writeHeader(REPORT_HEADERS);

    for (val result : results) {
      writer.write(result, REPORT_FIELD_NAMES);
    }
  }

  private static UUID createAnalysisId() {
    // Prevent "browser scanning" by using an opaque id
    return UUID.randomUUID();
  }

}
