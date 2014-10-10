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
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.service.GeneListService;
import org.icgc.dcc.portal.service.GeneService;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

@Path("/v1/genelist")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
@Slf4j
public class GeneListResource {

  private final GeneListService geneListService;
  private final GeneService geneService;

  @Path("/{genelistIds}")
  @GET
  public List<Map<String, String>> getGeneList(
      @PathParam("genelistIds") IdsParam geneListIds
      ) {

    val result = Lists.<Map<String, String>> newLinkedList();
    for (val id : geneListIds.get()) {
      val geneListStr = geneListService.get(Long.parseLong(id));

      if (isNullOrEmpty(geneListStr)) {
        result.add(ImmutableMap.<String, String> of(id, ""));
      } else {
        result.add(ImmutableMap.<String, String> of(id, geneListService.get(Long.parseLong(id))));
      }
    }
    return result;
  }

  // @Path("/genelist")
  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Timed
  public Map<String, String> saveGeneList(
      @ApiParam(value = "The contents to be parsed and verified") @FormParam("geneIds") String geneIds) {
    long id = geneListService.insert(geneIds);
    return ImmutableMap.<String, String> of("id", "" + id);
  }

  @Path("/validate")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST
  @Timed
  public Map<String, List<String>> findGenesByIdentifiers(
      @ApiParam(value = "The contents to be parsed and verified") @FormParam("geneIdentifiers") String geneIdentifiers) {

    log.info("!!! Form content {}", geneIdentifiers);

    // Spaces, tabs, commas, or new lines
    val delimiters = Pattern.compile("[, \t\r\n]");
    val splitter = Splitter.on(delimiters).omitEmptyStrings();
    val ids = ImmutableList.<String> copyOf(splitter.split(geneIdentifiers));

    log.info("Sending {} gene identifiers to be verified.", ids.size());
    val validResults = geneService.validateIdentifiers(ids);

    val result = Maps.<String, List<String>> newHashMap();
    for (String id : ids) {
      if (validResults.containsKey(id)) {
        result.put(id, ImmutableList.copyOf(validResults.get(id)));
      } else {
        result.put(id, Collections.<String> emptyList());
      }
    }
    return result;
  }

}
