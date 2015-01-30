/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import java.util.HashMap;
import java.util.Map;

import lombok.val;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.portal.model.BeaconQueryResponse;
import org.icgc.dcc.portal.model.BeaconResponse;
import org.icgc.dcc.portal.model.BeaconResponseResponse;
import org.icgc.dcc.portal.model.IndexModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BeaconService {

  private final Client client;
  private final String index;

  @Autowired
  public BeaconService(Client client, @Value("#{indexName}") String index) {
    this.index = index;
    this.client = client;
  }

  public BeaconResponse query(String chromosome, int position, String reference, String allele) {
    val search = client.prepareSearch(index)
        .setTypes(IndexModel.Type.MUTATION_CENTRIC.getId())
        .setSearchType(QUERY_THEN_FETCH);

    val boolQuery = QueryBuilders.boolQuery();
    boolQuery.must(QueryBuilders.rangeQuery("chromosome_start").gte(position - 1000).lte(position));
    boolQuery.must(QueryBuilders.rangeQuery("chromosome_end").lte(position + 1000));
    boolQuery.must(QueryBuilders.termQuery("chromosome", chromosome));
    search.setQuery(boolQuery);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("allelelength", allele.length());
    params.put("allele", allele);
    params.put("position", position);

    if (allele.equals("D")) {
      search.addScriptField("result",
          "doc['mutation'].value.endsWith('.')");
    } else if (allele.equals("I")) {
      search.addScriptField("result",
          "doc['mutation'].value.startsWith('.')");
    } else {
      search.addScriptField("result",
          "var m = doc['mutation'].value;"
              + "var offset = position - doc['chromosome_start'].value;"
              + "var begin = m.indexOf('>') + 1 + offset;"
              + "var end = Math.min(begin + allelelength, m.length());"
              + "m = m.substring(begin,end);"
              + "m==allele", params);
    }

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

  private BeaconResponse createBeaconResponse(String exists, String chr, int pos, String ref, String ale) {
    val queryResp = new BeaconQueryResponse(ale, chr, pos, ref);
    val respResp = new BeaconResponseResponse(exists);
    return new BeaconResponse("whats the id", queryResp, respResp);
  }
}
