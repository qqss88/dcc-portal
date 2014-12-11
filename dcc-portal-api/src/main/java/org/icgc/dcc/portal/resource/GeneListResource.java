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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.FormatUtils._;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.UploadedGeneList;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.UserGeneSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

@Slf4j
@Component
@Path("/v1/genelists")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @_({ @Autowired }))
public class GeneListResource {

  @NonNull
  private final UserGeneSetService userGeneSetService;
  @NonNull
  private final GeneService geneService;

  // Spaces, tabs, commas, or new lines
  private final static Pattern GENE_DELIMITERS = Pattern.compile("[, \t\r\n]");
  private final static int MAX_GENE_LIST_SIZE = 1000;

  @Path("/{genelistIds}")
  @GET
  public List<Map<String, String>> getGeneList(@PathParam("genelistIds") IdsParam geneListIds) {
    val result = Lists.<Map<String, String>> newLinkedList();
    for (val value : geneListIds.get()) {
      val id = UUID.fromString(value);
      val geneList = userGeneSetService.get(id);

      if (isNullOrEmpty(geneList)) {
        result.add(ImmutableMap.of(value, ""));
      } else {
        result.add(ImmutableMap.of(value, geneList));
      }
    }

    return result;
  }

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @Timed
  public UploadedGeneList processGeneList(
      @ApiParam(value = "The Ids to be saved as a Gene List") @FormParam("geneIds") String geneIds,
      @ApiParam(value = "Validation") @QueryParam("validationOnly") @DefaultValue("false") boolean validationOnly
      ) {

    val result = findGenesByIdentifiers(geneIds);

    if (validationOnly) {
      return result;
    }

    // Extract the set of unique ensembl ids for storage
    Set<String> uniqueIds = Sets.<String> newHashSet();
    for (val searchField : GeneRepository.GENE_ID_SEARCH_FIELDS) {
      if (result.getValidGenes().containsKey(searchField)) {
        for (val gene : result.getValidGenes().get(searchField).values()) {
          uniqueIds.add(gene.getId());
        }
      }
    }

    // Sanity check, we require at least one valid id in order to store
    if (uniqueIds.size() == 0) {
      result.getWarnings().add("Request contains no valid gene Ids");

      return result;
    }

    val id = userGeneSetService.save(uniqueIds);

    result.setGeneListId(id.toString());

    return result;
  }

  private UploadedGeneList findGenesByIdentifiers(String data) {

    val geneList = new UploadedGeneList();

    val splitter = Splitter.on(GENE_DELIMITERS).omitEmptyStrings();
    val originalIds = ImmutableList.<String> copyOf(splitter.split(data));
    val matchIds = ImmutableList.<String> builder();

    if (originalIds.size() > MAX_GENE_LIST_SIZE) {
      log.info("Exceeds maximum size {}", MAX_GENE_LIST_SIZE);
      geneList.getWarnings().add(
          _("Input data exceeds maximum threshold of %s gene identifiers.", MAX_GENE_LIST_SIZE));
      return geneList;
    }

    for (val id : originalIds) {
      matchIds.add(id.toUpperCase());
    }
    val validResults = geneService.validateIdentifiers(matchIds.build());

    log.debug("Search results {}", validResults);

    // All matched identifiers
    val allMatchedIdentifiers = Sets.<String> newHashSet();
    for (val searchField : GeneRepository.GENE_ID_SEARCH_FIELDS) {
      if (!validResults.get(searchField).isEmpty()) {
        allMatchedIdentifiers.addAll(validResults.get(searchField).keySet());
        geneList.getValidGenes().put(searchField, validResults.get(searchField));
      }
    }

    // Construct valid and invalid gene matches
    for (val id : originalIds) {
      if (!allMatchedIdentifiers.contains(id.toUpperCase())) {
        geneList.getInvalidGenes().add(id);
      }
    }

    return geneList;
  }

}
