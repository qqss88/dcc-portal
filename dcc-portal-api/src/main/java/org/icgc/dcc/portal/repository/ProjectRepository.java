package org.icgc.dcc.portal.repository;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.buildFilters;
import static org.icgc.dcc.portal.service.QueryService.getFacets;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.getFilters;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.resolveSourceFields;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchRequestBuilder;
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

  private static final Type TYPE = Type.PROJECT;
  private static final Kind KIND = Kind.PROJECT;

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
    SearchRequestBuilder search = client
        .prepareSearch(index)
        .setTypes(TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELDS_MAPPING.get(KIND).get(query.getSort()), query.getOrder());

    val filters = query.getFilters();
    search.setPostFilter(getFilters(filters, KIND));
    search.addFields(getFields(query, KIND));
    String[] sourceFields = resolveSourceFields(query, KIND);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, KIND), EMPTY_SOURCE_FIELDS);
    }

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
    SearchRequestBuilder search = client.prepareSearch(index).setTypes(TYPE.getId()).setSearchType(COUNT);

    if (query.hasFilters()) search.setPostFilter(buildFilters(query.getFilters(), KIND));

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE.getId(), id);
    search.setFields(getFields(query, KIND));

    String[] sourceFields = resolveSourceFields(query, KIND);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, KIND), EMPTY_SOURCE_FIELDS);
    }

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val result = createResponseMap(response, query, KIND);
    log.debug("{}", result);

    return result;
  }
}
