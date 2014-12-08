package org.icgc.dcc.portal.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.EXECUTING;
import static org.icgc.dcc.portal.model.Universe.GO_BIOLOGICAL_PROCESS;
import static org.icgc.dcc.portal.util.Filters.geneSetFilter;

import java.util.UUID;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Params;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.GeneSetRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.icgc.dcc.portal.service.TermsLookupService;
import org.icgc.dcc.portal.test.AbstractSpringIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
public class EnrichmentAnalyzerTest extends AbstractSpringIntegrationTest {

  /**
   * Dependencies.
   */
  @Autowired
  TermsLookupService termLookupService;
  @Autowired
  EnrichmentAnalysisRepository repository;
  @Autowired
  GeneRepository geneRepository;
  @Autowired
  GeneSetRepository geneSetRepository;
  @Autowired
  DonorRepository donorRepository;
  @Autowired
  MutationRepository mutationRepository;

  /**
   * Subject.
   */
  EnrichmentAnalyzer analyzer;

  @Before
  public void setUp() {
    this.analyzer = new EnrichmentAnalyzer(
        termLookupService,
        repository,
        geneRepository,
        geneSetRepository,
        donorRepository,
        mutationRepository);
  }

  @Test
  public void testAnalyze() {
    val inputFilter = geneSetFilter("GO:0050794");

    val analysis = execute(inputFilter);

    val overview = analysis.getOverview();
    assertThat(overview.getOverlapGeneCount()).isGreaterThan(0);
    assertThat(overview.getOverlapGeneSetCount()).isGreaterThan(0);
    assertThat(overview.getUniverseGeneCount()).isGreaterThan(0);
    assertThat(overview.getUniverseGeneSetCount()).isGreaterThan(0);
  }

  private EnrichmentAnalysis execute(ObjectNode inputFilter) {
    val analysis =
        new EnrichmentAnalysis()
            .setId(UUID.randomUUID())
            .setState(EXECUTING)
            .setParams(
                new Params()
                    .setUniverse(GO_BIOLOGICAL_PROCESS)
                    .setFdr(0.05f)
                    .setMaxGeneCount(1000)
                    .setMaxGeneSetCount(100))
            .setQuery(Query.builder()
                .filters(inputFilter)
                .sort("affectedDonorCountFiltered")
                .order("ASC")
                .build());

    log.info("Starting...");
    analyzer.analyze(analysis);

    log.info("Result: {}", analysis);
    return analysis;
  }

}
