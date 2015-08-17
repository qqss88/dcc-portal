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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.icgc.dcc.portal.model.Donor;
import org.icgc.dcc.portal.model.UploadedDonorSet;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.UserDonorListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Path("/v1/donorsets")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class DonorSetResource {

  @NonNull
  private final UserDonorListService userDonorListService;

  @NonNull
  private final DonorService donorService;

  // Spaces, tabs, commas, or new lines
  private final static Pattern DONOR_DELIMITERS = Pattern.compile("[, \t\r\n]");
  private final static int MAX_DONOR_LIST_SIZE = 1000;

  private final static Set<String> ICDC_COL_TYPES =
      ImmutableSet.<String> of(
          "_donor_id",
          "specimenIds",
          "sampleIds");

  private final static String ICDC_COL = "icdc";
  private final static String SUBMITTER_COL = "submitter";

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @Timed
  public UploadedDonorSet processDonorSet(
      @ApiParam(value = "The Ids to be saved as a Donor List") @FormParam("donorIds") String donorIds,
      @ApiParam(value = "Validation") @QueryParam("validationOnly") @DefaultValue("false") boolean validationOnly) {

    val result = findDonorsByIdentifiers(donorIds);

    if (validationOnly) {
      return result;
    } // otherwise we will need to store

    // uniqueIds are provided by the keyset of the pivoted table.
    Set<String> uniqueIds = result.getDonorSet().keySet();

    // Sanity check, we require at least one valid id in order to store
    if (uniqueIds.size() == 0) {
      result.getWarnings().add("Request contains no valid donor Ids");

      return result;
    }

    val id = userDonorListService.save(uniqueIds); // save list for retrieval later

    result.setDonorListId(id.toString()); // return the id to client

    return result;

  }

  private UploadedDonorSet findDonorsByIdentifiers(String data) {
    val donorList = new UploadedDonorSet();

    val splitter = Splitter.on(DONOR_DELIMITERS).omitEmptyStrings();
    val originalIds = ImmutableList.<String> copyOf(splitter.split(data));
    val matchIds = ImmutableList.<String> builder();
    val validIds = Maps.<String, Multimap<String, Donor>> newHashMap();

    if (originalIds.size() > MAX_DONOR_LIST_SIZE) {
      log.info("Exceeds maximum size {}", MAX_DONOR_LIST_SIZE);
      donorList.getWarnings().add(
          String.format("Input data exceeds maximum threshold of %s donor identifiers.", MAX_DONOR_LIST_SIZE));
      return donorList;
    }

    for (val id : originalIds) {
      matchIds.add(id.toLowerCase());
    }

    val donorTextResults = donorService.validateIdentifiers(matchIds.build(), false);
    log.debug("Search results {}", donorTextResults);

    // All matched identifiers
    val allMatchedIdentifiers = Sets.<String> newHashSet();
    for (val searchField : DonorRepository.DONOR_ID_SEARCH_FIELDS.values()) {
      if (!donorTextResults.get(searchField).isEmpty()) {

        // Case doesn't matter
        for (val k : donorTextResults.get(searchField).keySet()) {
          allMatchedIdentifiers.add(k.toLowerCase());
        }

        validIds.put(searchField, donorTextResults.get(searchField));
      }
    }

    val fileDonorTextResults = donorService.validateIdentifiers(matchIds.build(), true);
    log.debug("Search results {}", fileDonorTextResults);
    for (val searchField : DonorRepository.FILE_DONOR_ID_SEARCH_FIELDS.values()) {
      if (!fileDonorTextResults.get(searchField).isEmpty()) {

        // Case doesn't matter
        for (val k : fileDonorTextResults.get(searchField).keySet()) {
          allMatchedIdentifiers.add(k.toLowerCase());
        }

        validIds.put(searchField, fileDonorTextResults.get(searchField));
      }
    }

    // Construct valid and invalid donor matches
    for (val id : originalIds) {
      if (!allMatchedIdentifiers.contains(id.toLowerCase())) {
        donorList.getInvalidIds().add(id);
      }
    }

    donorList.setDonorSet(pivotDonorList(validIds));
    return donorList;
  }

  private Map<String, Map<String, Set<String>>> pivotDonorList(Map<String, Multimap<String, Donor>> validDonors) {
    Map<String, Map<String, Set<String>>> pivotedMap = Maps.<String, Map<String, Set<String>>> newHashMap();

    // iterate across all field types that matched
    for (val searchType : validDonors.entrySet()) {
      val key = searchType.getKey();
      val value = searchType.getValue();

      // check if it belongs in the ICDC column
      if (ICDC_COL_TYPES.contains(key)) {

        val matchedIds = value.keySet();

        for (val id : matchedIds) {
          val donors = value.get(id); // this returns a collection but almost surely of size 1
          for (val donor : donors) {

            if (pivotedMap.containsKey(donor.getId())) { // donor occured already
              pivotedMap.get(donor.getId()).get(ICDC_COL).add(id);
            } else { // donor has not occured yet
              val colMap = Maps.<String, Set<String>> newHashMap();
              colMap.put(ICDC_COL, new HashSet<String>());
              colMap.put(SUBMITTER_COL, new HashSet<String>());
              colMap.get(ICDC_COL).add(id);
              pivotedMap.put(donor.getId(), colMap);
            }

          }
        }

      } else { // otherwise belongs in the submitter column

        val matchedIds = value.keySet();

        for (val id : matchedIds) {
          val donors = value.get(id); // this returns a collection but almost surely of size 1
          for (val donor : donors) {

            if (pivotedMap.containsKey(donor.getId())) { // donor occured already
              pivotedMap.get(donor.getId()).get(SUBMITTER_COL).add(id);
            } else { // donor has not occured yet
              val colMap = Maps.<String, Set<String>> newHashMap();
              colMap.put(ICDC_COL, new HashSet<String>());
              colMap.put(SUBMITTER_COL, new HashSet<String>());
              colMap.get(SUBMITTER_COL).add(id);
              pivotedMap.put(donor.getId(), colMap);
            }

          }
        }

      }

    }

    return pivotedMap;

  }

}