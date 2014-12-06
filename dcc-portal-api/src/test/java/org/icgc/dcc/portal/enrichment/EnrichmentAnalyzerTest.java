package org.icgc.dcc.portal.enrichment;

import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.EXECUTING;
import static org.icgc.dcc.portal.model.Universe.GO_BIOLOGICAL_PROCESS;
import static org.icgc.dcc.portal.util.Filters.emptyFilter;

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
                .filters(emptyFilter())
                .sort("affectedDonorCountFiltered")
                .order("ASC")
                .build());

    log.info("Starting...");
    analyzer.analyze(analysis);
  }

}
