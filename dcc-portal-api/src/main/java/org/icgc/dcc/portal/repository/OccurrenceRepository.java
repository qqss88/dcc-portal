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

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.service.QueryService.buildConsequenceFilters;
import static org.icgc.dcc.portal.service.QueryService.buildDonorFilters;
import static org.icgc.dcc.portal.service.QueryService.buildGeneFilters;
import static org.icgc.dcc.portal.service.QueryService.buildMutationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildObservationFilters;
import static org.icgc.dcc.portal.service.QueryService.buildPathwayFilters;
import static org.icgc.dcc.portal.service.QueryService.buildProjectFilters;
import static org.icgc.dcc.portal.service.QueryService.getFields;
import static org.icgc.dcc.portal.service.QueryService.hasConsequence;
import static org.icgc.dcc.portal.service.QueryService.hasDonor;
import static org.icgc.dcc.portal.service.QueryService.hasGene;
import static org.icgc.dcc.portal.service.QueryService.hasMutation;
import static org.icgc.dcc.portal.service.QueryService.hasObservation;
import static org.icgc.dcc.portal.service.QueryService.hasPathway;
import static org.icgc.dcc.portal.service.QueryService.hasProject;
import static org.icgc.dcc.portal.service.QueryService.remapD2P;
import static org.icgc.dcc.portal.service.QueryService.remapG2P;
import static org.icgc.dcc.portal.service.QueryService.remapM2C;
import static org.icgc.dcc.portal.service.QueryService.remapM2O;

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
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Component
public class OccurrenceRepository {

  private static final Type CENTRIC_TYPE = Type.OCCURRENCE_CENTRIC;
  private static final Kind KIND = Kind.OCCURRENCE;

  // Type -> ES nested doc
  private static final ImmutableMap<Kind, String> NESTED_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.PROJECT, "project")
      .put(Kind.DONOR, "donor")
      .put(Kind.MUTATION, "ssm")
      .put(Kind.CONSEQUENCE, "ssm.consequence")
      .put(Kind.GENE, "ssm.consequence")
      .put(Kind.PATHWAY, "ssm.consequence.gene.pathways")
      .put(Kind.OBSERVATION, "ssm.observation")
      .build());

  static final ImmutableMap<Kind, String> PREFIX_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<Kind, String> builder()
      .put(Kind.PROJECT, "project")
      .put(Kind.DONOR, "donor")
      .put(Kind.MUTATION, "ssm")
      .put(Kind.CONSEQUENCE, "ssm.consequence")
      .put(Kind.GENE, "ssm.consequence.gene")
      .put(Kind.PATHWAY, "ssm.consequence.gene.pathways")
      .put(Kind.OBSERVATION, "ssm.observation")
      .build());

  private final Client client;
  private final String index;

  @Autowired
  OccurrenceRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  private FilterBuilder getFilters(ObjectNode filters) {
    if (filters.fieldNames().hasNext()) return buildFilters(filters);
    return matchAllFilter();
  }

  private FilterBuilder buildFilters(ObjectNode filters) {
    val qb = FilterBuilders.boolFilter();
    val musts = Lists.<FilterBuilder> newArrayList();

    boolean matchAll = true;
    boolean hasDonor = hasDonor(filters);
    boolean hasProject = hasProject(filters);
    boolean hasGene = hasGene(filters);
    boolean hasPathway = hasPathway(filters);
    boolean hasMutation = hasMutation(filters);
    boolean hasConsequence = hasConsequence(filters);
    boolean hasObservation = hasObservation(filters);

    if (hasProject || hasPathway || hasDonor || hasGene || hasMutation || hasConsequence || hasObservation) {
      matchAll = false;
      if (hasDonor) {
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.DONOR), buildDonorFilters(filters, PREFIX_MAPPING)));
      }
      if (hasProject) {
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.PROJECT), buildProjectFilters(filters, PREFIX_MAPPING)));
      }

      if (hasGene || hasPathway || hasMutation || hasConsequence || hasObservation) {
        val mb = FilterBuilders.boolFilter();
        val mMusts = Lists.<FilterBuilder> newArrayList();
        if (hasMutation) mMusts.add(buildMutationFilters(filters, PREFIX_MAPPING));

        if (hasObservation) {
          log.info("Adding nested observation filter");
          mMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.OBSERVATION),
              buildObservationFilters(filters, PREFIX_MAPPING)));
        }

        if (hasGene || hasPathway || hasConsequence) {
          val nb = FilterBuilders.boolFilter();
          val nMusts = Lists.<FilterBuilder> newArrayList();
          if (hasConsequence) nMusts.add(buildConsequenceFilters(filters, PREFIX_MAPPING));
          if (hasGene) nMusts.add(buildGeneFilters(filters, PREFIX_MAPPING));
          if (hasPathway) nMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.PATHWAY),
              buildPathwayFilters(filters, PREFIX_MAPPING)));
          nb.must(nMusts.toArray(new FilterBuilder[nMusts.size()]));
          mMusts.add(nestedFilter(NESTED_MAPPING.get(Kind.CONSEQUENCE), nb));
        }
        mb.must(mMusts.toArray(new FilterBuilder[mMusts.size()]));
        musts.add(nestedFilter(NESTED_MAPPING.get(Kind.MUTATION), mb));
      }
      qb.must(musts.toArray(new FilterBuilder[musts.size()]));
    }
    return matchAll ? matchAllFilter() : qb;
  }

  public SearchResponse findAllCentric(Query query) {
    val search = buildFindAllRequest(query, CENTRIC_TYPE);

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    SearchRequestBuilder search =
        client.prepareSearch(index).setTypes(type.getId()).setSearchType(QUERY_THEN_FETCH).setFrom(query.getFrom())
            .setSize(query.getSize()).addSort(FIELDS_MAPPING.get(KIND).get(query.getSort()), query.getOrder());

    ObjectNode filters = remapFilters(query.getFilters());
    log.info("Remapped filters {}", filters);
    search.setFilter(getFilters(filters));

    search.addFields(getFields(query, KIND));
    return search;
  }

  public long count(Query query) {
    SearchRequestBuilder search = buildCountRequest(query, CENTRIC_TYPE);

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  public SearchRequestBuilder buildCountRequest(Query query, Type type) {
    SearchRequestBuilder search = client.prepareSearch(index).setTypes(type.getId()).setSearchType(COUNT);

    if (query.hasFilters()) {
      ObjectNode filters = remapFilters(query.getFilters());
      search.setFilter(getFilters(filters));
    }
    return search;
  }

  public ObjectNode remapFilters(ObjectNode filters) {
    return remapM2O(remapM2C(remapD2P(remapG2P(filters))));
  }

  public Map<String, Object> findOne(String id, Query query) {
    val fieldMapping = FIELDS_MAPPING.get(KIND);
    val fs = Lists.<String> newArrayList();

    GetRequestBuilder search = client.prepareGet(index, CENTRIC_TYPE.getId(), id);

    if (query.hasFields()) {
      for (String field : query.getFields()) {
        if (fieldMapping.containsKey(field)) {
          fs.add(fieldMapping.get(field));
        }
      }
    } else
      fs.addAll(fieldMapping.values());

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
      if (Lists.newArrayList(fieldMapping.get("platform"), fieldMapping.get("consequenceType"),
          fieldMapping.get("verificationStatus"), "transcript", "ssm_occurrence").contains(f.getName())) {
        map.put(f.getName(), f.getValues());
      } else {
        map.put(f.getName(), f.getValue());
      }
    }

    log.debug("{}", map);

    return map;
  }
}
