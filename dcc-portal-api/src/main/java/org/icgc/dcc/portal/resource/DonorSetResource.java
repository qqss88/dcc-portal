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
import static org.icgc.dcc.portal.repository.DonorRepository.DONOR_ID_SEARCH_FIELDS;
import static org.icgc.dcc.portal.repository.DonorRepository.FILE_DONOR_ID_SEARCH_FIELDS;

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
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.UserDonorListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
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
  private final static Splitter splitter = Splitter.on(DONOR_DELIMITERS).omitEmptyStrings();

  private final static Set<String> ICGC_COL_TYPES =
      ImmutableSet.<String> of(
          "_donor_id",
          "specimenIds",
          "sampleIds");

  private final static String ICGC_COL = "icgc";
  private final static String SUBMITTER_COL = "submitter";

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @Timed
  public UploadedDonorSet processDonorSet(
      @ApiParam(value = "The Ids to be saved as a Donor List") @FormParam("donorIds") String donorIds,
      @ApiParam(value = "Validation") @QueryParam("validationOnly") @DefaultValue("false") boolean validationOnly,
      @ApiParam(value = "External Repository") @QueryParam("externalRepo") @DefaultValue("false") boolean externalRepo) {

    val result = findDonorsByIdentifiers(donorIds, externalRepo);
    if (validationOnly) {
      return result;
    }

    val uniqueIds = result.getDonorSet().keySet();
    if (uniqueIds.size() == 0) {
      result.getWarnings().add("Request contains no valid donor Ids");
      return result;
    }

    val id = userDonorListService.save(uniqueIds);
    result.setDonorListId(id.toString());
    return result;
  }

  private UploadedDonorSet findDonorsByIdentifiers(String data, boolean externalRepo) {
    val donorList = new UploadedDonorSet();
    val originalIds = ImmutableList.<String> copyOf(splitter.split(data));
    if (originalIds.size() > MAX_DONOR_LIST_SIZE) {
      log.info("Exceeds maximum size {}", MAX_DONOR_LIST_SIZE);
      donorList.getWarnings().add(
          String.format("Input data exceeds maximum threshold of %s donor identifiers.", MAX_DONOR_LIST_SIZE));
      return donorList;
    }

    val matchIds = ImmutableList.<String> builder();
    for (val id : originalIds) {
      matchIds.add(id.toLowerCase());
    }

    val donorResults = donorService.validateIdentifiers(matchIds.build(), externalRepo);
    log.debug("Search results {}", donorResults);

    // All matched identifiers
    val validIds = Maps.<String, Multimap<String, Donor>> newHashMap();
    val allMatchedIdentifiers = Sets.<String> newHashSet();
    val searchFields = externalRepo ? FILE_DONOR_ID_SEARCH_FIELDS.values() : DONOR_ID_SEARCH_FIELDS.values();
    for (val searchField : searchFields) {
      if (!donorResults.get(searchField).isEmpty()) {
        // Case doesn't matter
        for (val k : donorResults.get(searchField).keySet()) {
          allMatchedIdentifiers.add(k.toLowerCase());
        }
        validIds.put(searchField, donorResults.get(searchField));
      }
    }

    for (val id : originalIds) {
      if (!allMatchedIdentifiers.contains(id.toLowerCase())) {
        donorList.getInvalidIds().add(id);
      }
    }
    donorList.setDonorSet(pivotDonorList(validIds));
    return donorList;
  }

  private Map<String, SetMultimap<String, String>> pivotDonorList(Map<String, Multimap<String, Donor>> validDonors) {
    Map<String, SetMultimap<String, String>> pivotedMap = Maps.newHashMap();

    for (val searchType : validDonors.entrySet()) {
      val key = searchType.getKey();
      val value = searchType.getValue();
      val matchedIds = value.keySet();
      val colName = ICGC_COL_TYPES.contains(key) ? ICGC_COL : SUBMITTER_COL;

      for (val id : matchedIds) {
        val donors = value.get(id);
        for (val donor : donors) {
          if (pivotedMap.containsKey(donor.getId())) {
            pivotedMap.get(donor.getId()).put(colName, id);
          } else {
            SetMultimap<String, String> setMultimap = HashMultimap.create();
            setMultimap.put(colName, id);
            pivotedMap.put(donor.getId(), setMultimap);
          }
        }
      }
    }
    return pivotedMap;
  }

}