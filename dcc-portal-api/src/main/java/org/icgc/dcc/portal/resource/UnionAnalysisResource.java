/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_VALUE;

import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.UnionAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * A dropwizard resource endpoint that provides set operation analysis for bench list.
 */

@Slf4j
@Component
@Path("/v1/analysis/union")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class UnionAnalysisResource {

  @NonNull
  private final UnionAnalysisService service;

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an union analysis by id", response = UnionAnalysisResult.class)
  public UnionAnalysisResult getAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) final UUID analysisId) {

    if (null == analysisId) {
      throw new BadRequestException(API_ANALYSIS_ID_PARAM + " is null.");
    }
    log.info("Received request with {} of '{}'", API_ANALYSIS_ID_PARAM, analysisId);

    return service.getAnalysis(analysisId);
  }

  private final static int MIN_ENTITY_PARAM_ARRAY_SIZE = 2;

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an union analysis asynchronously. Status can be retrieved by polling the /{id} GET endpoint.",
      response = UnionAnalysisResult.class)
  public Response sumbitAnalysis(
      final Set<UUID> entityLists) {

    if (null == entityLists ||
        entityLists.size() < MIN_ENTITY_PARAM_ARRAY_SIZE) {

      throw new BadRequestException(
          "The minimum number of UNIQUE entity lists required to perform an union analysis is "
              + MIN_ENTITY_PARAM_ARRAY_SIZE);
    }
    log.info("Received request with entity lists of '{}'", entityLists);

    val newAnalysis = service.submitAnalysis(entityLists);

    return Response.status(ACCEPTED)
        .entity(newAnalysis)
        .build();
  }
}
