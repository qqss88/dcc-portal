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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.icgc.dcc.portal.model.UploadedDonorList;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.service.DonorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Path("/v1/donorlists")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class DonorListResource {

  @NonNull
  private final DonorService donorService;

  // Spaces, tabs, commas, or new lines
  private final static Pattern DONOR_DELIMITERS = Pattern.compile("[, \t\r\n]");
  private final static int MAX_DONOR_LIST_SIZE = 1000;

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @Timed
  public UploadedDonorList processGeneList(
      @ApiParam(value = "The Ids to be saved as a Donor List") @FormParam("donorIds") String donorIds,
      @ApiParam(value = "Validation") @QueryParam("validationOnly") @DefaultValue("false") boolean validationOnly) {

    val result = findDonorsByIdentifiers(donorIds);

    return result;
  }

  private UploadedDonorList findDonorsByIdentifiers(String data) {
    val donorList = new UploadedDonorList();

    val splitter = Splitter.on(DONOR_DELIMITERS).omitEmptyStrings();
    val originalIds = ImmutableList.<String> copyOf(splitter.split(data));
    val matchIds = ImmutableList.<String> builder();

    if (originalIds.size() > MAX_DONOR_LIST_SIZE) {
      log.info("Exceeds maximum size {}", MAX_DONOR_LIST_SIZE);
      donorList.getWarnings().add(
          String.format("Input data exceeds maximum threshold of %s gene identifiers.", MAX_DONOR_LIST_SIZE));
      return donorList;
    }

    for (val id : originalIds) {
      matchIds.add(id.toLowerCase());
    }

    val validResults1 = donorService.validateIdentifiersDonorText(matchIds.build());
    log.debug("Search results {}", validResults1);

    // All matched identifiers
    val allMatchedIdentifiers = Sets.<String> newHashSet();
    for (val searchField : DonorRepository.DONOR_ID_SEARCH_FIELDS.values()) {
      if (!validResults1.get(searchField).isEmpty()) {

        // Case doesn't matter
        for (val k : validResults1.get(searchField).keySet()) {
          allMatchedIdentifiers.add(k.toLowerCase());
        }

        donorList.getValidDonors().put(searchField, validResults1.get(searchField));
      }
    }

    val validResults2 = donorService.validateIdentifiersFileDoner(matchIds.build());

    for (val searchField : DonorRepository.FILE_DONOR_ID_SEARCH_FIELDS.values()) {
      if (!validResults2.get(searchField).isEmpty()) {

        // Case doesn't matter
        for (val k : validResults2.get(searchField).keySet()) {
          allMatchedIdentifiers.add(k.toLowerCase());
        }

        donorList.getValidDonors().put(searchField, validResults2.get(searchField));
      }
    }

    // Construct valid and invalid gene matches
    for (val id : originalIds) {
      if (!allMatchedIdentifiers.contains(id.toLowerCase())) {
        donorList.getInvalidDonors().add(id);
      }
    }

    return donorList;
  }

}