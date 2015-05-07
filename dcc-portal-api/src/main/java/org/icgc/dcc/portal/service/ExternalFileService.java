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

import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.ExternalFile;
import org.icgc.dcc.portal.model.ExternalFiles;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.ExternalFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ExternalFileService {

  private final ExternalFileRepository externalFileRepository;

  /*
   * @Autowired public ExternalFileService(ExternalFileRepository externalFileRepository) { this.externalFileRepository
   * = externalFileRepository; }
   */

  public ExternalFiles findAll(Query query) {
    SearchResponse response = externalFileRepository.findAll(query);
    SearchHits hits = response.getHits();

    val list = ImmutableList.<ExternalFile> builder();

    for (SearchHit hit : hits) {
      val fieldMap = createResponseMap(hit, query, Kind.EXTERNAL_FILE);
      list.add(new ExternalFile(fieldMap));
    }

    val externalFiles = new ExternalFiles(list.build());
    externalFiles.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return externalFiles;
  }

}
