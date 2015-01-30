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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.commons.validator.routines.IntegerValidator;
import org.icgc.dcc.portal.model.BeaconResponse;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.BeaconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Api(value = "/beacon", description = "Answers the question have you observed this genotype?")
@Path("/v1/beacon/query")
@RequiredArgsConstructor(onConstructor = @_({ @Autowired }))
public class BeaconResource extends BaseResource {

  private final BeaconService beaconService;
  private final ImmutableMap<String, Integer> CHR_LENGTHS =
      new ImmutableMap.Builder<String, Integer>()
          .put("1", 249250621).put("2", 243199373).put("3", 198022430).put("4", 191154276).put("5", 180915260)
          .put("6", 171115067).put("7", 159138663).put("8", 146364022).put("9.", 141213431).put("10", 135534747)
          .put("11", 135006516).put("12", 133851895).put("13", 115169878).put("14", 107349540).put("15", 102531392)
          .put("16", 90354753).put("17", 81195210).put("18", 78077248).put("19", 59128983).put("20", 63025520)
          .put("21", 48129895).put("22", 51304566).put("X", 155270560).put("Y", 59373566).put("MT", 16569).build();

  @GET
  @ApiOperation(value = "Beacon", nickname = "Beacon", response = BeaconResponse.class)
  @Consumes("application/x-www-form-urlencoded")
  @Produces(APPLICATION_JSON)
  @Timed
  public BeaconResponse query(

      @ApiParam(value = "Chromosome ID: 1-22, X, Y, MT", required = true) @QueryParam("chromosome") String chromosome,

      @ApiParam(value = "Coordinate (0-based)", required = true) @QueryParam("position") IntParam position,

      @ApiParam(value = "Genome ID: GRCh?", required = true) @QueryParam("reference") String reference,

      @ApiParam(value = "Alleles: [ACTG]+, D, I") @QueryParam("allele") String allele

      ) {
    // Validate
    if (!isValidChromosome(chromosome)) {
      throw new BadRequestException("'chromosome' is empty or invalid (must be 1-22, X, Y or MT)");
    } else if (!isValidPosition(position.get(), chromosome)) {
      throw new BadRequestException("'position' is missing, invalid or exceeds chromosome size");
    } else if (!isValidReference(reference)) {
      throw new BadRequestException("'reference' is empty or invalid (must be GRCh?)");
    } else if (Strings.isNullOrEmpty(allele)) {
      allele = "ANY"; // wildcard, can mean anything
    } else if (!isValidAllele(allele)) {
      throw new BadRequestException("'allele' is invalid (must be [ACTG]+, D or I)");
    }

    return beaconService.query(chromosome.trim(), position.get(), reference.trim(), allele.trim());
  }

  private Boolean isValidChromosome(String chromosome) {
    if (Strings.isNullOrEmpty(chromosome)) {
      return false;
    }
    chromosome = chromosome.trim();
    val validator = IntegerValidator.getInstance();
    if (validator.isValid(chromosome)) {
      return validator.isInRange(Integer.parseInt(chromosome), 1, 22);
    } else {
      return chromosome.equals("X") || chromosome.equals("Y") || chromosome.equals("MT");
    }
  }

  private Boolean isValidReference(String ref) {
    if (Strings.isNullOrEmpty(ref)) {
      return false;
    }
    ref = ref.trim();
    val validator = IntegerValidator.getInstance();
    return ref.startsWith("GRCh") && validator.isValid(ref.substring(4));
  }

  private Boolean isValidAllele(String allele) {
    allele.trim();
    return allele.matches("^[ACTG]+") || allele.equals("D") || allele.equals("I");
  }

  private Boolean isValidPosition(int pos, String chr) {
    return pos >= 0 && CHR_LENGTHS.get(chr) >= pos;
  }

}
