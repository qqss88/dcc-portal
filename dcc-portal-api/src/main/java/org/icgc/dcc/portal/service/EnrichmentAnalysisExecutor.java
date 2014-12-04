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

import static org.icgc.dcc.portal.model.EnrichmentAnalysis.GeneSetType.GO_TERM;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.service.TermsLookupService.TermLookupType.GENE_IDS;
import static org.icgc.dcc.portal.util.EnrichmentAnalyses.adjustRawGeneSetResults;
import static org.icgc.dcc.portal.util.EnrichmentAnalyses.calculateExpectedValue;
import static org.icgc.dcc.portal.util.EnrichmentAnalyses.calculateHypergeometricTest;
import static org.icgc.dcc.portal.util.Filters.andFilter;
import static org.icgc.dcc.portal.util.Filters.enrichmentAnalysisFilter;
import static org.icgc.dcc.portal.util.Filters.geneSetFilter;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.search.facet.terms.TermsFacet;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.GeneSetType;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Result;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Summary;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Universe;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.GeneSetRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
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
public class EnrichmentAnalysisExecutor {

  /**
   * Dependencies
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
  public void execute(@NonNull EnrichmentAnalysis analysis) {
    val watch = Stopwatch.createStarted();
    log.info("Starting analysis for {}...", analysis);

    // Shorthands
    val query = analysis.getQuery();
    val params = analysis.getParams();
    val universe = params.getUniverse();
    val inputGeneListId = analysis.getId();

    // Determine input gene ids
    val inputGeneIds = calculateInputGeneIds(query, params.getMaxGeneCount());

    // Save ids in index for efficient search (see next) using "term lookup"
    indexInputGeneIds(inputGeneListId, inputGeneIds);

    // Get all gene-set gene counts of the input query
    val geneSetCounts = calculateGeneSetCounts(
        query,
        params.getUniverse(),
        analysis.getId());

    // Overview section
    val summary = calculateGeneSetSummary(query, universe, inputGeneListId);

    // Perform gene-set specific calculations
    val rawResults = calculateRawGeneSetsResults(
        query,
        params.getUniverse(),
        inputGeneListId,
        geneSetCounts,
        summary.getIntersectionGeneCount(),
        summary.getUniverseGeneCount());

    // Unfiltered gene-set count
    summary.setIntersectionGeneSetCount(rawResults.size());

    // Statistical adjustment
    val adjustedResults = adjustRawGeneSetResults(params.getFdr(), rawResults);

    // Keep only the number of results that the user requested
    val limitedAdjustedResults = adjustedResults.subList(0, params.getMaxGeneSetCount());

    calculateFinalGeneSetResults(query, universe, inputGeneListId, limitedAdjustedResults);

    // Update state for UI polling
    analysis.setSummary(summary);
    analysis.setResults(limitedAdjustedResults);
    analysis.setState(FINISHED);

    log.info("Saving analysis...");
    repository.save(analysis);
    log.info("Finished executing in {}", watch);
  }

  private List<String> calculateInputGeneIds(Query query, int maxGeneCount) {
    // Determine input gene ids
    val filters = query.getFilters();
    val fields = ImmutableList.<String> of("_id");
    val limitQuery = Query.builder()
        .fields(fields)
        .filters(filters)
        .size(maxGeneCount)
        .sort(query.getSort())
        .order(query.getOrder().toString())
        .build();

    val results = geneRepository.findAll(limitQuery);

    // Pluck gene ids
    val inputGeneIds = Lists.<String> newArrayList();
    for (val hit : results.getHits()) {
      inputGeneIds.add(hit.getId());
    }

    return inputGeneIds;
  }

  private Summary calculateGeneSetSummary(Query query, Universe universe, UUID inputGeneListId) {
    val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, false);

    val summary = new Summary()
        .setIntersectionGeneCount((int) geneRepository.count(intersectionQuery));

    // TODO: Calculate summary.universeGeneSetCount

    if (universe.getGeneSetType() == GO_TERM) {
      // TODO:
      val GENE_COUNT_FIELD_NAME = "geneCount";
      val geneSet = geneSetRepository.findOne(universe.getGeneSetId(), GENE_COUNT_FIELD_NAME);
      val universeGeneCount = (Long) geneSet.get("_summary._gene_count");

      summary.setUniverseGeneCount(universeGeneCount.intValue());
    } else {
      val universeGeneCount = (int) geneRepository.count(Query.builder().filters(universe.getFilter()).build());

      summary.setUniverseGeneCount(universeGeneCount);
    }

    return summary;
  }

  private List<GeneSetCount> calculateGeneSetCounts(Query query, Universe universe, UUID inputGeneListId) {
    val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, true);

    log.info("Finding gene sets...");
    val response = geneRepository.findGeneSets(intersectionQuery.getFilters());

    val geneSetCounts = ImmutableList.<GeneSetCount> builder();
    val geneSetFacet = (TermsFacet) response.getFacets().getFacets().get(universe.getGeneSetFacetName());
    for (val entry : geneSetFacet.getEntries()) {
      val geneSetId = entry.getTerm().string();
      val count = entry.getCount();

      geneSetCounts.add(new GeneSetCount(geneSetId, count));
    }

    return geneSetCounts.build();
  }

  private List<Result> calculateRawGeneSetsResults(Query query, Universe universe, UUID inputGeneListId,
      List<GeneSetCount> geneSetGeneCounts, int intersectionGeneCount, int universeGeneCount) {
    val rawResults = Lists.<Result> newArrayList();
    for (int i = 0; i < geneSetGeneCounts.size(); i++) {
      val geneSetCount = geneSetGeneCounts.get(i);

      log.info("[{}/{}] Processing {}", new Object[] { i + 1, geneSetGeneCounts.size(), geneSetCount.getGeneSetId() });
      val rawResult = calculateRawGeneSetResult(
          query,
          universe,

          geneSetCount.getGeneSetId(),
          universe.getGeneSetType(),
          inputGeneListId,

          // Formula inputs
          geneSetCount.getCount(),
          intersectionGeneCount,
          universeGeneCount
          );

      // Add result for the current gene-set
      rawResults.add(rawResult);
    }

    return rawResults;
  }

  private Result calculateRawGeneSetResult(Query query, Universe universe, String geneSetId, GeneSetType geneSetType,
      UUID inputGeneListId, int geneSetGeneCount, int intersectionGeneCount, int universeGeneCount) {
    val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, false);
    val geneSetIntersectionQuery = resolveGeneSetIntersectionQuery(geneSetId, intersectionQuery);

    // "#Genes in overlap"
    val geneSetIntersectionGeneCount = (int) geneRepository.count(geneSetIntersectionQuery);

    // Statistics
    val expectedValue = calculateExpectedValue(
        intersectionGeneCount,
        geneSetGeneCount, universeGeneCount);
    val pValue = calculateHypergeometricTest(
        geneSetIntersectionGeneCount, intersectionGeneCount,
        geneSetGeneCount, universeGeneCount);

    // Assemble
    return new Result()
        .setGeneSetId(geneSetId)

        .setGeneCount(geneSetGeneCount)
        .setIntersectionGeneCount(intersectionGeneCount)

        .setExpectedValue(expectedValue)
        .setPValue(pValue);
  }

  private void calculateFinalGeneSetResults(Query query, Universe universe, UUID inputGeneListId,
      List<Result> limitedAdjustedResults) {
    for (int i = 0; i < limitedAdjustedResults.size(); i++) {
      val result = limitedAdjustedResults.get(i);

      log.info("[{}/{}] Post-processing {}",
          new Object[] { i + 1, limitedAdjustedResults.size(), result.getGeneSetId() });
      val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, false);
      val geneSetIntersectionQuery = resolveGeneSetIntersectionQuery(result.getGeneSetId(), intersectionQuery);

      // Update
      result
          .setGeneSetName(geneSetRepository.findOne(result.getGeneSetId(), "name").get("name").toString())

          // "#Donors affected"
          .setIntersectionDonorCount((int) donorRepository.count(geneSetIntersectionQuery))

          // "#Mutations"
          .setIntersectionMutationCount((int) mutationRepository.count(geneSetIntersectionQuery));
    }
  }

  @SneakyThrows
  private void indexInputGeneIds(UUID id, List<String> inputGeneIds) {
    termLookupService.createTermsLookup(GENE_IDS, id, inputGeneIds);
  }

  private static Query resolveGeneSetIntersectionQuery(String geneSetId, Query intersectionQuery) {
    // TODO: Do not mutate
    intersectionQuery.setFilters(andFilter(intersectionQuery.getFilters(), geneSetFilter(geneSetId)));
    return intersectionQuery;
  }

  private static Query resolveIntersectionQuery(Query query, Universe universe, UUID inputGeneListId, boolean facets) {
    // Components
    val queryFilter = query.getFilters();
    val universeFilter = universe.getFilter();
    val analysisFilter = enrichmentAnalysisFilter(inputGeneListId);

    // Intersection
    val filters = andFilter(queryFilter, universeFilter, analysisFilter);

    val includes = Lists.<String> newArrayList();
    if (facets) {
      includes.add("facets");
    }

    // TODO: Remove the need for this
    val fields = ImmutableList.of("_id");
    return Query.builder().filters(filters).fields(fields).includes(includes).build();
  }

  @Value
  public static class GeneSetCount {

    String geneSetId;
    int count;

  }

}
