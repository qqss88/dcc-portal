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
package org.icgc.dcc.portal.service;

import java.util.ArrayList;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.model.UnionUnitWithCount;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.repository.UnionAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class UnionAnalysisService {

  @NonNull
  private final UnionAnalysisRepository repository;

  @NonNull
  private final UnionAnalyzer analyzer;

  public UnionAnalysisResult getAnalysis(
      @NonNull final UUID analysisId) {

    val result = repository.find(analysisId);

    if (null == result) {

      log.info("No analysis is found for id: '{}'.", analysisId);

    } else {

      log.info("Got analysis: '{}'", result);
    }
    // TODO: temp
    return result;
  }

  public UnionAnalysisResult submitAnalysis(
      @NonNull final UnionAnalysisRequest request) {

    val entityType = request.getType();

    val newAnalysis = UnionAnalysisResult.forNewlyCreated(entityType);
    repository.save(newAnalysis);

    val definitions = request.toUnionSets();

    val result = new ArrayList<UnionUnitWithCount>(definitions.size());

    for (val def : definitions) {

      val count = analyzer.getUnionCount(def, entityType);
      result.add(UnionUnitWithCount.copyOf(def, count));
    }
    log.info("Result of Union Analysis is: " + result);

    val updatedAnalysis = UnionAnalysisResult.withResult(newAnalysis.getId(), entityType, result);
    repository.update(updatedAnalysis);

    return newAnalysis;
  }
}
