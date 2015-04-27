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
package org.icgc.dcc.portal.resource;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.primitives.Doubles.tryParse;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_ORDER;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.DiagramProtein;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.DiagramService;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.OccurrenceService;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Path("/v1/ui")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UIResource {

  protected static final String DEFAULT_FILTERS = "{}";
  private final DonorService donorService;
  private final OccurrenceService occurrenceService;
  private final DiagramService diagramService;

  /*
   * This is used to fetch project-donorCount breakdown for a list of genes. It builds the data for gene chart on the
   * projects page.
   * 
   * gene1: [ proj1: K11, proj2: K12 ... projN:K1N ] ... geneM: [ proj1: KM1, proj2: KM2 ... projN:KMN ]
   * 
   * FIXME: Checkout elasticsearch aggregation framework when we have it to see if it can alleviate the amount of
   * requests, which is based on # of genes passed in.
   */
  @Path("/gene-project-donor-counts/{geneIds}")
  @GET
  public Map<String, TermFacet> countProjectDonor(
      @ApiParam(value = "Gene ID. Multiple IDs can be entered as ENSG00000155657,ENSG00000141510", required = true) @PathParam("geneIds") IdsParam geneIds,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters
      ) {

    val result = Maps.<String, TermFacet> newHashMap();

    for (val geneId : geneIds.get()) {
      val geneFilter = new FiltersParam(String.format("{gene:{id:{is:[\"%s\"]}}}", geneId));
      val filterNode = JsonUtils.merge(filters.get(), geneFilter.get());

      Donors donors = donorService.findAllCentric(Query.builder()
          .filters(filterNode)
          .sort("id")
          .order(DEFAULT_ORDER)
          .fields(Collections.<String> emptyList())
          .includes(ImmutableList.of("facets"))
          .size(0).build());
      result.put(geneId, donors.getFacets().get("projectId"));
    }

    log.debug("geneProjectDonorCounts {}", result);
    return result;
  }

  @Path("/file")
  @Consumes(MULTIPART_FORM_DATA)
  @POST
  @Timed
  public Map<String, String> uploadIds(
      @FormDataParam("filepath") InputStream inputStream,
      @FormDataParam("filepath") FormDataContentDisposition fileDetail) throws Exception {

    log.info("Input stream {}", inputStream);
    log.info("Content disposition {}", fileDetail);

    String content = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));

    return ImmutableMap.<String, String> of("data", content);
  }

  @Path("/projects/donor-mutation-counts")
  @GET
  public Map<String, Map<String, Integer>> getProjectDonorMutation() {
    return occurrenceService.getProjectMutationDistribution();
  }

  @Path("/reactome/protein-map")
  @GET
  public Map<String, DiagramProtein> getReactomeProteinMap(
      @ApiParam(value = "A pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId) {
    if (isValidPathwayId(pathwayId)) {
      throw new BadRequestException("Pathway id '" + pathwayId + "' is empty or not valid");
    }

    return diagramService.mapProteinIds(pathwayId);
  }

  @Path("/reactome/pathway-diagram")
  @GET
  @Produces(APPLICATION_XML)
  public Response getReactomePathwayDiagram(
      @ApiParam(value = "A pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId) {
    if (isValidPathwayId(pathwayId)) {
      throw new BadRequestException("Pathway id '" + pathwayId + "' is empty or not valid");
    }

    return Response.ok(diagramService.getPathwayDiagramString(pathwayId), APPLICATION_XML).build();
  }

  @Path("/reactome/pathway-sub-diagram")
  @GET
  public List<String> getShownPathwaySection(
      @ApiParam(value = "A non-diagrammed pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId) {
    if (isValidPathwayId(pathwayId)) {
      throw new BadRequestException("Pathway id '" + pathwayId + "' is empty or not valid");
    }

    return diagramService.getShownPathwaySection(pathwayId);
  }

  private Boolean isValidPathwayId(String id) {
    return isNullOrEmpty(id) || !(id.startsWith("REACT_") && tryParse(id.substring(6)) != null);
  }

}
