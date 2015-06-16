package org.icgc.dcc.portal.repository;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.buildFilters;
import static org.icgc.dcc.portal.service.QueryService.getFacets;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.getFilters;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.isRelatedToDoublePendingDonor;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.setFetchSourceOfSearchRequest;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class ProjectRepository {

  private static final String TYPE_ID = Type.PROJECT.getId();
  private static final Kind KIND = Kind.PROJECT;
  private static final Map<String, String> FIELD_MAP = FIELDS_MAPPING.get(KIND);

  private static final ImmutableList<String> FACETS = ImmutableList.of("id", "primarySite", "primaryCountries",
      "availableDataTypes");

  private final Client client;
  private final String index;

  @Autowired
  ProjectRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  public SearchResponse findAll(Query query) {
    val search = client
        .prepareSearch(index)
        .setTypes(TYPE_ID)
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELD_MAP.get(query.getSort()), query.getOrder());

    val filters = query.getFilters();
    search.setPostFilter(getFilters(filters, KIND));
    search.addFields(getFields(query, KIND));
    setFetchSourceOfSearchRequest(search, query, KIND);

    val facets = getFacets(query, KIND, FACETS, filters, null, null);
    for (val facet : facets) {
      search.addFacet(facet);
    }

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  public long count(Query query) {
    val search = client.prepareSearch(index)
        .setTypes(TYPE_ID)
        .setSearchType(COUNT);

    if (query.hasFilters()) {
      search.setPostFilter(buildFilters(query.getFilters(), KIND));
    }

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE_ID, id)
        .setFields(getFields(query, KIND));
    setFetchSourceOfGetRequest(search, query, KIND);

    val response = search.execute().actionGet();

    if (response.isExists()) {
      val result = createResponseMap(response, query, KIND);
      log.debug("Found project: '{}'.", result);

      return result;
    }

    if (!isRelatedToDoublePendingDonor(client, "project_code", id)) {
      // We know this is guaranteed to throw a 404, since the 'id' was not found in the first query.
      checkResponseState(id, response, KIND);
    }

    return singletonMap(FIELD_MAP.get("id"), id);
  }
}
