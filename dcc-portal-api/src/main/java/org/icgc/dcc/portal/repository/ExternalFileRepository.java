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
package org.icgc.dcc.portal.repository;

import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.getFacets;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.resolveSourceFields;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Slf4j
@Component
public class ExternalFileRepository {

  private final String INDEX_NAME = "icgc-repository";

  // private static final Type TYPE = Type.RELEASE;
  private static final Kind KIND = Kind.EXTERNAL_FILE;

  private static final ImmutableList<String> FACETS = ImmutableList.of("study", "dataType", "fileFormat", "access",
      "projectCode", "primarySite", "donorStudy", "repositoryNames");

  private final Client client;
  private final String index;

  @Autowired
  ExternalFileRepository(Client client) {
    this.index = INDEX_NAME;
    this.client = client;
  }

  public Map<String, String> getIndexMetadata() {
    return null;
  }

  public SearchResponse findAll(Query query) {
    val kind = Kind.EXTERNAL_FILE;
    val filters = QueryService.buildFilters(query.getFilters(), Kind.EXTERNAL_FILE);
    val search = client.prepareSearch(index)
        .setTypes("file")
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELDS_MAPPING.get(KIND).get(query.getSort()), query.getOrder());

    search.setPostFilter(filters);

    search.addFields(getFields(query, KIND));
    String[] sourceFields = resolveSourceFields(query, kind);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, kind), EMPTY_SOURCE_FIELDS);
    }
    val facets = getFacets(query, kind, FACETS, query.getFilters(), null, null);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    log.debug("{}", search);
    val response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

}
