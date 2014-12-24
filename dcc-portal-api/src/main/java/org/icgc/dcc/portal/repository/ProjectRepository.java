package org.icgc.dcc.portal.repository;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.buildFilters;
import static org.icgc.dcc.portal.service.QueryService.getFacets;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.getFilters;
import static org.icgc.dcc.portal.util.ElasticsearchUtils.flattenFieldsMap;

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Slf4j
@Component
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

    ObjectNode filters = query.getFilters();
    search.setPostFilter(getFilters(filters, KIND));

    search.addFields(getFields(query, KIND));

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
    val fields = FIELDS_MAPPING.get(KIND);
    String[] excludeFields = null;

    GetRequestBuilder search = client.prepareGet(index, TYPE.getId(), id);

    if (query.hasFields()) {
      val fs = Lists.<String> newArrayList();
      for (String field : query.getFields()) {
        if (fields.containsKey(field)) {
          fs.add(fields.get(field));
        }
      }
      search.setFetchSource(fs.toArray(new String[fs.size()]), excludeFields);
    } else {
      search.setFetchSource(fields.values().toArray(new String[fields.size()]), excludeFields);
    }

    GetResponse response = search.execute().actionGet();

    if (!response.isExists()) {
      String type = KIND.getId().substring(0, 1).toUpperCase() + KIND.getId().substring(1);
      log.info("{} {} not found.", type, id);
      String msg = String.format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(msg).build());
    }

    val map = flattenFieldsMap(response.getSource());
    log.debug("{}", map);

    return map;
  }
}
