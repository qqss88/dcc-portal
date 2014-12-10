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
package org.icgc.dcc.portal.enrichment;

import static com.google.common.base.Stopwatch.createStarted;
import static org.icgc.dcc.common.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.adjustRawGeneSetResults;
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.calculateExpectedValue;
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.calculateHypergeometricTest;
import static org.icgc.dcc.portal.enrichment.EnrichmentQueries.geneSetOverlapQuery;
import static org.icgc.dcc.portal.enrichment.EnrichmentQueries.overlapQuery;
import static org.icgc.dcc.portal.enrichment.EnrichmentSearchResponses.getUniverseTermsFacet;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.model.Query.idField;
import static org.icgc.dcc.portal.service.TermsLookupService.TermLookupType.GENE_IDS;
import static org.icgc.dcc.portal.util.Facets.getFacetCounts;
import static org.icgc.dcc.portal.util.SearchResponses.getHitIds;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.AndQuery;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Overview;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Result;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.GeneSetRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.util.Facets.Count;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Technical specification for this feature may be found here:
 * 
 * https://wiki.oicr.on.ca/display/DCCSOFT/Data+Portal+-+Enrichment+Analysis+-+Technical+Specification
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EnrichmentAnalyzer {

  /**
   * Dependencies.
   */
  @NonNull
  private final TermsLookupService termLookupService;
  @NonNull
  private final EnrichmentAnalysisRepository analysisRepository;
  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final GeneSetRepository geneSetRepository;
  @NonNull
  private final DonorRepository donorRepository;
  @NonNull
  private final MutationRepository mutationRepository;

  /**
   * This method runs asynchronously to perform enrichment analysis.
   * 
   * @param analysis the definition
   */
  @Async
  public void analyze(@NonNull EnrichmentAnalysis analysis) {
    val watch = createStarted();
    log.info("Executing analysis for {}...", analysis);

    // Shorthands
    val query = analysis.getQuery();
    val params = analysis.getParams();
    val universe = params.getUniverse();
    val inputGeneListId = analysis.getId();

    // Determine "InputGeneList"
    log.info("Finding input gene list @ {}...", watch);
    val inputGeneList = findInputGeneList(query, params.getMaxGeneCount());

    // Save ids in index for efficient search using "term lookup"
    log.info("Indexing input gene list ({} genes) @ {}...", formatCount(inputGeneList.size()), watch);
    indexInputGeneList(inputGeneListId, inputGeneList);

    // Get all gene-set gene counts of the input query
    log.info("Calculating overlap gene set counts @ {}...", watch);
    val overlapGeneSetCounts = findOverlapGeneSetCounts(
        query,
        universe,
        inputGeneListId);

    // Overview section
    log.info("Calculating overview @ {}...", watch);
    val overview = calculateOverview(query, universe, inputGeneListId);

    // Perform gene-set specific calculations
    log.info("Calculating raw gene set results @ {}...", watch);
    val rawResults = calculateRawGeneSetsResults(
        query,
        universe,
        inputGeneListId,

        overlapGeneSetCounts,
        overview.getOverlapGeneCount(),
        overview.getUniverseGeneCount());

    // Unfiltered gene-set count
    overview.setOverlapGeneSetCount(rawResults.size());

    // Statistical adjustment
    log.info("Adjusting raw gene set results @ {}...", watch);
    val adjustedResults = adjustRawGeneSetResults(params.getFdr(), rawResults);

    // Keep only the number of results that the user requested
    val limitedAdjustedResults = limitGeneSetResults(adjustedResults, params.getMaxGeneSetCount());

    log.info("Calculating final gene set results @ {}...", watch);
    calculateFinalGeneSetResults(query, universe, inputGeneListId, limitedAdjustedResults);

    // Update state for UI polling
    analysis.setOverview(overview);
    analysis.setResults(limitedAdjustedResults);
    analysis.setState(FINISHED);

    log.info("Updating analysis @ {} ...", watch);
    analysisRepository.update(analysis);

    log.info("Finished analyzing in {}", watch);
  }

  private Overview calculateOverview(Query query, Universe universe, UUID inputGeneListId) {
    return new Overview()
        .setOverlapGeneCount(countGenes(overlapQuery(query, universe, inputGeneListId)))
        .setUniverseGeneCount(countUniverseGenes(universe))
        .setUniverseGeneSetCount(countUniverseGeneSets(universe));
  }

  private List<Result> calculateRawGeneSetsResults(Query query, Universe universe, UUID inputGeneListId,
      List<Count> overlapGeneSetGeneCounts, int overlapGeneCount, int universeGeneCount) {
    val rawResults = Lists.<Result> newArrayList();
    for (int i = 0; i < overlapGeneSetGeneCounts.size(); i++) {
      val overlapGeneSet = overlapGeneSetGeneCounts.get(i);
      val geneSetId = overlapGeneSet.getId();
      int geneSetOverlapGeneCount = overlapGeneSet.getValue();

      log.info("[{}/{}] Processing {}", new Object[] { i + 1, overlapGeneSetGeneCounts.size(), geneSetId });
      if (geneSetId.equals(universe.getGeneSetId())) {
        // T6: Skip universe as this will trivially be most enriched by definition
        log.info("Skipping universe gene set: {}", geneSetId);
        continue;
      }

      val rawResult = calculateRawGeneSetResult(
          query,
          universe,
          inputGeneListId,
          geneSetId,

          // Formula inputs
          geneSetOverlapGeneCount,
          overlapGeneCount,
          universeGeneCount
          );

      // Add result for the current gene-set
      rawResults.add(rawResult);
    }

    return rawResults;
  }

  private Result calculateRawGeneSetResult(Query query, Universe universe, UUID inputGeneListId, String geneSetId,
      int geneSetOverlapGeneCount, int overlapGeneCount, int universeGeneCount) {
    val geneSetGeneCount = countGeneSetGenes(geneSetId);

    // Statistics
    val expectedValue = calculateExpectedValue(
        overlapGeneCount,
        geneSetGeneCount, universeGeneCount);
    val pValue = calculateHypergeometricTest(
        geneSetOverlapGeneCount, overlapGeneCount, // The "four numbers"
        geneSetGeneCount, universeGeneCount);

    log.debug("q = {}, k = {}, m = {}, n = {}, pValue = {}", new Object[] { geneSetOverlapGeneCount, overlapGeneCount,
        geneSetGeneCount, universeGeneCount, pValue });

    // Assemble
    return new Result()
        .setGeneSetId(geneSetId)

        .setGeneCount(geneSetGeneCount)
        .setOverlapGeneSetGeneCount(geneSetOverlapGeneCount)

        .setExpectedValue(expectedValue)
        .setPValue(pValue);
  }

  private void calculateFinalGeneSetResults(Query query, Universe universe, UUID inputGeneListId,
      List<Result> limitedAdjustedResults) {
    for (int i = 0; i < limitedAdjustedResults.size(); i++) {
      val geneSetResult = limitedAdjustedResults.get(i);
      val geneSetId = geneSetResult.getGeneSetId();

      log.info("[{}/{}] Post-processing {}", new Object[] { i + 1, limitedAdjustedResults.size(), geneSetId });
      val geneSetOverlapQuery = geneSetOverlapQuery(query, universe, inputGeneListId, geneSetId);

      // Update
      geneSetResult
          .setGeneSetName(findGeneSetName(geneSetId))
          .setOverlapGeneSetDonorCount(countDonors(geneSetOverlapQuery))
          .setOverlapGeneSetMutationCount(countMutations(geneSetOverlapQuery));
    }
  }

  private List<String> findInputGeneList(Query query, int maxGeneCount) {
    val limitedGeneQuery = Query.builder()
        .fields(idField())
        .filters(query.getFilters())
        .sort(query.getSort())
        .order(query.getOrder().toString())

        // This is non standard in terms of size of result set, but its just ids
        .size(maxGeneCount)
        .limit(maxGeneCount)
        .build();

    return getHitIds(geneRepository.findAllCentric(limitedGeneQuery));
  }

  private List<Count> findOverlapGeneSetCounts(Query query, Universe universe, UUID inputGeneListId) {
    val overlapQuery = overlapQuery(query, universe, inputGeneListId);
    val response = geneRepository.findGeneSetCounts(overlapQuery);
    val geneSetFacet = getUniverseTermsFacet(response, universe);

    return getFacetCounts(geneSetFacet);
  }

  private String findGeneSetName(String geneSetId) {
    return geneSetRepository.findName(geneSetId);
  }

  private int countGenes(AndQuery query) {
    return (int) geneRepository.countIntersection(query);
  }

  private int countDonors(AndQuery query) {
    return (int) donorRepository.countIntersection(query);
  }

  private int countMutations(AndQuery query) {
    return (int) mutationRepository.countIntersection(query);
  }

  private int countGeneSetGenes(String geneSetId) {
    return geneSetRepository.countGenes(geneSetId);
  }

  private int countUniverseGenes(Universe universe) {
    if (universe.isGo()) {
      return countGeneSetGenes(universe.getGeneSetId());
    } else {
      return (int) geneRepository.count(Query.builder().filters(universe.getFilter()).build());
    }
  }

  private int countUniverseGeneSets(Universe universe) {
    return geneSetRepository.countDecendants(universe.getGeneSetType(), Optional.fromNullable(universe.getGeneSetId()));
  }

  @SneakyThrows
  private void indexInputGeneList(UUID inputGeneListId, List<String> inputGeneList) {
    termLookupService.createTermsLookup(GENE_IDS, inputGeneListId, inputGeneList);
  }

  private static List<Result> limitGeneSetResults(List<Result> results, int maxGeneSetCount) {
    return results.size() < maxGeneSetCount ? results : results.subList(0, maxGeneSetCount);
  }

}
