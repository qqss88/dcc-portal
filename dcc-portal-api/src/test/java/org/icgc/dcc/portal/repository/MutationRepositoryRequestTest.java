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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Test that verifies the content and structure of requests sent by the {@link OccurrenceRepository} implementation.
 */
@Ignore("Too complex and error prone. Needs to rework validation")
public class MutationRepositoryRequestTest extends BaseRepositoryRequestTest {

  /**
   *
   */
  private static final String MATCH_ALL = "{\"nested\":{\"filter\":{\"match_all\":{}},\"path\":\"ssm_occurrence\"}}";

  protected static String DONOR_PROJECT_FILTER =
      "'donor':{'primarySite':{'is':['Brain']},'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}";

  protected static String DONOR_FILTER =
      "'donor':{'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}";

  private static String CONSEQUENCE_FILTER = "'mutation':{'consequenceType':{'is':['missense']}}";

  private static String TRANSCRIPT_FILTER = "'mutation':{'functionalImpact':{'is':['High']}}";

  private static String OCCURRENCE_FILTER =
      "'mutation':{'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  private static String MUTATION_OCCURRENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  private static String MUTATION_TRANSCRIPT_FILTER =
      "'mutation':{'type':{'is':['substitution']},'functionalImpact':{'is':['High']}}";

  private static String MUTATION_CONSEQUENCE_OCCURRENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'consequenceType':{'is':['missense']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  private static String MUTATION_CONSEQUENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'consequenceType':{'is':['missense']}}";

  private static String MUTATION_TRANSCRIPT_OCCURRENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'functionalImpact':{'is':['High']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  private static String MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'consequenceType':{'is':['missense']},'functionalImpact':{'is':['High']},'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}";

  private static String MUTATION_TRANSCRIPT_CONSEQUENCE_FILTER =
      "'mutation':{'type':{'is':['substitution']},'consequenceType':{'is':['missense']},'functionalImpact':{'is':['High']}}";

  private static final String DONOR_TERMS = "{\"terms\":{\""
      + getField("gender", Kind.DONOR, MutationRepository.PREFIX_MAPPING) + "\":[\"male\"]}},"
      + "{\"terms\":{\"" + getField("availableDataTypes", Kind.DONOR, MutationRepository.PREFIX_MAPPING)
      + "\":[\"cnsm\"]}}]}}";

  private static final String PROJECT_TERMS = "{\"terms\":{\""
      + getField("primarySite", Kind.PROJECT, MutationRepository.PREFIX_MAPPING) + "\":[\"Brain\"]}}";

  private static final String OCCURRENCE_TERMS =
      "{\"terms\":{\"ssm_occurrence.observation.platform\":[\"Illumina GA sequencing\"]}},{\"terms\":{\"ssm_occurrence.observation.verification_status\":[\"not tested\"]}}";

  private static final String GENE_TERMS = geneTerms(MutationRepository.PREFIX_MAPPING);

  private static final String PATHWAY_TERMS = pathwayTerms(MutationRepository.PREFIX_MAPPING);

  private static final String MUTATION_TERMS = "{\"terms\":{\"mutation_type\":[\"substitution\"]}}}}";

  private static final String CONSEQUENCE_TERMS =
      "{\"terms\":{\"transcript.consequence.consequence_type\":[\"missense\"]}}";

  private static final String PATH_SSM = "}},\"path\":\"ssm_occurrence\"}}";

  private static final String PATH_OBSERVATION = "}},\"path\":\"ssm_occurrence.observation\"}}";

  private static final String PATH_TRANSCRIPT = "}},\"path\":\"transcript\"}}";

  private static final String PATH_TRANSCRIPT_GENE_PATHWAYS = "}},\"path\":\"transcript.gene.pathways\"}}";

  private static final Type CENTRIC_TYPE = Type.GENE_CENTRIC;

  private static final String SORT = "affectedDonorCountFiltered";

  private static final String MUTATION_REQUEST = BOOL_MUST + MUTATION_TERMS;

  private static final String TRANSCRIPT_TERMS =
      "{\"terms\":{\"transcript.functional_impact_prediction_summary\":[\"High\"]}}}}";

  private static final String GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST_R
      + GENE_TERMS + PATH_TRANSCRIPT;

  private static final String GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST_R
      + GENE_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + "]" + PATH_TRANSCRIPT;

  private static final String CONSEQUENCE_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + CONSEQUENCE_TERMS + "}},"
      + BOOL_MUST_R + GENE_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + "]" + PATH_TRANSCRIPT;

  private static final String TRANSCRIPT_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + TRANSCRIPT_TERMS + ","
      + BOOL_MUST_R + GENE_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + "]" + PATH_TRANSCRIPT;

  private static final String PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + PATH_TRANSCRIPT;

  private static final String TRANSCRIPT_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST
      + TRANSCRIPT_TERMS + PATH_TRANSCRIPT;

  private static final String TRANSCRIPT_CONSEQUENCE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + TRANSCRIPT_TERMS + ","
      + BOOL_MUST + CONSEQUENCE_TERMS + "}},"
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + "]" + PATH_TRANSCRIPT;

  private static final String CONSEQUENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST
      + CONSEQUENCE_TERMS + "}}" + PATH_TRANSCRIPT;

  private static final String TRANSCRIPT_CONSEQUENCE_GENE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + TRANSCRIPT_TERMS + ","
      + BOOL_MUST + CONSEQUENCE_TERMS + "}},"
      + BOOL_MUST_R + GENE_TERMS
      + "]" + PATH_TRANSCRIPT;

  private static final String TRANSCRIPT_CONSEQUENCE_GENE_PATHWAY_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + TRANSCRIPT_TERMS + ","
      + BOOL_MUST + CONSEQUENCE_TERMS + "}},"
      + BOOL_MUST_R + GENE_TERMS + ","
      + NESTED_FILTER_BOOL_MUST_R
      + PATHWAY_TERMS
      + PATH_TRANSCRIPT_GENE_PATHWAYS
      + "]" + PATH_TRANSCRIPT;

  private static final String DONOR_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST_R
      + DONOR_TERMS + PATH_SSM;

  private static final String DONOR_OCCURRENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "["
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OCCURRENCE_TERMS + "]" + PATH_OBSERVATION + ","
      + BOOL_MUST_R + DONOR_TERMS
      + "]" + PATH_SSM;

  private static final String OCCURRENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R
      // + OCCURRENCE_TERMS
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OCCURRENCE_TERMS + "]" + PATH_OBSERVATION + PATH_SSM;

  private static final String DONOR_PROJECT_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + PROJECT_TERMS + "}},"
      + BOOL_MUST_R + DONOR_TERMS
      + "]" + PATH_SSM;

  private static final String PROJECT_REQUEST = NESTED_FILTER_BOOL_MUST_R + BOOL_MUST
      + PROJECT_TERMS + "}}" + PATH_SSM;

  private static final String DONOR_PROJECT_OCCURRENCE_REQUEST = NESTED_FILTER_BOOL_MUST_R + "[" + BOOL_MUST
      + PROJECT_TERMS + "}},"
      + NESTED_FILTER_BOOL_MUST_R + "["
      + OCCURRENCE_TERMS + "]" + PATH_OBSERVATION + ","
      + BOOL_MUST_R + DONOR_TERMS
      + "]" + PATH_SSM;

  @InjectMocks
  MutationRepository repository;

  @Test
  public void testDonorProjectGeneMutationTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_REQUEST + "," + DONOR_PROJECT_OCCURRENCE_REQUEST
            + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectPathwayMutationTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_PATHWAY_REQUEST + "," + DONOR_PROJECT_OCCURRENCE_REQUEST
            + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationTranscriptConsequenceOccurrenceFilter() {
    val fs =
        new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationTranscriptConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationTranscriptOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationTranscriptFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayMutationConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, OCCURRENCE_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayOccurrenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, OCCURRENCE_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayTranscriptFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, TRANSCRIPT_FILTER };
    val expected =
        "[" + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayConsequenceFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER, CONSEQUENCE_FILTER };
    val expected =
        "[" + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectGenePathwayFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER, GENE_PATHWAY_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_PROJECT_REQUEST + "]";
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorProjectFilter() {
    val fs = new String[] { DONOR_PROJECT_FILTER };
    val expected = DONOR_PROJECT_REQUEST;
    val scoreExpected = DONOR_PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGeneMutationTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_REQUEST + "," + DONOR_OCCURRENCE_REQUEST
            + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorPathwayMutationTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_PATHWAY_REQUEST + "," + DONOR_OCCURRENCE_REQUEST
            + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationTranscriptConsequenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_CONSEQUENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationTranscriptOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationTranscriptFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_TRANSCRIPT_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_OCCURRENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayMutationConsequenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, MUTATION_CONSEQUENCE_FILTER };
    val expected =
        "[" + MUTATION_REQUEST + "," + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayTranscriptConsequenceOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, OCCURRENCE_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayOccurrenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, OCCURRENCE_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_OCCURRENCE_REQUEST + "]";
    val scoreExpected = DONOR_OCCURRENCE_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayTranscriptFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, TRANSCRIPT_FILTER };
    val expected =
        "[" + TRANSCRIPT_GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayConsequenceFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER, CONSEQUENCE_FILTER };
    val expected =
        "[" + CONSEQUENCE_GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorGenePathwayFilter() {
    val fs = new String[] { DONOR_FILTER, GENE_PATHWAY_FILTER };
    val expected =
        "[" + GENE_PATHWAY_REQUEST + ","
            + DONOR_REQUEST + "]";
    val scoreExpected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testDonorFilter() {
    val fs = new String[] { DONOR_FILTER };
    val expected = DONOR_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  @Test
  public void testProjectFilter() {
    val fs = new String[] { PROJECT_FILTER };
    val expected = PROJECT_REQUEST;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), expected);
  }

  @Test
  public void testGeneFilter() {
    val fs = new String[] { GENE_FILTER };
    val expected = GENE_REQUEST;
    val scoreExpected = MATCH_ALL;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testPathwayFilter() {
    val fs = new String[] { PATHWAY_FILTER };
    val expected = PATHWAY_REQUEST;
    val scoreExpected = MATCH_ALL;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testTranscriptFilter() {
    val fs = new String[] { TRANSCRIPT_FILTER };
    val expected = TRANSCRIPT_REQUEST;
    val scoreExpected = MATCH_ALL;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testConsequenceFilter() {
    val fs = new String[] { CONSEQUENCE_FILTER };
    val expected = CONSEQUENCE_REQUEST;
    val scoreExpected = MATCH_ALL;

    assertFilter(findAllSetup(fs), expected);
    assertFilter(countSetup(fs), expected);
    assertScore(scoreSetup(fs), scoreExpected);
  }

  @Test
  public void testOccurrenceFilter() {
    val fs = new String[] { OCCURRENCE_FILTER };
    val expected = OCCURRENCE_REQUEST;

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

    val request = $(repository.buildScoreFilters(query));
    log(filter.get(), request);
    return request;
  }

  private void assertScore(JsonNode jn, String expected) {
    val s = "{\"nested\":{\"filter\":" + jn.toString() + ",\"path\":\"ssm_occurrence\"}}";
    assertScore(s, expected);
  }
}
