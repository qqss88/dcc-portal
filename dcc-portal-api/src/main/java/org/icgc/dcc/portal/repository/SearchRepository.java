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

import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Sets;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;

import com.google.inject.Inject;

@Slf4j
public class SearchRepository {

  private static final Kind KIND = Kind.KEYWORD;
  private static final Type GENE_TEXT = Type.GENE_TEXT;
  private static final Type DONOR_TEXT = Type.DONOR_TEXT;
  private static final Type PROJECT_TEXT = Type.PROJECT_TEXT;
  private static final Type MUTATION_TEXT = Type.MUTATION_TEXT;
  private static final Type PATHWAY_TEXT = Type.PATHWAY_TEXT;

  private final Client client;

  private final String index;

  @Inject
  SearchRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  public SearchResponse findAll(Query query, String type) {
    SearchRequestBuilder search = client.prepareSearch(index)
        .setSearchType(DFS_QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize());

    if (type.equals("gene")) search.setTypes(GENE_TEXT.getId());
    else if (type.equals("mutation")) search.setTypes(MUTATION_TEXT.getId());
    else if (type.equals("donor")) search.setTypes(DONOR_TEXT.getId());
    else if (type.equals("project")) search.setTypes(PROJECT_TEXT.getId());
    else if (type.equals("pathway")) search.setTypes(PATHWAY_TEXT.getId());
    else
      search.setTypes(GENE_TEXT.getId(), DONOR_TEXT.getId(), PROJECT_TEXT.getId(), MUTATION_TEXT.getId(),
          PATHWAY_TEXT.getId());

    search.addFields(getFields(query, KIND));

    val baseKeys = IndexModel.FIELDS_MAPPING.get(KIND).keySet();
    val keys = Sets.<String> newHashSet();
    for (val baseKey : baseKeys) {

      // Exact match fields (DCC-2324)
      if (baseKey.equals("start")) {
        keys.add(baseKey);
      } else if (!baseKey.equals("geneMutations")) {
        keys.add(baseKey + ".search^2");
        keys.add(baseKey + ".analyzed");
      }

    }

    // don't boost without space or genes won't show when partially matched
    if (query.getQuery().contains(" ")) {
      keys.add("geneMutations.search^2");
      keys.add("geneMutations.analyzed^2");
    } else {
      keys.add("geneMutations.search^2");
      keys.add("geneMutations.analyzed");
    }

    String[] aKeys = keys.toArray(new String[keys.size()]);

    search.setQuery(multiMatchQuery(query.getQuery(), aKeys).tieBreaker(0.7F));

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }
}
