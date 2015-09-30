/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static org.elasticsearch.action.search.SearchType.QUERY_AND_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.andFilter;
import static org.elasticsearch.index.query.FilterBuilders.orFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.search.facet.FacetBuilders.histogramFacet;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;

@Component
public class BrowserRepository {

  /*
   * Constants
   */
  private static final String MUTATION = Type.MUTATION_CENTRIC.getId();
  private static final String GENE = Type.GENE_CENTRIC.getId();
  private static final Integer MUTATION_SIZE = 100000;
  private static final Integer GENE_SIZE = 10000;

  private final Client client;
  private final String indexName;

  @Autowired
  public BrowserRepository(@NonNull Client client, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
  }

  public SearchResponse getMutation(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters) {
    val filter = getMutationFilter(segmentId, start, stop, consequenceTypes, projectFilters);

    val request = client.prepareSearch(indexName)
        .setTypes(MUTATION)
        .setSearchType(QUERY_AND_FETCH)
        .setPostFilter(filter)
        .addFields(
            "_mutation_id",
            "chromosome",
            "chromosome_start",
            "chromosome_end",
            "mutation_type",
            "mutation",
            "reference_genome_allele",
            "functional_impact_prediction_summary",
            "ssm_occurrence.project._project_id",
            "ssm_occurrence.project.project_name",
            "ssm_occurrence.project._summary._ssm_tested_donor_count")
        .setFetchSource(
            includes(
                "transcript.gene.symbol",
                "transcript.consequence._transcript_id",
                "transcript.consequence.consequence_type",
                "transcript.consequence.aa_mutation"),
            excludes())
        .setFrom(0)
        .setSize(MUTATION_SIZE);

    return request.execute().actionGet();
  }

  public SearchResponse getGene(String segmentId, Long start, Long stop, List<String> biotypes,
      boolean withTranscripts) {
    val filter = getGeneFilter(segmentId, start, stop, biotypes);

    val request = client.prepareSearch(indexName)
        .setTypes(GENE)
        .setSearchType(QUERY_AND_FETCH)
        .addFields(
            "_gene_id",
            "name",
            "biotype",
            "chromosome",
            "start",
            "end",
            "strand",
            "description")
        .setPostFilter(filter)
        .setFrom(0)
        .setSize(GENE_SIZE);

    if (withTranscripts) {
      request.setFetchSource("transcripts", null);
    }

    return request.execute().actionGet();
  }

  @SuppressWarnings("deprecation")
  public SearchResponse getGeneHistogram(Long interval, String segmentId, Long start, Long stop,
      List<String> biotypes) {
    val filter = getGeneFilter(segmentId, start, stop, biotypes);

    val histogramFacet = histogramFacet("hf")
        .facetFilter(filter)
        .field("start")
        .interval(interval);

    val request = client.prepareSearch(indexName)
        .setTypes(GENE)
        .setSearchType(QUERY_AND_FETCH)
        .setPostFilter(filter)
        .addFacet(histogramFacet)
        .setSize(0);

    return request.execute().actionGet();
  }

  @SuppressWarnings("deprecation")
  public SearchResponse getMutationHistogram(Long interval, String segmentId, Long start, Long stop,
      List<String> consequenceTypes, List<String> projectFilters) {
    val filter = getMutationFilter(segmentId, start, stop, consequenceTypes, projectFilters);

    val histogramFacet = histogramFacet("hf")
        .facetFilter(filter)
        .field("chromosome_start")
        .interval(interval);

    val request = client.prepareSearch(indexName)
        .setTypes(MUTATION)
        .setSearchType(QUERY_AND_FETCH)
        .setPostFilter(filter)
        .addFacet(histogramFacet)
        .setFrom(0)
        .setSize(0);

    return request.execute().actionGet();
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static FilterBuilder getMutationFilter(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters) {

    val filter = andFilter(
        termFilter("chromosome", segmentId),
        rangeFilter("chromosome_start").lte(stop),
        rangeFilter("chromosome_end").gte(start));

    if (consequenceTypes != null) {
      val consequenceFilter = getConsequenceFilter(consequenceTypes);
      filter.add(consequenceFilter);
    }

    if (projectFilters != null) {
      val projectFilter = getProjectFilter(projectFilters);
      filter.add(projectFilter);
    }

    return filter;
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static FilterBuilder getGeneFilter(String segmentId, Long start, Long stop, List<String> biotypes) {
    val filter = andFilter(
        termFilter("chromosome", segmentId),
        rangeFilter("start").lte(stop),
        rangeFilter("end").gte(start));

    if (biotypes != null) {
      val biotypeFilter = getBiotypeFilterBuilder(biotypes);
      filter.add(biotypeFilter);
    }

    return filter;
  }

  /**
   * Readability method to build list of biotype filters.
   */
  private static FilterBuilder getBiotypeFilterBuilder(List<String> biotypes) {
    val biotypeFilter = orFilter();
    for (val biotype : biotypes) {
      biotypeFilter.add(termFilter("biotype", biotype));
    }

    return biotypeFilter;
  }

  /**
   * Readability method for naming literal array of inclusion paths.
   */
  private static String[] includes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Readability method for naming literal array of exclusion paths.
   */
  private static String[] excludes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Builds a FilterBuilder for consequence types
   */
  private static FilterBuilder getConsequenceFilter(List<String> consequenceTypes) {
    val consequenceFilter = orFilter();
    for (val consequenceType : consequenceTypes) {
      consequenceFilter.add(FilterBuilders.termFilter("transcript.consequence.consequence_type", consequenceType));
    }

    return consequenceFilter;
  }

  /**
   * Builds a FilterBuilder for project names.
   */
  private static FilterBuilder getProjectFilter(List<String> projects) {
    val projectFilter = orFilter();
    for (val project : projects) {
      projectFilter.add(FilterBuilders.termFilter("ssm_occurrence.project.project_name", project));
    }

    return projectFilter;
  }

}