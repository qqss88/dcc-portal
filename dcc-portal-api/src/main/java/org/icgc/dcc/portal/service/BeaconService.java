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
package org.icgc.dcc.portal.service;

import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME_END;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME_START;
import lombok.val;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.portal.model.Beacon;
import org.icgc.dcc.portal.model.BeaconQuery;
import org.icgc.dcc.portal.model.BeaconResponse;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.resource.BeaconResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

/**
 * The Beacon searches the dataset for mutation in a given position and chromosome. The result is null if no mutations
 * are found at the given position, false if the mutation doesn't match the expected allele and true otherwise. Also see
 * {@link BeaconResource}.
 * 
 * <p>
 * <a href="https://docs.google.com/document/d/154GBOixuZxpoPykGKcPOyrYUcgEXVe2NvKx61P4Ybn4/edit?usp=sharing">Draft of
 * v0.2 API </a>
 * </p>
 * 
 * <p>
 * <a href="https://github.com/ga4gh/beacon-team">GA4GH's Beacon github home </a>
 * </p>
 * 
 */
@Service
public class BeaconService {

  private final int POSITION_BUFFER = 1000; // Must be larger than any single mutation.
  private final String BEACON_ID = "ICGC";

  private final Client client;
  private final String index;

  @Autowired
  public BeaconService(Client client, @Value("#{indexName}") String index) {
    this.index = index;
    this.client = client;
  }

  public Beacon query(String chromosome, int position, String reference, String allele) {
    val search = client.prepareSearch(index)
        .setTypes(IndexModel.Type.MUTATION_CENTRIC.getId())
        .setSearchType(QUERY_THEN_FETCH);

    val boolQuery = QueryBuilders.boolQuery();
    boolQuery.must(QueryBuilders.rangeQuery(MUTATION_CHROMOSOME_START).gte(position - POSITION_BUFFER)
        .lte(position));
    boolQuery.must(QueryBuilders.rangeQuery(MUTATION_CHROMOSOME_END).lte(position + POSITION_BUFFER));
    boolQuery.must(QueryBuilders.termQuery(MUTATION_CHROMOSOME, chromosome));
    search.setQuery(boolQuery);

    val params = new ImmutableMap.Builder<String, Object>()
        .put("allelelength", allele.length())
        .put("allele", allele)
        .put("position", position).build();

    search.addScriptField("result", generateDefaultScriptField(), params);

    val filter = FilterBuilders.scriptFilter(
        "var m = doc['mutation'].value;"
            + "var length = m.substring(m.indexOf('>')+1,m.length()).length();"
            + "position <= doc['chromosome_start'].value+length"
        ).addParam("position", position);
    search.setFilter(filter);

    val hits = search.execute().actionGet().getHits();
    String finalResult = "null";

    for (val hit : hits) {
      Boolean result = hit.field("result").getValue();
      if (result) {
        finalResult = "true";
        break;
      } else if (!result) {
        finalResult = "false";
      }
    }

    return createBeaconResponse(finalResult, chromosome, position, reference, allele);
  }

  private String generateDefaultScriptField() {
    return "var m = doc['mutation'].value;"
        + "var offset = position - doc['chromosome_start'].value;"
        + "var begin = m.indexOf('>') + 1 + offset;"
        + "var end = Math.min(begin + allelelength, m.length());"
        + "m = m.substring(begin,end);"
        + "m==allele";
  }

  private Beacon createBeaconResponse(String exists, String chromosome, int position, String reference, String allele) {
    val queryResp = new BeaconQuery(allele, chromosome, position, reference);
    val respResp = new BeaconResponse(exists);
    return new Beacon(BEACON_ID, queryResp, respResp);
  }

  /**
   * Ignored until more detail is given about wildcard behaviour
   */
  @SuppressWarnings("unused")
  private String generateInsertionScriptField() {
    return "doc['mutation'].value.startsWith('.')";
  }

  /**
   * Ignored until more detail is given about wildcard behaviour
   */
  @SuppressWarnings("unused")
  private String generateDeletionScriptField() {
    return "doc['mutation'].value.endsWith('.')";
  }

}
