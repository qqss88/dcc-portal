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

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.analysis.EnrichmentAnalyzer;
import org.icgc.dcc.portal.analysis.EnrichmentReporter;
import org.icgc.dcc.portal.model.BaseEntityList.Type;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EntityList;
import org.icgc.dcc.portal.model.EntityList.SubType;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EnrichmentAnalysisService {

  /**
   * Dependencies.
   */
  @NonNull
  private final EnrichmentAnalyzer analyzer;
  @NonNull
  private final EnrichmentReporter reporter;
  @NonNull
  private final EnrichmentAnalysisRepository repository;
  @NonNull
  private final EntityListRepository entityListRepository;

  @NonNull
  public EnrichmentAnalysis getAnalysis(@NonNull UUID analysisId) {
    val analysis = repository.find(analysisId);
    if (analysis == null) {
      throw new NotFoundException(analysisId.toString(), "enrichment analysis");
    }

    return analysis;
  }

  public void submitAnalysis(@NonNull EnrichmentAnalysis analysis) {
    val id = createAnalysisId();
    analysis.setId(id);

    // Ensure persisted for polling
    log.info("Saving analysis '{}'...", analysis.getId());

    // Save this as an entity list too in order to capture the subtype information.
    val newEntityList = EntityList.createForStatusFinished(id, "Input gene set", "", Type.GENE, 0);
    newEntityList.setSubtype(SubType.ENRICHMENT);
    entityListRepository.save(newEntityList);

    val insertCount = repository.save(analysis);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    // Execute asynchronously
    log.info("Executing analysis '{}'...", analysis.getId());
    analyzer.analyze(analysis.getId());
  }

  public void reportAnalysis(@NonNull EnrichmentAnalysis analysis, @NonNull OutputStream outputStream)
      throws IOException {
    val results = analysis.getResults();
    if (results == null) {
      log.info("No results to report for analysis id '{}'", analysis.getId());
      return;
    }

    reporter.report(analysis, outputStream);
  }

  private static UUID createAnalysisId() {
    // Prevent "browser scanning" by using an opaque id
    return UUID.randomUUID();
  }

}
