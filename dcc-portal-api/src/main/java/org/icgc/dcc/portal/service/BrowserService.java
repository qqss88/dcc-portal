/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal.service;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.common.core.model.ChromosomeLocation;
import org.icgc.dcc.portal.browser.ds.GeneParser;
import org.icgc.dcc.portal.browser.ds.MutationParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class BrowserService {

  private final MutationParser mutationParser;
  private final GeneParser geneParser;

  private static final class ParameterNames {

    private static final String SEGMENT = "segment";
    private static final String RESOURCE = "resource";
  }

  /**
   * Retrieves histogram.
   */
  public List<Object> getHistogram(Map<String, String> queryMap) {

    val segmentRegion = queryMap.get(ParameterNames.SEGMENT);
    ChromosomeLocation chromosome = null;
    try {
      chromosome = ChromosomeLocation.parse(segmentRegion);
    } catch (Exception e) {
      val message = "Value of the '" + ParameterNames.SEGMENT +
          "' parameter (" + segmentRegion + ") is not valid. Reason: " + e.getMessage();
      throw new BadRequestException(message);
    }

    if (queryMap.get(ParameterNames.RESOURCE).equals("mutation")) {
      return getHistogramSegmentMutation(chromosome.getChromosome().getName(),
          Long.valueOf(chromosome.getStart()),
          Long.valueOf(chromosome.getEnd()),
          queryMap);
    } else if (queryMap.get(ParameterNames.RESOURCE).equals("gene")) {
      return getHistogramSegmentGene(chromosome.getChromosome().getName(),
          Long.valueOf(chromosome.getStart()),
          Long.valueOf(chromosome.getEnd()),
          queryMap);
    } else {
      throw new IllegalArgumentException("Invalid Resource: " + queryMap.get(ParameterNames.RESOURCE));
    }
  }

  /**
   * Retrieves complete data.
   */
  @SneakyThrows
  public List<List<Object>> getRecords(Map<String, String> queryMap) {

    val segmentRegion = queryMap.get(ParameterNames.SEGMENT);
    List<List<Object>> result = newArrayList();

    for (val chromosomeString : segmentRegion.split(",")) {
      ChromosomeLocation chromosome = null;

      try {
        chromosome = ChromosomeLocation.parse(chromosomeString);
      } catch (Exception e) {
        val message = "Value of the '" + ParameterNames.SEGMENT +
            "' parameter (" + segmentRegion + ") is not valid. Reason: " + e.getMessage();
        throw new BadRequestException(message);
      }

      if (queryMap.get(ParameterNames.RESOURCE).equals("mutation")) {
        result.add(getSegmentMutation(chromosome.getChromosome().getName(),
            Long.valueOf(chromosome.getStart()),
            Long.valueOf(chromosome.getEnd()),
            queryMap));
      } else if (queryMap.get(ParameterNames.RESOURCE).equals("gene")) {
        result.add(getSegmentGene(chromosome.getChromosome().getName(),
            Long.valueOf(chromosome.getStart()),
            Long.valueOf(chromosome.getEnd()),
            queryMap));
      } else {
        throw new IllegalArgumentException("Invalid Resource: " + queryMap.get(ParameterNames.RESOURCE));
      }
    }

    return result;
  }

  private List<Object> getSegmentMutation(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String consequenceValue = queryMap.get("consequence_type");
    List<String> consequenceTypes = consequenceValue != null ? Arrays.asList(consequenceValue.split(",")) : null;

    String projectFilterValue = queryMap.get("consequence_type");
    List<String> projectFilters = projectFilterValue != null ? Arrays.asList(projectFilterValue.split(",")) : null;

    return mutationParser.parse(segmentId, start, stop, consequenceTypes, projectFilters);
  }

  private List<Object> getHistogramSegmentMutation(String segmentId, Long start, Long stop,
      Map<String, String> queryMap) {
    String consequenceValue = queryMap.get("consequence_type");
    List<String> consequenceTypes = consequenceValue != null ? Arrays.asList(consequenceValue.split(",")) : null;

    String projectFilterValue = queryMap.get("consequence_type");
    List<String> projectFilters = projectFilterValue != null ? Arrays.asList(projectFilterValue.split(",")) : null;

    String intervalValue = queryMap.get("interval");
    Long interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : null;

    return mutationParser.parseHistogram(segmentId, start, stop, interval, consequenceTypes, projectFilters);
  }

  private List<Object> getSegmentGene(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String biotypeValue = queryMap.get("biotype");
    List<String> biotypes = biotypeValue != null ? Arrays.asList(biotypeValue.split(",")) : null;

    val withTranscripts = nullToEmpty(queryMap.get("dataType")).equals("withTranscripts");

    return geneParser.parse(segmentId, start, stop, biotypes, withTranscripts);
  }

  private List<Object> getHistogramSegmentGene(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    String biotypeValue = queryMap.get("biotype");
    List<String> biotypes = biotypeValue != null ? Arrays.asList(biotypeValue.split(",")) : null;

    String intervalValue = queryMap.get("interval");
    Long interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : null;

    return geneParser.parseHistogram(segmentId, start, stop, interval, biotypes);
  }

}