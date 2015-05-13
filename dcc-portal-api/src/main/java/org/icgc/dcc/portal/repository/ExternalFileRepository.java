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
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.getFacets;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.resolveSourceFields;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.util.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvMapWriter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class ExternalFileRepository {

  private final String INDEX_NAME = "icgc-repository";

  // private static final Type TYPE = Type.RELEASE;
  private static final Kind KIND = Kind.EXTERNAL_FILE;
  private final static TimeValue KEEP_ALIVE = new TimeValue(10000);

  private static final ImmutableList<String> FACETS = ImmutableList.of("study", "dataType", "fileFormat", "access",
      "projectCode", "primarySite", "donorStudy", "repositoryNames");

  private final ImmutableMap<String, String> EXPORT_FIELDS = ImmutableMap.<String, String> builder()
      .put("access", "Access")
      .put("repository.file_name", "File name")
      .put("donor.donor_id", "ICGC Donor")
      .put("repository.repo_server.repo_name", "Repository")
      .put("donor.project_code", "Project")
      .put("study", "Study")
      .put("data_types.data_type", "Data type")
      .put("data_types.data_format", "Format")
      .put("repository.file_size", "Size")
      .build();

  private final ImmutableList<String> ARRAY_FIELDS = ImmutableList.of("data_types.data_type", "data_types.data_format",
      "repository.repo_server.repo_name");

  private final Client client;
  private final String index;

  @Autowired
  ExternalFileRepository(Client client) {
    this.index = INDEX_NAME;
    this.client = client;
  }

  public StreamingOutput exportData(Query query) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        val filters = QueryService.buildFilters(query.getFilters(), Kind.EXTERNAL_FILE);
        String headers[] = EXPORT_FIELDS.values().toArray(new String[EXPORT_FIELDS.values().size()]);
        String keys[] = EXPORT_FIELDS.keySet().toArray(new String[EXPORT_FIELDS.keySet().size()]);

        @Cleanup
        val writer = new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(os)), TAB_PREFERENCE);
        writer.writeHeader(headers);

        val search = client
            .prepareSearch(index)
            .setTypes(KIND.getId())
            .setSearchType(SCAN)
            .setSize(5000)
            .setScroll(KEEP_ALIVE)
            .setPostFilter(filters)
            .setQuery(matchAllQuery())
            .addFields(EXPORT_FIELDS.keySet().toArray(new String[EXPORT_FIELDS.keySet().size()]));

        SearchResponse response = search.execute().actionGet();
        while (true) {
          response = client.prepareSearchScroll(response.getScrollId())
              .setScroll(KEEP_ALIVE)
              .execute().actionGet();

          val finished = !hasHits(response);

          if (finished) {
            break;
          } else {

            for (SearchHit hit : response.getHits()) {
              val map = normalize(createResponseMap(hit, query, Kind.EXTERNAL_FILE));
              writer.write(map, keys);
            }
          }
        }
      }
    };
  }

  /**
   * Untangle array/list objects, numeric objects
   */
  private Map<String, String> normalize(Map<String, Object> map) {
    val result = Maps.<String, String> newHashMap();
    for (val key : map.keySet()) {
      // if (key.equals("repository.repo_server.repo_name")) {
      if (ARRAY_FIELDS.contains(key)) {
        result.put(key, Joiner.on(StringUtils.COMMA).join((List<String>) map.get(key)));
      } else if (key.equals("repository.file_size")) {
        result.put(key, getLong(map.get(key)).toString());
      } else {
        result.put(key, getString(map.get(key)));
      }
    }
    return result;
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
