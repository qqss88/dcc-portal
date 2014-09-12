package org.icgc.dcc.portal.service;

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Project;
import org.icgc.dcc.portal.model.Projects;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.ProjectRepository;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class ProjectService {

  private final ProjectRepository projectRepository;

  public Projects findAll(Query query) {
    val response = projectRepository.findAll(query);
    val hits = response.getHits();

    val projectList = getProjectList(hits);

    Projects projects = new Projects(projectList);
    projects.setFacets(response.getFacets());
    projects.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return projects;
  }

  private ImmutableList<Project> getProjectList(SearchHits hits) {
    val projectList = ImmutableList.<Project> builder();
    for (val hit : hits) {
      val fieldMap = Maps.<String, Object> newHashMap();
      for (val field : hit.getFields().entrySet()) {
        fieldMap.put(field.getKey(), field.getValue().getValue());
      }
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
