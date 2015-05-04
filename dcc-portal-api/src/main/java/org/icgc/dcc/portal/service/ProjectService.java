package org.icgc.dcc.portal.service;

import static org.dcc.portal.pql.meta.Type.PROJECT;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Project;
import org.icgc.dcc.portal.model.Projects;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ProjectService {

  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();
  private final AggregationToFacetConverter aggregationsConverter = AggregationToFacetConverter.getInstance();

  private final ProjectRepository projectRepository;
  private final QueryEngine queryEngine;

  public Projects findAll(Query query) {
    val pql = converter.convert(query, PROJECT);
    log.debug("Query: {}. PQL: {}", query, pql);

    val request = queryEngine.execute(pql, PROJECT);
    log.debug("Request: {}", request);

    val response = request.getRequestBuilder().execute().actionGet();
    log.debug("Response: {}", response);

    val hits = response.getHits();
    val projectList = getProjectList(hits, query);

    Projects projects = new Projects(projectList);
    projects.addFacets(aggregationsConverter.convert(response.getAggregations()));
    projects.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return projects;
  }

  private ImmutableList<Project> getProjectList(SearchHits hits, Query query) {
    val projectList = ImmutableList.<Project> builder();
    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, query, Kind.PROJECT);
      projectList.add(new Project(fieldMap));
    }

    return projectList.build();
  }

  public long count(Query query) {
    return projectRepository.count(query);
  }

  public Project findOne(String projectId, Query query) {
    return new Project(projectRepository.findOne(projectId, query));
  }
}
