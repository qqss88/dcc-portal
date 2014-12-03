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

package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.EXECUTING;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_PARAMS_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_PARAMS_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_FIELD;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_VALUE;

import java.util.UUID;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.ParamsParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.EnrichmentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Slf4j
@Component
@Path("/analysis/enrichment")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EnrichmentAnalysisResource {

  @NonNull
  private final EnrichmentAnalysisService service;

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @ApiOperation(value = "Retrieves an enrichment analysis by id", response = EnrichmentAnalysis.class)
  public EnrichmentAnalysis getAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) UUID analysisId) {
    log.info("Getting analysis with id '{}'...", analysisId);
    return service.getAnalysis(analysisId);
  }

  @POST
  @ApiOperation(value = "Submits an enrichment analysis request", response = EnrichmentAnalysis.class)
  public EnrichmentAnalysis submitAnalysis(
      @ApiParam(value = API_PARAMS_VALUE) @FormParam(API_PARAMS_PARAM) ParamsParam paramsParam,
      @ApiParam(value = API_FILTER_VALUE) @FormParam(API_FILTER_PARAM) FiltersParam filtersParam,
      @ApiParam(value = API_SORT_VALUE) @FormParam(API_SORT_FIELD) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @FormParam(API_ORDER_PARAM) String order
      ) {

    // TODO: Add @FormParam version of ExpandingFilterParamsProvider (type hierarchy?)

    // Construct
    val query = Query.builder().filters(filtersParam.get()).sort(sort).order(order).defaultLimit(10).build();
    val params = paramsParam.get();
    val analysis = new EnrichmentAnalysis()
        .setState(EXECUTING)
        .setQuery(query)
        .setParams(params);

    log.info("Submitting analysis '{}'...", analysis);
    service.submitAnalysis(analysis);

    return analysis;
  }

}
