package org.icgc.dcc.portal.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.icgc.dcc.portal.analysis.EnrichmentAnalyses.calculateGeneCountPValue;
import lombok.val;

import org.junit.Test;

public class EnrichmentAnalysesTest {

  @Test
    public void testCalculateGeneCountPValue() throws Exception {
      int q = 110; // geneSetOverlapGeneCount
      int k = 180; // overlapGeneCount
      int m = 3652; // geneSetGeneCount
      int n = 21804; // universeGeneCount
  
      val pValue = calculateGeneCountPValue(q, k, m, n);
      assertThat(pValue).isCloseTo(2.220446e-16, offset(0.0001));
    }

}
