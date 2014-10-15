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
package org.icgc.dcc.portal.model;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Set;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.GeneListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Slf4j
@Component
@Provider
// @RequiredArgsConstructor(onConstructor = @_({ @Autowired }))
public class QueryFilterProvider implements InjectableProvider<QueryParam, Parameter> {

  @Context
  private HttpContext context;

  @Autowired
  private GeneListService genelistService;

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable<FiltersParam> getInjectable(ComponentContext ic, QueryParam a, Parameter c) {
    log.info(">>>> {}", c);

    if (FiltersParam.class != c.getParameterClass()) {
      return null;
    }

    return new Injectable<FiltersParam>() {

      @Override
      public FiltersParam getValue() {

        val queryParameters = context.getUriInfo().getQueryParameters();
        val filtersStr = Objects.firstNonNull(queryParameters.getFirst("filters"), "{}");
        val result = new FiltersParam(filtersStr);
        val node = result.get();

        // Extract gene ids from genelist ids, if any
        Set<String> geneListIds = Sets.<String> newHashSet();
        if (!node.path("gene").path("myGeneList").isMissingNode()) {
          log.info("Rewriting filter {}", node);
          val geneListIdArray = (ArrayNode) node.path("gene").path("myGeneList");
          for (val geneListId : geneListIdArray) {
            String blob = genelistService.get(Long.parseLong(geneListId.textValue()));
            if (isNullOrEmpty(blob)) {
              throw new BadRequestException("Cannot find gene list id " + geneListId.textValue());
            }
            for (val geneId : blob.split(",")) {
              log.info("Adding genelist ids");
              geneListIds.add(geneId);
            }
            // geneListIds.addAll(ImmutableList.<String> copyOf(blob.split(",")));
          }
          ((ObjectNode) node.get("gene")).remove("myGeneList");
        } else {
          return result;
        }

        log.info("Gene list IDS {}", geneListIds);
        for (val id : geneListIds) {
          node.with("gene").with("id").withArray("is").add(id);
        }
        log.info("Result filter {} ", node);

        return result;
      }

    };
  }
}
