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
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Test that verifies the content and structure of requests sent by the {@link OccurrenceRepository} implementation.
 */
@Ignore("Too complex and error prone. Needs to rework validation")
public class OccurrenceRepositoryRequestTest extends BaseRepositoryRequestTest {

  protected static String DONOR_PROJECT_FILTER =
      "'donor':{'primarySite':{'is':['Brain']},'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}";

  protected static String DONOR_FILTER =
      "'donor':{'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}";

  private static final String DONOR_TERMS = "{\"terms\":{\""
      + getField("gender", Kind.DONOR, OccurrenceRepository.PREFIX_MAPPING) + "\":[\"male\"]}},"
      + "{\"terms\":{\"" + getField("availableDataTypes", Kind.DONOR, OccurrenceRepository.PREFIX_MAPPING)
      + "\":[\"cnsm\"]}}]}}";

  private static final String PROJECT_TERMS = "{\"terms\":{\""
      + getField("primarySite", Kind.PROJECT, OccurrenceRepository.PREFIX_MAPPING) + "\":[\"Brain\"]}}";

  private static final String GENE_TERMS = geneTerms(OccurrenceRepository.PREFIX_MAPPING);

  private static final String PATHWAY_TERMS = pathwayTerms(OccurrenceRepository.PREFIX_MAPPING);

  private static final String MUTATION_TERMS = mutationTerms(OccurrenceRepository.PREFIX_MAPPING);

  private static final String OBSERVATION_TERMS = observationTerms(OccurrenceRepository.PREFIX_MAPPING);

  private static final String CONSEQUENCE_TERMS = consequenceTerms(OccurrenceRepository.PREFIX_MAPPING);

  private static final String PATH_DONOR = ",\"path\":\"donor\"}}";

  private static final String PATH_PROJECT = "}},\"path\":\"project\"}}";

  private static final String PATH_SSM = "}},\"path\":\"ssm\"}}";

  private static final String PATH_OBSERVATION = "}},\"path\":\"ssm.observation\"}}";

  private static final String PATH_SSM_CONSEQUENCE = "\"path\":\"ssm.consequence\"}}";

  private static final String PATH_SSM_CONSEQUENCE_PATHWAY = "\"path\":\"ssm.consequence.gene.pathways\"}}";

  private static final Type CENTRIC_TYPE = Type.OCCURRENCE_CENTRIC;

  private static final String SORT = "donorId";

  private static final String DONOR_REQUEST = NESTED_FILTER_BOOL_MUST_R + "["
      + DONOR_TERMS
      + PATH_DONOR;

  private static final String PROJECT_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + PROJECT_TERMS
      + PATH_PROJECT;

  private static final String PATHWAY_EMBED_EMBED = NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS + "}},"
      + PATH_SSM_CONSEQUENCE_PATHWAY;

  private static final String PATHWAY_EMBED = NESTED_FILTER_BOOL_MUST_R + PATHWAY_EMBED_EMBED + "}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String GENE_EMBED = NESTED_FILTER_BOOL_MUST_R
      + BOOL_MUST_R + GENE_TERMS + "}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String GENE_PATHWAY_EMBED = NESTED_FILTER_BOOL_MUST_R + "["
      + BOOL_MUST_R + GENE_TERMS + ","
      + PATHWAY_EMBED_EMBED + "]}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + GENE_PATHWAY_EMBED + PATH_SSM;

  private static final String PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_EMBED + PATH_SSM;

  private static final String GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + GENE_EMBED + PATH_SSM;

  private static final String CONSEQUENCE_PATHWAY_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + CONSEQUENCE_TERMS + ","
      + PATHWAY_EMBED_EMBED + "]}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String CONSEQUENCE_GENE_PATHWAY_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + CONSEQUENCE_TERMS + ","
      + BOOL_MUST_R + GENE_TERMS + ","
      + PATHWAY_EMBED_EMBED + "]}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String CONSEQUENCE_GENE_EMBED = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + CONSEQUENCE_TERMS + ","
      + BOOL_MUST_R + GENE_TERMS + "]}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String CONSEQUENCE_EMBED = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST_R
      + CONSEQUENCE_TERMS + "}},"
      + PATH_SSM_CONSEQUENCE;

  private static final String CONSEQUENCE_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_GENE_PATHWAY_EMBED + PATH_SSM;

  private static final String CONSEQUENCE_GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_GENE_EMBED + PATH_SSM;

  private static final String CONSEQUENCE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_PATHWAY_EMBED + PATH_SSM;

  private static final String MUTATION_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + PATHWAY_EMBED + "]" + PATH_SSM;

  private static final String MUTATION_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + GENE_PATHWAY_EMBED + "]" + PATH_SSM;

  private static final String MUTATION_GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + GENE_EMBED + "]" + PATH_SSM;

  private static final String MUTATION_CONSEQUENCE_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + CONSEQUENCE_GENE_PATHWAY_EMBED
      + "]" + PATH_SSM;

  private static final String MUTATION_CONSEQUENCE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + CONSEQUENCE_PATHWAY_EMBED + "]" + PATH_SSM;

  private static final String MUTATION_CONSEQUENCE_GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + CONSEQUENCE_GENE_EMBED
      + "]" + PATH_SSM;

  private static final String MUTATION_CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION + ","
      + CONSEQUENCE_EMBED
      + "]" + PATH_SSM;

  private static final String MUTATION_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + MUTATION_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OBSERVATION_TERMS + "]" + PATH_OBSERVATION
      + "]" + PATH_SSM;

  private static final String CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + CONSEQUENCE_EMBED + PATH_SSM;

  @InjectMocks
  OccurrenceRepository repository;

  @Test
  public void testDonorProjectGenePathwayMutationConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_CONSEQUENCE_GENE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_GENE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGenePathwayConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + CONSEQUENCE_GENE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGenePathwayFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + GENE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGeneMutationConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_CONSEQUENCE_GENE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGeneMutationFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_FILTER, MUTATION_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_GENE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGeneConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + CONSEQUENCE_GENE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectGeneFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + GENE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectPathwayMutationConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, PATHWAY_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_CONSEQUENCE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectPathwayMutationFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, PATHWAY_FILTER, MUTATION_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectPathwayConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, PATHWAY_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + CONSEQUENCE_PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectPathwayFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, PATHWAY_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + PATHWAY_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectMutationConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_CONSEQUENCE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectMutationFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, MUTATION_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + MUTATION_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, CONSEQUENCE_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "," + CONSEQUENCE_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorProjectFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER };
    val expected = "[" + DONOR_REQUEST + "," + PROJECT_REQUEST + "]";

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testDonorFilter() {
    val fs = new String[] { DONOR_FILTER };
    val expected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testProjectFilter() {
    val fs = new String[] { PROJECT_FILTER };
    val expected = PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testGeneFilter() {
    val fs = new String[] { GENE_FILTER };
    val expected = GENE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testPathwayFilter() {
    val fs = new String[] { PATHWAY_FILTER };
    val expected = PATHWAY_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testMutationFilter() {
    val fs = new String[] { MUTATION_FILTER };
    val expected = MUTATION_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
  }

  @Test
  public void testConsequenceFilter() {
    val fs = new String[] { CONSEQUENCE_FILTER };
    val expected = CONSEQUENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
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
}
