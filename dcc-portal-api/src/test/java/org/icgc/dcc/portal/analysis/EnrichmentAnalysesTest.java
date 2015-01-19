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

  @Test
  public void testCalculateGeneCountPValueNegative() throws Exception {
    int q = 266; // geneSetOverlapGeneCount
    int k = 271; // overlapGeneCount
    int m = 13863; // geneSetGeneCount
    int n = 17351; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    assertThat(pValue).isCloseTo(0.0, offset(0.0001)).isGreaterThanOrEqualTo(0.0);
  }

}
