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

package org.icgc.dcc.portal.service;

import static org.elasticsearch.common.base.Throwables.propagate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.Occurrence;
import org.icgc.dcc.portal.model.Occurrences;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.OccurrenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@Slf4j
@Service
public class OccurrenceService {

  private final OccurrenceRepository occurrenceRepository;

  private final Map<String, Map<String, Integer>> projectMutationCache =
      new ConcurrentHashMap<String, Map<String, Integer>>();

  @Autowired
  public OccurrenceService(OccurrenceRepository occurrenceRepository) {
    this.occurrenceRepository = occurrenceRepository;
  }

  @Async
  public void init() {
    try {
      log.info("Caching...");
      projectMutationCache.putAll(occurrenceRepository.getProjectDonorMutationDistribution());
    } catch (Exception e) {
      log.error("Error caching: ", e);

      propagate(e);
    }
  }

  public Occurrences findAll(Query query) {
    SearchResponse response = occurrenceRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    val list = ImmutableList.<Occurrence> builder();

    for (SearchHit hit : hits) {
      Map<String, Object> fieldMap = Maps.newHashMap();
      for (Map.Entry<String, SearchHitField> field : hit.getFields().entrySet()) {
        fieldMap.put(field.getKey(), field.getValue().getValue());
      }
      list.add(new Occurrence(fieldMap));
    }

    val occurrences = new Occurrences(list.build());
    occurrences.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return occurrences;
  }

  public long count(Query query) {
    return occurrenceRepository.count(query);
  }

  public Occurrence findOne(String occurrenceId, Query query) {
    return new Occurrence(occurrenceRepository.findOne(occurrenceId, query));
  }

  public Map<String, Map<String, Integer>> getProjectMutationDistribution() {
    if (projectMutationCache != null) {
      return projectMutationCache;
    }
    return occurrenceRepository.getProjectDonorMutationDistribution();
  }
}
