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

import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.REPOSITORY_INDEX_NAME;
import static org.icgc.dcc.portal.service.QueryService.getFields;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.val;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

@Slf4j
@Component
public class SearchRepository {

  // Constants
  @UtilityClass
  private class Types {

    private final String PATHWAY = "pathway";
    private final String CURATED_SET = "curated_set";
    private final String GO_TERM = "go_term";
    private final String DONOR = "donor";
    private final String PROJECT = "project";
    private final String GENE = "gene";
    private final String MUTATION = "mutation";
    private final String GENE_SET = "geneSet";
    private final String FILE = "file";
    private final String FILE_DONOR = "file-donor";

  }

  private static final Kind KIND = Kind.KEYWORD;
  private static final Set<String> FIELD_KEYS = FIELDS_MAPPING.get(KIND).keySet();
  private static final String FILE_NAME_FIELD = "file_name";
  private static final float TIE_BREAKER = 0.7F;
  private static final List<String> SIMPLE_TERM_FILTER_TYPES = ImmutableList.of(
      Types.PATHWAY, Types.CURATED_SET, Types.GO_TERM);

  private static final Map<String, Type> TYPE_MAPPINGS = ImmutableMap.<String, Type> builder()
      .put(Types.GENE, Type.GENE_TEXT)
      .put(Types.MUTATION, Type.MUTATION_TEXT)
      .put(Types.DONOR, Type.DONOR_TEXT)
      .put(Types.PROJECT, Type.PROJECT_TEXT)
      .put(Types.PATHWAY, Type.GENESET_TEXT)
      .put(Types.GENE_SET, Type.GENESET_TEXT)
      .put(Types.GO_TERM, Type.GENESET_TEXT)
      .put(Types.CURATED_SET, Type.GENESET_TEXT)
      .put(Types.FILE, Type.REPOSITORY_FILE_TEXT)
      .put(Types.FILE_DONOR, Type.REPOSITORY_FILE_DONOR_TEXT)
      .build();
  private static final Map<String, String> TYPE_ID_MAPPINGS = transformValues(TYPE_MAPPINGS, type -> type.getId());
  private static final String MUTATION_PREFIX = TYPE_ID_MAPPINGS.get(Types.MUTATION);

  private static final Set<String> MULTIPLE_SEARCH_TYPES = Stream.of(
      Types.GENE,
      /*
       * Types.FILE must appear before Types.DONOR for searching file UUID in "file-text" to work. See DCC-3967 and
       * https://github.com/elastic/elasticsearch/issues/2218 for details.
       */
      Types.FILE,
      Types.DONOR,
      Types.PROJECT,
      Types.MUTATION,
      Types.GENE_SET)
      .map(t -> TYPE_ID_MAPPINGS.get(t))
      .collect(toImmutableSet());

  // Instance variables
  private final Client client;

  @Value("#{indexName}")
  private String indexName;

  @Autowired
  SearchRepository(Client client, IndexModel indexModel) {
    this.client = client;
  }

  @SuppressWarnings("deprecation")
  @NonNull
  public SearchResponse findAll(Query query, String type) {
    log.info("Requested search type is: '{}'.", type);

    val search = createSearch(type)
        .setSearchType(DFS_QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .setTypes(getSearchTypes(type))
        .addFields(getFields(query, KIND))
        .setQuery(getQuery(query))
        .setPostFilter(getPostFilter(type));

    log.info("ES search query is: {}", search);
    val response = search.execute().actionGet();
    log.debug("ES search result is: {}", response);

    return response;
  }

  // Helpers
  private SearchRequestBuilder createSearch(String type) {
    // Determines which index to use as external file repository are in a daily generated index separated
    // from the main icgc-index.
    if (type.equals(Types.FILE) || type.equals(Types.FILE_DONOR)) {
      return client.prepareSearch(REPOSITORY_INDEX_NAME);
    }

    if (type.equals(Types.DONOR)) {
      return client.prepareSearch(indexName);
    }

    return client.prepareSearch(indexName, REPOSITORY_INDEX_NAME);
  }

  private static String[] toStringArray(Collection<String> source) {
    return source.stream().toArray(String[]::new);
  }

  private static String[] getSearchTypes(String type) {
    val result = TYPE_ID_MAPPINGS.containsKey(type) ?
        newHashSet(TYPE_ID_MAPPINGS.get(type)) :
        MULTIPLE_SEARCH_TYPES;

    return toStringArray(result);
  }

  private static FilterBuilder getPostFilter(String type) {
    val field = "type";
    val result = boolFilter();

    if (SIMPLE_TERM_FILTER_TYPES.contains(type)) {
      return result.must(termFilter(field, type));
    }

    val donor = boolFilter()
        .must(termFilter(field, Types.DONOR));
    val project = boolFilter()
        .must(termFilter(field, Types.PROJECT));
    val others = boolFilter()
        .mustNot(termsFilter(field, Types.DONOR, Types.PROJECT));

    return result
        .should(donor)
        .should(project)
        .should(others);
  }

  private static QueryBuilder getQuery(Query query) {
    val queryString = query.getQuery();
    val prefixQuery = prefixQuery(FILE_NAME_FIELD + ".raw", queryString);
    val keys = buildMultiMatchFieldList(FIELD_KEYS, queryString);
    val multiMatchQuery = multiMatchQuery(queryString, keys).tieBreaker(TIE_BREAKER);

    return boolQuery()
        .should(prefixQuery)
        .should(multiMatchQuery);
  }

  @NonNull
  private static String[] buildMultiMatchFieldList(Iterable<String> baseKeys, String queryString) {
    val keys = Sets.<String> newHashSet();

    for (val baseKey : baseKeys) {

      // Exact match fields (DCC-2324)
      if (baseKey.equals("start")) {
        // NOTE: This is a work around quirky ES issue.
        // We need to prefix the document type here to prevent NumberFormatException, it appears that ES
        // cannot determine what type 'start' is.
        // This is for ES 0.9, later versions may not have this problem.
        keys.add(format("%s.%s", MUTATION_PREFIX, baseKey));

      } else if (!baseKey.equals("geneMutations") && !baseKey.equals(FILE_NAME_FIELD)) {
        keys.add(baseKey + ".search^2");
        keys.add(baseKey + ".analyzed");
      }

    }

    // don't boost without space or genes won't show when partially matched
    if (queryString.contains(" ")) {
      keys.add("geneMutations.search^2");
      keys.add("geneMutations.analyzed^2");
    } else {
      keys.add("geneMutations.search^2");
      keys.add("geneMutations.analyzed");
    }

    // Exact-match search on "id".
    keys.add("id");

    return toStringArray(keys);
  }

}
