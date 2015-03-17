package org.icgc.dcc.portal.model;

import java.util.Map;

import lombok.Data;

import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.terms.TermsFacet;

import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Data
public class Paginated {

  @ApiModelProperty(value = "EnrichmentSearchResponses")
  private Map<String, TermFacet> facets;

  @ApiModelProperty(value = "Pagination Data", required = true)
  private Pagination pagination;

  public void setFacets(Facets facets) {
    if (facets != null) {
      ImmutableMap.Builder<String, TermFacet> terms = ImmutableMap.builder();
      for (Facet facet : facets.facets())
        terms.put(facet.getName(), TermFacet.of((TermsFacet) facet));

      this.facets = terms.build();
    }
  }

  public void setFacets(Map<String, TermFacet> facets) {
    this.facets = facets;
  }

}
