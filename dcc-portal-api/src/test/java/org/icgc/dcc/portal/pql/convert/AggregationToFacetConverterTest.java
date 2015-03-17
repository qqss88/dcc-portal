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
package org.icgc.dcc.portal.pql.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.repository.BaseRepositoryTest;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class AggregationToFacetConverterTest extends BaseRepositoryTest {

  AggregationToFacetConverter converter = new AggregationToFacetConverter();

  @Before
  public void setUp() {
    es.execute(createIndexMappings(Type.DONOR, Type.DONOR_CENTRIC).withData(bulkFile(getClass())));
  }

  @Test
  public void convertTest() {
    val filter = FilterBuilders.existsFilter("_donor_id");

    val aggregation = global("projectId").subAggregation(
        filter("projectId").filter(filter).subAggregation(
            nested("projectId").path("gene.ssm").subAggregation(
                terms("projectId").field("gene.ssm.mutation_type"))));

    val response = converter.convert(executeQuery(aggregation).getAggregations());

    assertThat(response).hasSize(1);
    val termFacet = response.get("projectId");
    assertThat(termFacet.getType()).isEqualTo("terms");
    assertThat(termFacet.getMissing()).isEqualTo(0L);
    assertThat(termFacet.getOther()).isEqualTo(0L);
    assertThat(termFacet.getTotal()).isEqualTo(211L);

    val terms = termFacet.getTerms();
    assertThat(terms).hasSize(1);
    val firstTerm = terms.get(0);
    assertThat(firstTerm.getCount()).isEqualTo(211L);
    assertThat(firstTerm.getTerm()).isEqualTo("single base substitution");
  }

  private SearchResponse executeQuery(AggregationBuilder<?> builder) {
    val request = es.client()
        .prepareSearch(INDEX.getIndex())
        .setTypes(Type.DONOR_CENTRIC.getId())
        .addAggregation(builder);
    log.debug("Request - {}", request);

    val response = request.execute().actionGet();
    log.debug("Response = {}", response);

    return response;
  }

}
