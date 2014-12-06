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
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.adjustRawGeneSetResults;
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.calculateExpectedValue;
import static org.icgc.dcc.portal.enrichment.EnrichmentAnalyses.calculateHypergeometricTest;
import static org.icgc.dcc.portal.enrichment.EnrichmentFacets.universeTermsFacet;
import static org.icgc.dcc.portal.enrichment.EnrichmentQueries.geneSetOverlapQuery;
import static org.icgc.dcc.portal.enrichment.EnrichmentQueries.overlapQuery;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.model.Query.idField;
import static org.icgc.dcc.portal.service.TermsLookupService.TermLookupType.GENE_IDS;
import static org.icgc.dcc.portal.util.SearchResponses.getHitIds;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
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
   * Constants.
   */
  private static final String API_GENE_COUNT_FIELD_NAME = "geneCount";
  private static final String INDEX_GENE_COUNT_FIELD_NAME = "_summary._gene_count";
  private static final String INDEX_GENE_SETS_NAME_FIELD_NAME = "name";

  /**
   * Dependencies.
   */
  @NonNull
  private final TermsLookupService termLookupService;
  @NonNull
  private final EnrichmentAnalysisRepository repository;
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

    // Determine input gene ids
    log.info("Finding input gene list @ {}...", watch);
    val inputGeneList = findInputGeneList(query, params.getMaxGeneCount());

    // Save ids in index for efficient search using "term lookup"
    log.info("Indexing input gene list @ {}...", watch);
    indexInputGeneList(inputGeneListId, inputGeneList);

    // Get all gene-set gene counts of the input query
    log.info("Calculating gene set counts @ {}...", watch);
    val geneSetCounts = calculateGeneSetCounts(
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

        geneSetCounts,
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
    repository.update(analysis);

    log.info("Finished analyzing in {}", watch);
  }

  private Overview calculateOverview(Query query, Universe universe, UUID inputGeneListId) {
    val overlapQuery = overlapQuery(query, universe, inputGeneListId);

    // Common
    val overview = new Overview()
        .setOverlapGeneCount(countGenes(overlapQuery));

    if (universe.isGo()) {
      // Gene set id based
      val universeGeneCount = countGeneSetGenes(universe.getGeneSetId());
      val universeGeneSetCount = 0; // FIXME: This needs to be calculated!!!

      overview
          .setUniverseGeneCount(universeGeneCount)
          .setUniverseGeneSetCount(universeGeneSetCount);
    } else {
      // Gene set type based
      val universeGeneCount = countUniverseGenes(universe);
      val universeGeneSetCount = 0; // FIXME: This needs to be calculated!!!

      overview
          .setUniverseGeneCount(universeGeneCount)
          .setUniverseGeneSetCount(universeGeneSetCount);
    }

    return overview;
  }

  private List<GeneSetCount> calculateGeneSetCounts(Query query, Universe universe, UUID inputGeneListId) {
    val overlapQuery = overlapQuery(query, universe, inputGeneListId);

    val response = geneRepository.findGeneSets(overlapQuery.getFilters());

    // EnrichmentFacets represent the GO
    val geneSetFacet = universeTermsFacet(response, universe);

    val geneSetCounts = ImmutableList.<GeneSetCount> builder();
    for (val entry : geneSetFacet.getEntries()) {
      val geneSetId = entry.getTerm().string();
      val count = entry.getCount();

      geneSetCounts.add(new GeneSetCount(geneSetId, count));
    }

    return geneSetCounts.build();
  }

  private List<Result> calculateRawGeneSetsResults(Query query, Universe universe, UUID inputGeneListId,
      List<GeneSetCount> geneSetGeneCounts, int overlapGeneCount, int universeGeneCount) {
    val rawResults = Lists.<Result> newArrayList();
    for (int i = 0; i < geneSetGeneCounts.size(); i++) {
      val geneSetCount = geneSetGeneCounts.get(i);
      val geneSetId = geneSetCount.getGeneSetId();

      log.info("[{}/{}] Processing {}", new Object[] { i + 1, geneSetGeneCounts.size(), geneSetId });
      val rawResult = calculateRawGeneSetResult(
          query,
          universe,
          inputGeneListId,
          geneSetId,

          // Formula inputs
          geneSetCount.getCount(),
          overlapGeneCount,
          universeGeneCount
          );

      // Add result for the current gene-set
      rawResults.add(rawResult);
    }

    return rawResults;
  }

  private Result calculateRawGeneSetResult(Query query, Universe universe, UUID inputGeneListId,
      String geneSetId, int geneSetGeneCount, int overlapGeneCount, int universeGeneCount) {
    val geneSetOverlapQuery = geneSetOverlapQuery(query, universe, inputGeneListId, geneSetId);

    // "#Genes in overlap"
    val geneSetOverlapGeneCount = countGenes(geneSetOverlapQuery);

    // Statistics
    val expectedValue = calculateExpectedValue(
        overlapGeneCount,
        geneSetGeneCount, universeGeneCount);
    val pValue = calculateHypergeometricTest(
        geneSetOverlapGeneCount, overlapGeneCount,
        geneSetGeneCount, universeGeneCount);

    // Assemble
    return new Result()
        .setGeneSetId(geneSetId)

        .setGeneCount(geneSetGeneCount)
        .setOverlapGeneCount(overlapGeneCount)

        .setExpectedValue(expectedValue)
        .setPValue(pValue);
  }

  private void calculateFinalGeneSetResults(Query query, Universe universe, UUID inputGeneListId,
      List<Result> limitedAdjustedResults) {
    for (int i = 0; i < limitedAdjustedResults.size(); i++) {
      val geneSetResult = limitedAdjustedResults.get(i);
      val geneSetId = geneSetResult.getGeneSetId();

      log.info("[{}/{}] Post-processing {}", new Object[] {
          i + 1, limitedAdjustedResults.size(), geneSetId });

      val geneSetOverlapQuery = geneSetOverlapQuery(query, universe, inputGeneListId, geneSetId);

      // Update
      geneSetResult
          .setGeneSetName(findGeneSetName(geneSetId))

          // "#Donors affected"
          .setOverlapDonorCount(findDonorCount(geneSetOverlapQuery))

          // "#Mutations"
          .setOverlapMutationCount(findMutationCount(geneSetOverlapQuery));
    }
  }

  private int findMutationCount(Query query) {
    return (int) mutationRepository.count(query);
  }

  private int findDonorCount(Query query) {
    return (int) donorRepository.count(query);
  }

  private List<String> findInputGeneList(Query query, int maxGeneCount) {
    val limitedGeneQuery = Query.builder()
        .fields(idField())
        .filters(query.getFilters())
        .size(maxGeneCount)
        .sort(query.getSort())
        .order(query.getOrder().toString())
        .build();

    val results = geneRepository.findAllCentric(limitedGeneQuery);

    return getHitIds(results);
  }

  private String findGeneSetName(String geneSetId) {
    val nameField = INDEX_GENE_SETS_NAME_FIELD_NAME;

    return geneSetRepository.findOne(geneSetId, nameField).get(nameField).toString();
  }

  private int countGenes(Query query) {
    return (int) geneRepository.count(query);
  }

  private int countUniverseGenes(Universe universe) {
    return (int) geneRepository.count(Query.builder().filters(universe.getFilter()).build());
  }

  private int countGeneSetGenes(String geneSetId) {
    val geneSet = geneSetRepository.findOne(geneSetId, API_GENE_COUNT_FIELD_NAME);

    return ((Long) geneSet.get(INDEX_GENE_COUNT_FIELD_NAME)).intValue();
  }

  @SneakyThrows
  private void indexInputGeneList(UUID inputGeneListId, List<String> inputGeneList) {
    termLookupService.createTermsLookup(GENE_IDS, inputGeneListId, inputGeneList);
  }

  private static List<Result> limitGeneSetResults(List<Result> results, int maxGeneSetCount) {
    return results.subList(0, maxGeneSetCount);
  }

  @Value
  static class GeneSetCount {

    String geneSetId;
    int count;

  }

}
