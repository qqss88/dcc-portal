/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.getFields;

import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class DiagramRepository {

  private static final Type TYPE = Type.DIAGRAM;
  private static final Kind KIND = Kind.DIAGRAM;

  private final Client client;

  private final String index;

  @Autowired
  DiagramRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  public SearchResponse findAll(Query query) {
    SearchRequestBuilder search =
        client.prepareSearch(index).setTypes(TYPE.getId()).setSearchType(QUERY_THEN_FETCH).setFrom(query.getFrom())
            .setSize(query.getSize());

    search.addFields(getFields(query, KIND));

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  public Map<String, Object> findOne(String id, Query query) {
    val fieldMapping = FIELDS_MAPPING.get(KIND);
    val fs = Lists.<String> newArrayList();

    GetRequestBuilder search = client.prepareGet(index, TYPE.getId(), id);

    if (query.hasFields()) {
      for (String field : query.getFields()) {
        if (fieldMapping.containsKey(field)) {
          fs.add(fieldMapping.get(field));
        }
      }
    } else
      fs.addAll(fieldMapping.values().asList());

    search.setFields(fs.toArray(new String[fs.size()]));

    GetResponse response = search.execute().actionGet();

    if (!response.isExists()) {
      String type = KIND.getId().substring(0, 1).toUpperCase() + KIND.getId().substring(1);
      log.info("{} {} not found.", type, id);
      String msg = String.format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(msg).build());
    }

    val map = Maps.<String, Object> newHashMap();
    for (val f : response.getFields().values()) {
      if (Lists.newArrayList().contains(f.getName())) {
        map.put(f.getName(), f.getValues());
      } else {
        map.put(f.getName(), f.getValue());
      }
    }

    log.debug("{}", map);

    return map;
  }
}
