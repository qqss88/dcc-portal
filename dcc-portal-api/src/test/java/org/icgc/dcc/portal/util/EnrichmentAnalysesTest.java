package org.icgc.dcc.portal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import lombok.val;

import org.icgc.dcc.portal.util.EnrichmentAnalyses;
import org.junit.Test;

public class EnrichmentAnalysesTest {

  @Test
  public void testCalculateHypergeometricTest() throws Exception {
    int q = 110;
    int k = 180;
    int m = 3652;
    int n = 21804;

    val pValue = EnrichmentAnalyses.calculateHypergeometricTest(q, k, m, n);
    assertThat(pValue).isCloseTo(2.220446e-16, offset(0.0001));
  }

}
