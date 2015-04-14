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

import static org.apache.commons.lang.StringEscapeUtils.unescapeJavaScript;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DiagramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@Service
public class DiagramService {

  private DiagramRepository repo;
  private final ImmutableMap<String, String> INDEX_MODEL = IndexModel.FIELDS_MAPPING.get(Kind.DIAGRAM);

  @Autowired
  public DiagramService(Client client, IndexModel index) {
    repo = new DiagramRepository(client, index);
  }

  public Map<String, String> mapProteinIds(@NonNull List<String> proteinUniprotIds, @NonNull String pathwayId) {
    val proteinMap = getProteinIdMap(pathwayId);
    val map = Maps.<String, String> newHashMap();
    proteinUniprotIds.forEach(id -> map.put(id, proteinMap.get(id)));

    return map;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getProteinIdMap(@NonNull String pathwayId) {
    return (Map<String, String>) getPathway(pathwayId).get(INDEX_MODEL.get("proteinMap"));
  }

  public String getPathwayDiagramString(@NonNull String pathwayId) {
    // TODO fix the escaping... (it probably wont work right now)
    return unescapeJavaScript(getPathway(pathwayId).get(INDEX_MODEL.get("xml")).toString());
  }

  public String[] getShownPathwaySection(@NonNull String pathwayId) {
    return getPathway(pathwayId).get(INDEX_MODEL.get("highlights")).toString().split(",");
  }

  private Map<String, Object> getPathway(String id) {
    val query = Query.builder().build();
    return repo.findOne(id, query);
  }

}
