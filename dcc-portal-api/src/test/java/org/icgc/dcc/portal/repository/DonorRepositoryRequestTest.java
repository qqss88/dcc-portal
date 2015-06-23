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

import org.junit.Ignore;

/**
 * Test that verifies the content and structure of requests sent by the {@link DonorRepository} implementation.
 */
@Ignore("Too complex and error prone. Needs to rework validation")
public class DonorRepositoryRequestTest extends BaseRepositoryRequestTest {
  /*
   * 
   * private static final String DONOR_TERMS = donorTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String GENE_TERMS = geneTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String PATHWAY_TERMS = pathwayTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String MUTATION_TERMS = mutationTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String CONSEQUENCE_TERMS = consequenceTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String OBSERVATION_TERMS = observationTerms(DonorRepository.PREFIX_MAPPING);
   * 
   * private static final String PATH_GENE = "}},\"path\":\"gene\"}}";
   * 
   * private static final String PATH_GENE_SSM = "}},\"path\":\"gene.ssm\"}}";
   * 
   * private static final String PATH_GENE_SSM_OBSERVATION = "}},\"path\":\"gene.ssm.observation\"}}";
   * 
   * private static final String PATH_GENE_SSM_CONSEQUENCE = ",\"path\":\"gene.ssm.consequence\"}}";
   * 
   * private static final String PATH_GENE_PATHWAYS = "}},\"path\":\"gene.pathways\"}}";
   * 
   * private static final Type TYPE = Type.DONOR_CENTRIC;
   * 
   * private static final String SORT = "ssmAffectedGenes";
   * 
   * private static final String DONOR_REQUEST = BOOL_MUST_R + DONOR_TERMS;
   * 
   * private static final String CONSEQUENCE_REQUEST_EMBED_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + CONSEQUENCE_TERMS +
   * PATH_GENE_SSM_CONSEQUENCE;
   * 
   * private static final String CONSEQUENCE_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + CONSEQUENCE_REQUEST_EMBED_EMBED
   * + PATH_GENE_SSM;
   * 
   * private static final String CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + CONSEQUENCE_REQUEST_EMBED +
   * PATH_GENE;
   * 
   * private static final String MUTATION_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST + MUTATION_TERMS +
   * "," + NESTED_FILTER_BOOL_MUST_R + "[" + OBSERVATION_TERMS + "]" + PATH_GENE_SSM_OBSERVATION + "]" + PATH_GENE_SSM;
   * 
   * private static final String MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + MUTATION_REQUEST_EMBED + PATH_GENE;
   * 
   * private static final String MUTATION_CONSEQUENCE_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST +
   * MUTATION_TERMS + "," + CONSEQUENCE_REQUEST_EMBED_EMBED + "," + NESTED_FILTER_BOOL_MUST_R + "[" + OBSERVATION_TERMS
   * + "]" + PATH_GENE_SSM_OBSERVATION + "]" + PATH_GENE_SSM;
   * 
   * private static final String MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R +
   * MUTATION_CONSEQUENCE_REQUEST_EMBED + PATH_GENE;
   * 
   * private static final String GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST_R + GENE_TERMS + PATH_GENE;
   * 
   * private static final String GENE_MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R + GENE_TERMS +
   * "," + MUTATION_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R + GENE_TERMS +
   * "," + CONSEQUENCE_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R +
   * GENE_TERMS + "," + MUTATION_CONSEQUENCE_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String PATHWAY_REQUEST_EMBED = NESTED_FILTER_BOOL_MUST_R + PATHWAY_TERMS + PATH_GENE_PATHWAYS;
   * 
   * private static final String PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + PATHWAY_REQUEST_EMBED + PATH_GENE;
   * 
   * private static final String PATHWAY_MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + PATHWAY_REQUEST_EMBED +
   * "," + MUTATION_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String PATHWAY_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + PATHWAY_REQUEST_EMBED +
   * "," + CONSEQUENCE_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String PATHWAY_MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" +
   * PATHWAY_REQUEST_EMBED + "," + MUTATION_CONSEQUENCE_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R + GENE_TERMS + ","
   * + PATHWAY_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_PATHWAY_MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R +
   * GENE_TERMS + "," + PATHWAY_REQUEST_EMBED + "," + MUTATION_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_PATHWAY_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R +
   * GENE_TERMS + "," + PATHWAY_REQUEST_EMBED + "," + CONSEQUENCE_REQUEST_EMBED + "]" + PATH_GENE;
   * 
   * private static final String GENE_PATHWAY_MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" +
   * BOOL_MUST_R + GENE_TERMS + "," + PATHWAY_REQUEST_EMBED + "," + MUTATION_CONSEQUENCE_REQUEST_EMBED + "]" +
   * PATH_GENE;
   * 
   * @InjectMocks DonorRepository repository;
   * 
   * @Test public void testDonorFilter() { val fs = new String[] { DONOR_FILTER }; val expected = DONOR_REQUEST; val
   * scoreExpected = "{\"nested\":{\"filter\":{\"match_all\":{}},\"path\":\"gene\"}}";
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGeneFilter() { val fs = new String[] { DONOR_FILTER, GENE_FILTER }; val expected = "[" +
   * DONOR_REQUEST + "," + GENE_REQUEST + "]"; val scoreExpected = GENE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGeneMutationFilter() { val fs = new String[] { DONOR_FILTER, GENE_FILTER,
   * MUTATION_FILTER }; val expected = "[" + DONOR_REQUEST + "," + GENE_MUTATION_REQUEST + "]"; val scoreExpected =
   * GENE_MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGeneConsequenceFilter() { val fs = new String[] { DONOR_FILTER, GENE_FILTER,
   * CONSEQUENCE_FILTER }; val expected = "[" + DONOR_REQUEST + "," + GENE_CONSEQUENCE_REQUEST + "]"; val scoreExpected
   * = GENE_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGeneMutationConsequenceFilter() { val fs = new String[] { DONOR_FILTER, GENE_FILTER,
   * MUTATION_CONSEQUENCE_FILTER }; val expected = "[" + DONOR_REQUEST + "," + GENE_MUTATION_CONSEQUENCE_REQUEST + "]";
   * val scoreExpected = GENE_MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGenePathwayFilter() { val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER }; val
   * expected = "[" + DONOR_REQUEST + "," + GENE_PATHWAY_REQUEST + "]"; val scoreExpected = GENE_PATHWAY_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGenePathwayMutationFilter() { val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER,
   * MUTATION_FILTER }; val expected = "[" + DONOR_REQUEST + "," + GENE_PATHWAY_MUTATION_REQUEST + "]"; val
   * scoreExpected = GENE_PATHWAY_MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGenePathwayConsequenceFilter() { val fs = new String[] { DONOR_FILTER,
   * GENE_PATHWAY_FILTER, CONSEQUENCE_FILTER }; val expected = "[" + DONOR_REQUEST + "," +
   * GENE_PATHWAY_CONSEQUENCE_REQUEST + "]"; val scoreExpected = GENE_PATHWAY_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testDonorGenePathwayMutationConsequenceFilter() { val fs = new String[] { DONOR_FILTER,
   * GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_FILTER }; val expected = "[" + DONOR_REQUEST + "," +
   * GENE_PATHWAY_MUTATION_CONSEQUENCE_REQUEST + "]"; val scoreExpected = GENE_PATHWAY_MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * scoreExpected); }
   * 
   * @Test public void testGenePathwayFilter() { val fs = new String[] { GENE_PATHWAY_FILTER }; val expected =
   * GENE_PATHWAY_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGenePathwayMutationFilter() { val fs = new String[] { GENE_PATHWAY_FILTER, MUTATION_FILTER };
   * val expected = GENE_PATHWAY_MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGenePathwayMutationConsequenceFilter() { val fs = new String[] { GENE_PATHWAY_FILTER,
   * MUTATION_CONSEQUENCE_FILTER }; val expected = GENE_PATHWAY_MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGenePathwayConsequenceFilter() { val fs = new String[] { GENE_PATHWAY_FILTER,
   * CONSEQUENCE_FILTER }; val expected = GENE_PATHWAY_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testPathwayFilter() { val fs = new String[] { PATHWAY_FILTER }; val expected = PATHWAY_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testPathwayMutationFilter() { val fs = new String[] { PATHWAY_FILTER, MUTATION_FILTER }; val
   * expected = PATHWAY_MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testPathwayMutationConsequenceFilter() { val fs = new String[] { PATHWAY_FILTER,
   * MUTATION_CONSEQUENCE_FILTER }; val expected = PATHWAY_MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testPathwayConsequenceFilter() { val fs = new String[] { PATHWAY_FILTER, CONSEQUENCE_FILTER };
   * val expected = PATHWAY_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGeneFilter() { val fs = new String[] { GENE_FILTER }; val expected = GENE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGeneMutationFilter() { val fs = new String[] { GENE_FILTER, MUTATION_FILTER }; val expected =
   * GENE_MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGeneConsequenceFilter() { val fs = new String[] { GENE_FILTER, CONSEQUENCE_FILTER }; val
   * expected = GENE_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testGeneMutationConsequenceFilter() { val fs = new String[] { GENE_FILTER,
   * MUTATION_CONSEQUENCE_FILTER }; val expected = GENE_MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testMutationFilter() { val fs = new String[] { MUTATION_FILTER }; val expected =
   * MUTATION_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testConsequenceFilter() { val fs = new String[] { CONSEQUENCE_FILTER }; val expected =
   * CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * @Test public void testMutationConsequenceFilter() { val fs = new String[] { MUTATION_CONSEQUENCE_FILTER }; val
   * expected = MUTATION_CONSEQUENCE_REQUEST;
   * 
   * assertFilter(findAllSetup(fs), expected); assertFilter(countSetup(fs), expected); assertScore(scoreSetup(fs),
   * expected); }
   * 
   * private ObjectNode findAllSetup(String... filters) { val filter = new FiltersParam(joinFilters(filters)); val query
   * = query() .filters(filter.get()) .fields(Lists.<String> newArrayList("")) .sort(SORT) .build();
   * 
   * val request = $(repository.buildFindAllRequest(query, TYPE)); log(filter.get(), request); return request; }
   * 
   * private ObjectNode countSetup(String... filters) { val filter = new FiltersParam(joinFilters(filters)); val query =
   * query() .filters(filter.get()) .sort(SORT) .build();
   * 
   * val request = $(repository.buildCountRequest(query, TYPE)); log(filter.get(), request); return request; }
   * 
   * private ObjectNode scoreSetup(String... filters) { val filter = new FiltersParam(joinFilters(filters)); val query =
   * query() .filters(filter.get()) .build();
   * 
   * val request = $(repository.buildScoreFilters(query)); log(filter.get(), request); return request; }
   * 
   * private void assertScore(JsonNode jn, String expected) { val s = "{\"nested\":{\"filter\":" + jn.toString() +
   * ",\"path\":\"gene\"}}"; assertScore(s, expected); }
   */

}
