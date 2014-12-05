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

import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.model.Query.includeFacets;
import static org.icgc.dcc.portal.model.Query.noFields;
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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.GeneSetType;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Result;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Summary;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;
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
   * Constants.
   */
  private static final String API_GENE_COUNT_FIELD_NAME = "geneCount";
  private static final String INDEX_GENE_SETS_NAME_FIELD_NAME = "name";
  private static final String INDEX_GENE_COUNT_FIELD_NAME = "_summary._gene_count";

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
  public void execute(@NonNull EnrichmentAnalysis analysis) {
    val watch = Stopwatch.createStarted();
    log.info("Starting analysis for {}...", analysis);

    // Shorthands
    val query = analysis.getQuery();
    val params = analysis.getParams();
    val universe = params.getUniverse();
    val inputGeneListId = analysis.getId();

    // Determine input gene ids
    val inputGeneList = findInputGeneList(query, params.getMaxGeneCount());

    // Save ids in index for efficient search (see next) using "term lookup"
    indexInputGeneList(inputGeneListId, inputGeneList);

    // Get all gene-set gene counts of the input query
    val geneSetCounts = calculateGeneSetCounts(
        query,
        universe,
        inputGeneListId);

    // Overview section
    val summary = calculateGeneSetSummary(query, universe, inputGeneListId);

    // Perform gene-set specific calculations
    val rawResults = calculateRawGeneSetsResults(
        query,
        universe,
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

  private Summary calculateGeneSetSummary(Query query, Universe universe, UUID inputGeneListId) {
    val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, false);

    // Common
    val summary = new Summary()
        .setIntersectionGeneCount(countGenes(intersectionQuery));

    if (universe.isGo()) {
      // Gene set id based
      val universeGeneCount = countGeneSetGenes(universe.getGeneSetId());
      val universeGeneSetCount = 0; // TODO

      summary
          .setUniverseGeneCount(universeGeneCount)
          .setUniverseGeneSetCount(universeGeneSetCount);
    } else {
      // Gene set type based
      val universeGeneCount = countUniverseGenes(universe);
      val universeGeneSetCount = 0; // TODO

      summary
          .setUniverseGeneCount(universeGeneCount)
          .setUniverseGeneSetCount(universeGeneSetCount);
    }

    return summary;
  }

  private List<GeneSetCount> calculateGeneSetCounts(Query query, Universe universe, UUID inputGeneListId) {
    val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, true);

    log.info("Finding gene sets...");
    val response = geneRepository.findGeneSets(intersectionQuery.getFilters());

    // Facets represent the GO
    val geneSetFacet = resolveUniverseTermsFacet(universe, response);

    val geneSetCounts = ImmutableList.<GeneSetCount> builder();
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
      val getGeneSetId = geneSetCount.getGeneSetId();

      log.info("[{}/{}] Processing {}", new Object[] { i + 1, geneSetGeneCounts.size(), getGeneSetId });
      val rawResult = calculateRawGeneSetResult(
          query,
          universe,

          getGeneSetId,
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
    val geneSetIntersectionGeneCount = countGenes(geneSetIntersectionQuery);

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
      val geneSetResult = limitedAdjustedResults.get(i);
      val geneSetId = geneSetResult.getGeneSetId();

      log.info("[{}/{}] Post-processing {}", new Object[] {
          i + 1, limitedAdjustedResults.size(), geneSetId });

      val intersectionQuery = resolveIntersectionQuery(query, universe, inputGeneListId, false);
      val geneSetIntersectionQuery = resolveGeneSetIntersectionQuery(geneSetId, intersectionQuery);

      // Update
      geneSetResult
          .setGeneSetName(findGeneSetName(geneSetId))

          // "#Donors affected"
          .setIntersectionDonorCount(findDonorCount(geneSetIntersectionQuery))

          // "#Mutations"
          .setIntersectionMutationCount(findMutationCount(geneSetIntersectionQuery));
    }
  }

  private int findMutationCount(Query query) {
    return (int) mutationRepository.count(query);
  }

  private int findDonorCount(Query query) {
    return (int) donorRepository.count(query);
  }

  private List<String> findInputGeneList(Query query, int maxGeneCount) {
    // Determine input gene ids
    val filters = query.getFilters();
    val limitQuery = Query.builder()
        .fields(noFields())
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

  private static TermsFacet resolveUniverseTermsFacet(Universe universe, SearchResponse response) {
    return (TermsFacet) response.getFacets().getFacets().get(universe.getGeneSetFacetName());
  }

  private static Query resolveGeneSetIntersectionQuery(String geneSetId, Query intersectionQuery) {
    // TODO: Do not mutate, creat a "Queries" instead
    val overlapFilter = andFilter(intersectionQuery.getFilters(), geneSetFilter(geneSetId));
    intersectionQuery.setFilters(overlapFilter);

    return intersectionQuery;
  }

  private static Query resolveIntersectionQuery(Query query, Universe universe, UUID inputGeneListId, boolean facets) {
    // Components
    val queryFilter = query.getFilters();
    val universeFilter = universe.getFilter();
    val analysisFilter = enrichmentAnalysisFilter(inputGeneListId);

    // Intersection
    val filters = andFilter(queryFilter, universeFilter, analysisFilter);

    // Facets?
    val includes = facets ? includeFacets() : Lists.<String> newArrayList();

    return Query.builder().filters(filters).fields(noFields()).includes(includes).build();
  }

  @Value
  public static class GeneSetCount {

    String geneSetId;
    int count;

  }

}
