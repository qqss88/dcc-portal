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

import static com.google.common.base.Charsets.UTF_8;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.ImmutableList;
import org.icgc.dcc.portal.service.GeneListService;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/*
 * Custom query param that translate context into query filter
 * See: http://avianey.blogspot.ca/2011/12/exception-mapping-jersey.html#contextinjection 
 */
@Provider
@Slf4j
public class QueryFiltersProvider implements Injectable<ObjectNode>, InjectableProvider<QueryFilters, Type> {

  @Context
  private HttpServletRequest request;

  @Inject
  private GeneListService genelistService;

  /*
   * @Inject public QueryFiltersProvider(HttpServletRequest request, GeneListService genelistService) { this.request =
   * request; this.genelistService = genelistService; }
   */

  @Override
  public Injectable<ObjectNode> getInjectable(ComponentContext cc, QueryFilters a, Type c) {
    if (c.equals(ObjectNode.class)) {
      return this;
    }
    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  @SneakyThrows
  public ObjectNode getValue() {
    log.info("Debugging !!! {}", request);

    String filtersStr = request.getParameter("filters");
    String genelistIdStr = request.getParameter("genelist");

    log.info("Debugging !!! {} {} ", filtersStr, genelistIdStr);

    // Default filter
    ObjectNode filters = JsonNodeFactory.instance.objectNode();

    if (filtersStr != null) {
      filters = (ObjectNode) MAPPER.readTree(URLDecoder.decode(filtersStr, UTF_8.name()));
    }

    List<String> genelist = Collections.emptyList();
    if (genelistIdStr != null) {
      // Augment gene list
      // TODO: Split in service
      String blob = genelistService.get(Long.parseLong(genelistIdStr));
      log.info("Gene list blob {}", blob);

      if (blob != null) {
        genelist = ImmutableList.<String> copyOf(blob.split(","));
      }
    }

    List<String> ids = Lists.<String> newLinkedList();

    // Check if there is one
    val node = filters.path("gene").path("id");
    if (node.path("is").isMissingNode() == false) {
      ArrayNode geneIds = (ArrayNode) node.get("is");
      for (val geneId : geneIds) {
        ids.add(geneId.asText());
      }
    }

    // Append
    ids.addAll(genelist);

    // Put it back
    if (ids.size() > 0) {
      filters.with("gene").with("id").remove("is");
      ArrayNode arrayNode = filters.with("gene").with("id").putArray("is");
      for (val id : ids) {
        arrayNode.add(id);
      }
    }
    log.info("Filters : {}", filters);

    return filters;
  }
}
