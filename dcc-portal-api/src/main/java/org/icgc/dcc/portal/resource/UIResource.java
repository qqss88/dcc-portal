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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.ImmutableList;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.util.JsonUtils;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

@Slf4j
@Path("/v1/ui")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class UIResource {

  protected static final String DEFAULT_FILTERS = "{}";

  private final DonorService donorService;

  @Path("/geneProjectDonorCounts/{geneIds}")
  @GET
  // This is used to build the genes-projects-donors breakdown
  public Map<String, TermFacet> countProjectDonor(
      @ApiParam(value = "Gene ID. Multiple IDs can be entered as ENSG00000155657,ENSG00000141510", required = true) @PathParam("geneIds") IdsParam geneIds,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters
      ) {

    Map<String, TermFacet> result = Maps.newHashMap();
    List<String> includes = ImmutableList.of("facets");

    for (String geneId : geneIds.get()) {

      val geneFilter = new FiltersParam(String.format("{gene:{id:{is:[\"%s\"]}}}", geneId));
      val filterNode = JsonUtils.merge(filters.get(), geneFilter.get());

      Donors donors = donorService.findAllCentric(Query.builder()
          .filters(filterNode)
          .fields(Lists.<String> newArrayList())
          .includes(includes)
          .from(1)
          .sort("id")
          .order("desc")
          .size(0).build());
      result.put(geneId, donors.getFacets().get("projectId"));
    }

    log.debug("Result {}", result);
    return result;
  }
}
