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
package org.icgc.dcc.portal.repository;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.isEmpty;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.common.collect.Iterables.size;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.icgc.dcc.portal.model.GeneSetType;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.service.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class GeneSetRepository {

  /**
   * Constants.
   */
  private static final String INDEX_GENE_COUNT_FIELD_NAME = "_summary._gene_count";
  private static final String INDEX_GENE_SETS_NAME_FIELD_NAME = "name";

  private static final Type TYPE = Type.GENE_SET;
  private static final Kind KIND = Kind.GENE_SET;

  private final Client client;
  private final String index;

  @Autowired
  public GeneSetRepository(@NonNull Client client, @NonNull IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  public int countGenes(@NonNull String id) {
    return countGenes(ImmutableList.of(id)).values().iterator().next();
  }

  public Map<String, Integer> countGenes(@NonNull Iterable<String> ids) {
    val fieldName = INDEX_GENE_COUNT_FIELD_NAME;
    val response = findField(ids, fieldName);

    val map = Maps.<String, Integer> newLinkedHashMap();
    for (val hit : response.getHits()) {
      val id = hit.getId();
      val count = (Integer) hit.getFields().get(fieldName).getValue();

      map.put(id, count);
    }

    return map;
  }

  public int countDecendants(@NonNull GeneSetType type, @NonNull Optional<String> id) {
    QueryBuilder query;

    switch (type) {
    case GO_TERM:
      query =
          new FilteredQueryBuilder(new MatchAllQueryBuilder(), id.isPresent() ?
              new TermFilterBuilder("go_term.inferred_tree.id", id.get()) :
              new TermFilterBuilder("type", "go_term"));
      break;
    case PATHWAY:
      query =
          new FilteredQueryBuilder(new MatchAllQueryBuilder(), id.isPresent() ?
              new TermFilterBuilder("pathway.hierarchy.id", id.get()) :
              new TermFilterBuilder("type", "pathway"));
      break;
    case CURATED_SET:
      query =
          new FilteredQueryBuilder(new MatchAllQueryBuilder(), id.isPresent() ?
              new TermFilterBuilder("curated_set.id", id.get()) :
              new TermFilterBuilder("type", "curated_set"));
      break;
    default:
      checkState(false, "Unexpected type %s", type);
      return -1;
    }

    return (int) client.prepareCount(index)
        .setTypes(TYPE.getId())
        .setQuery(query)
        .execute()
        .actionGet()
        .getCount();
  }

  public Map<String, String> findName(@NonNull Iterable<String> ids) {
    val fieldName = INDEX_GENE_SETS_NAME_FIELD_NAME;
    val response = findField(ids, fieldName);

    val map = Maps.<String, String> newLinkedHashMap();
    for (val hit : response.getHits()) {
      val id = hit.getId();
      val count = (String) hit.getFields().get(fieldName).getValue();

      map.put(id, count);
    }

    return map;
  }

  public Map<String, Object> findOne(@NonNull String id, String... fieldNames) {
    return findOne(id, ImmutableList.copyOf(fieldNames));
  }

  public Map<String, Object> findOne(String id, Iterable<String> fieldNames) {
    val fields = FIELDS_MAPPING.get(KIND);
    val fs = Lists.<String> newArrayList();

    // To be interpreted as explicit arrays
    val arrayFields = ImmutableList.<String> of(
        fields.get("hierarchy"),
        fields.get("altIds"),
        fields.get("synonyms"),
        fields.get("inferredTree"),
        fields.get("projects"));

    // Old fields - To remove
    /*
     * fields.get("geneList"), fields.get("parentPathways"), fields.get("linkOut"));
     */

    val search = client.prepareGet(index, TYPE.getId(), id);

    if (!isEmpty(fieldNames)) {
      for (val field : fieldNames) {
        if (fields.containsKey(field)) {
          fs.add(fields.get(field));
        }
      }
    } else {
      fs.addAll(fields.values().asList());
    }

    search.setFields(fs.toArray(new String[fs.size()]));

    GetResponse response = search.execute().actionGet();

    if (!response.isExists()) {
      String type = KIND.getId().substring(0, 1).toUpperCase() + KIND.getId().substring(1);
      log.info("{} {} not found.", type, id);

      throw new NotFoundException(id, type);
    }

    val map = Maps.<String, Object> newHashMap();
    for (GetField f : response.getFields().values()) {
      map.put(f.getName(), f.getValue());
    }
    for (val f : response.getFields().values()) {
      if (arrayFields.contains(f.getName())) {
        map.put(f.getName(), f.getValues());
      } else {
        map.put(f.getName(), f.getValue());
      }
    }

    log.debug("{}", map);

    return map;
  }

  private SearchResponse findField(Iterable<String> ids, String fieldName) {
    val filters = new TermsFilterBuilder("_id", ids);

    val search = client.prepareSearch(index)
        .setTypes(TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(size(ids))
        .setFilter(filters)
        .addField(fieldName);

    return search.execute().actionGet();
  }
}
