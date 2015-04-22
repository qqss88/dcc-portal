/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dcc.portal.pql.es.visitor.aggs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.qe.QueryEngine;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class NestedAggregationVisitorTest extends BaseElasticsearchTest {

  QueryEngine queryEngine;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(MUTATION_CENTRIC).withData(bulkFile(getClass())));
    queryEngine = new QueryEngine(es.client(), INDEX_NAME);
  }

  @Test
  public void nestedAggregationTest() {
    val result = executeQuery("facets(consequenceTypeNested)");

    Global global = result.getAggregations().get("consequenceTypeNested");
    Nested nested = global.getAggregations().get("consequenceTypeNested");
    Terms terms = nested.getAggregations().get("consequenceTypeNested");

    val missenseAll = terms.getBucketByKey("missense_variant");
    assertThat(missenseAll.getDocCount()).isEqualTo(5L);
    ReverseNested missenseNested = missenseAll.getAggregations().get("consequenceTypeNested");
    assertThat(missenseNested.getDocCount()).isEqualTo(3L);

    val frameshiftAll = terms.getBucketByKey("frameshift_variant");
    assertThat(frameshiftAll.getDocCount()).isEqualTo(2L);
    ReverseNested frameshiftNested = frameshiftAll.getAggregations().get("consequenceTypeNested");
    assertThat(frameshiftNested.getDocCount()).isEqualTo(2L);

    val intergenicAll = terms.getBucketByKey("intergenic_region");
    assertThat(intergenicAll.getDocCount()).isEqualTo(2L);
    ReverseNested intergenicNested = intergenicAll.getAggregations().get("consequenceTypeNested");
    assertThat(intergenicNested.getDocCount()).isEqualTo(1L);

    // Missing
    Global globalMissing = result.getAggregations().get("consequenceTypeNested_missing");
    Nested nestedMissing = globalMissing.getAggregations().get("consequenceTypeNested_missing");
    Missing termsMissing = nestedMissing.getAggregations().get("consequenceTypeNested_missing");
    ReverseNested reverseMissing = termsMissing.getAggregations().get("consequenceTypeNested_missing");
    assertThat(reverseMissing.getDocCount()).isEqualTo(1L);
  }

  @Test
  public void nestedAggregationTest_withFilter() {
    val result = executeQuery("facets(consequenceTypeNested), eq(transcriptId, 'T7')");

    Global global = result.getAggregations().get("consequenceTypeNested");
    Nested nested = global.getAggregations().get("consequenceTypeNested");
    Filter nestedFilter = nested.getAggregations().get("consequenceTypeNested");
    Terms terms = nestedFilter.getAggregations().get("consequenceTypeNested");

    val intergenicAll = terms.getBucketByKey("intergenic_region");
    assertThat(intergenicAll.getDocCount()).isEqualTo(1L);
    ReverseNested intergenicNested = intergenicAll.getAggregations().get("consequenceTypeNested");
    assertThat(intergenicNested.getDocCount()).isEqualTo(1L);
  }

  private SearchResponse executeQuery(String query) {
    val request = queryEngine.execute(query, MUTATION_CENTRIC);
    log.debug("Request - {}", request);
    val result = request.execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

}
