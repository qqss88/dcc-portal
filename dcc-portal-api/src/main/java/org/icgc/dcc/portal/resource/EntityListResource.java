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
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_DEFINITION_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_ID_VALUE;

import java.util.ArrayList;
import java.util.List;
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

import org.elasticsearch.common.collect.Sets;
import org.icgc.dcc.portal.model.DerivedEntityListDefinition;
import org.icgc.dcc.portal.model.EntityList;
import org.icgc.dcc.portal.model.EntityListDefinition;
import org.icgc.dcc.portal.model.UuidListParam;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.EntityListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiParam;

/**
 * TODO
 */

@Slf4j
@Component
@Path("/v1/entitylist")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EntityListResource {

  @NonNull
  private final EntityListService service;

  private List<EntityList> getEntityListsByIds(final Set<UUID> ids) {

    val result = new ArrayList<EntityList>(ids.size());
    for (val id : ids) {

      val list = service.getEntityList(id);
      if (null != list) {

        result.add(list);

      }
    }
    return result;
  }

  @GET
  @Path("/lists/{" + API_ENTITY_LIST_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  public List<EntityList> getEntityLists(
      @ApiParam(value = API_ENTITY_LIST_ID_VALUE, required = true) @PathParam(API_ENTITY_LIST_ID_PARAM) final UuidListParam entityListIds
      ) {

    Set<UUID> listIds;
    try {

      listIds = entityListIds.get();

    } catch (Exception e) {

      log.info("Unable to parse the incoming UUID list from the request: {}", entityListIds, e.getMessage());
      throw new BadRequestException("Unable to parse the entityListIds parameter.");
    }

    return getEntityListsByIds(listIds);
  }

  @GET
  @Path("/{" + API_ENTITY_LIST_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  // TODO
  public EntityList getEntityList(
      @ApiParam(value = API_ENTITY_LIST_ID_VALUE, required = true) @PathParam(API_ENTITY_LIST_ID_PARAM) final UUID entityListId) {

    val result = getEntityListsByIds(Sets.newHashSet(entityListId));
    if (result.isEmpty()) {

      log.error("Unexpected error: getEntityListsByIds returns empty.");
      throw new BadRequestException("Not found: " + entityListId); // TODO: better message
    }
    else {

      return result.get(0);
    }
  }

  @POST
  // this hits the root path
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response createList(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final EntityListDefinition listDefinition
      ) {

    val newList = service.createEntityList(listDefinition);

    return Response.status(ACCEPTED)
        .entity(newList)
        .build();
  }

  @POST
  @Path("/union")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response deriveList(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final DerivedEntityListDefinition listDefinition
      ) {

    val newList = service.deriveEntityList(listDefinition);

    return Response.status(ACCEPTED)
        .entity(newList)
        .build();
  }
}
