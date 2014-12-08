/*
 * Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.repository;

import lombok.val;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Test that verifies the content and structure of requests sent by the {@link GeneRepository} implementation.
 */
public class GeneRepositoryRequestTest extends BaseRepositoryRequestTest {

  private static final String DONOR_TERMS = donorTerms(GeneRepository.PREFIX_MAPPING);

  private static final String GENE_TERMS = geneTerms(GeneRepository.PREFIX_MAPPING);

  private static final String MUTATION_TERMS = mutationTerms(GeneRepository.PREFIX_MAPPING);

  private static final String CONSEQUENCE_TERMS = consequenceTerms(GeneRepository.PREFIX_MAPPING);

  private static final String OBSERVATION_TERMS = observationTerms(GeneRepository.PREFIX_MAPPING);

  private static final String PATH_DONOR = "}},\"path\":\"donor\"}}";

  private static final String PATH_DONOR_SSM = "}},\"path\":\"donor.ssm\"}}";

  private static final String PATH_DONOR_SSM_CONSEQUENCE = ",\"path\":\"donor.ssm.consequence\"}}";

  private static final String PATH_DONOR_SSM_OBSERVATION = "}},\"path\":\"donor.ssm.observation\"}}";

  private static final Type CENTRIC_TYPE = Type.GENE_CENTRIC;

  private static final String SORT = "affectedDonorCountFiltered";

  private static final String GENE_REQUEST = BOOL_MUST_R + GENE_TERMS;

  private static final String CONSEQUENCE_REQUEST_EMBED_EMBED = NESTED_FILTER_BOOL_MUST_R
      + "[" + CONSEQUENCE_TERMS + PATH_DONOR_SSM_CONSEQUENCE;

  private static final String CONSEQUENCE_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_REQUEST_EMBED_EMBED
      + PATH_DONOR_SSM;

  private static final String CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_REQUEST_EMBED
      + PATH_DONOR;

  private static final String MUTATION_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_DONOR_SSM_OBSERVATION
      + "]" + PATH_DONOR_SSM;

  private static final String MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + MUTATION_REQUEST_EMBED
      + PATH_DONOR;

  private static final String MUTATION_CONSEQUENCE_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + CONSEQUENCE_REQUEST_EMBED_EMBED + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_DONOR_SSM_OBSERVATION
      + "]" + PATH_DONOR_SSM;

  private static final String MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + MUTATION_CONSEQUENCE_REQUEST_EMBED
      + PATH_DONOR;

  private static final String DONOR_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST_R
      + DONOR_TERMS
      + PATH_DONOR;

  private static final String DONOR_MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + DONOR_TERMS + ","
      + MUTATION_REQUEST_EMBED
      + "]" + PATH_DONOR;

  private static final String DONOR_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + DONOR_TERMS + ","
      + CONSEQUENCE_REQUEST_EMBED
      + "]" + PATH_DONOR;

  private static final String DONOR_MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + DONOR_TERMS + ","
      + MUTATION_CONSEQUENCE_REQUEST_EMBED
      + "]" + PATH_DONOR;

  @InjectMocks
  GeneRepository repository;

  @Test
  public void testDonorFilter() {
    val fs = new String[] { DONOR_FILTER };
    val expected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  @Test
  public void testDonorGeneFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_FILTER };
    val expected = "[" + GENE_REQUEST + "," + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGeneMutationFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_FILTER, MUTATION_FILTER };
    val expected = "[" + GENE_REQUEST + "," + DONOR_MUTATION_REQUEST + "]";
    val scoreExpected = DONOR_MUTATION_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGeneConsequenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + GENE_REQUEST + "," + DONOR_CONSEQUENCE_REQUEST + "]";
    val scoreExpected = DONOR_CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGeneMutationConsequenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + GENE_REQUEST + "," + DONOR_MUTATION_CONSEQUENCE_REQUEST + "]";
    val scoreExpected = DONOR_MUTATION_CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testGeneFilter() {
    val fs = new String[] { GENE_FILTER };
    val expected = GENE_REQUEST;
    val scoreExpected = "{\"nested\":{\"filter\":{\"match_all\":{}},\"path\":\"donor\"}}";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testGeneMutationFilter() {
    val fs = new String[] { GENE_FILTER, MUTATION_FILTER };
    val expected = "[" + GENE_REQUEST + "," + MUTATION_REQUEST + "]";
    val scoreExpected = MUTATION_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testGeneConsequenceFilter() {
    val fs = new String[] { GENE_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + GENE_REQUEST + "," + CONSEQUENCE_REQUEST + "]";
    val scoreExpected = CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testGeneMutationConsequenceFilter() {
    val fs = new String[] { GENE_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + GENE_REQUEST + "," + MUTATION_CONSEQUENCE_REQUEST + "]";
    val scoreExpected = MUTATION_CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testMutationFilter() {
    val fs = new String[] { MUTATION_FILTER };
    val expected = MUTATION_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  @Test
  public void testConsequenceFilter() {
    val fs = new String[] { CONSEQUENCE_FILTER };
    val expected = CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  @Test
  public void testMutationConsequenceFilter() {
    val fs = new String[] { MUTATION_CONSEQUENCE_FILTER };
    val expected = MUTATION_CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  private ObjectNode findAllSetup(String... filters) {
    val filter = new FiltersParam(joinFilters(filters));
    val query = query()
        .filters(filter.get())
        .fields(Lists.<String> newArrayList(""))
        .sort(SORT)
        .build();

    val request = $(repository.buildFindAllRequest(query, CENTRIC_TYPE));
    log(filter.get(), request);
    return request;
  }

  private ObjectNode countSetup(String... filters) {
    val filter = new FiltersParam(joinFilters(filters));
    val query = query()
        .filters(filter.get())
        .sort(SORT)
        .build();

    val request = $(repository.buildCountRequest(query, CENTRIC_TYPE));
    log(filter.get(), request);
    return request;
  }

  private ObjectNode scoreSetup(String... filters) {
    val filter = new FiltersParam(joinFilters(filters));
    val query = query()
        .filters(filter.get())
        .build();

    val request = $(repository.buildScoreFilters(query.getFilters()));
    log(filter.get(), request);
    return request;
  }

  private void assertScore(JsonNode jn, String expected) {
    val s = "{\"nested\":{\"filter\":" + jn.toString() + ",\"path\":\"donor\"}}";
    assertScore(s, expected);
  }
}
