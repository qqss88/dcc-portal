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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Resources.readLines;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.io.LineProcessor;

@Service
@NoArgsConstructor
public class ReactomeService {

  private static final String REACTOME_PROTEIN_ENDPOINT_URL =
      "http://reactomews.oicr.on.ca:8080/ReactomeRESTfulAPI/RESTfulWS/getUniProtRefSeqs";
  private static final String REACTOME_PATHWAY_ENDPOINT_URL =
      "http://reactomews.oicr.on.ca:8080/ReactomeRESTfulAPI/RESTfulWS/pathwayDiagram/%s/XML";
  private static final String REACTOME_QUERY_BY_ID_ENDPOINT_URL =
      "http://reactomews.oicr.on.ca:8080/ReactomeRESTfulAPI/RESTfulWS/queryById/Pathway/%s";

  @Cacheable("reactomeIds")
  public Map<String, String> getProteinIdMap() {
    try {
      return readLines(new URL(REACTOME_PROTEIN_ENDPOINT_URL),
          UTF_8, new LineProcessor<Map<String, String>>() {

            Map<String, String> map = newHashMap();

            @Override
            public boolean processLine(String line) {
              String[] values = line.split("\t");
              val dbId = values[0];
              map.put(dbId, checkNotNull(parseUniprotId(values[1])));
              return true;
            }

            @Override
            public Map<String, String> getResult() {
              return map;
            }

          });
    } catch (IOException e) {
      throw new RuntimeException("Failed to get reactome protein list", e);
    }
  }

  private String parseUniprotId(String uniprotId) {
    return uniprotId.split(":")[1];
  }

  public Map<String, String> matchProteinIds(List<String> ids) {
    val proteinMap = getProteinIdMap();
    val map = Maps.<String, String> newHashMap();
    ids.forEach(id -> map.put(id, proteinMap.get(id)));

    return map;
  }

  public InputStream getPathwayStream(String id) {
    try {
      val url = new URL(String.format(REACTOME_PATHWAY_ENDPOINT_URL, getStableId(id)));
      val connection = url.openConnection();
      connection.setRequestProperty(ACCEPT, "*/*");

      return (InputStream) connection.getContent();
    } catch (IOException e) {
      throw new RuntimeException("Failed to get pathway diagram from reactome", e);
    }
  }

  @SneakyThrows
  private String getStableId(String id) {
    val tree = DEFAULT.readTree(new URL(String.format(REACTOME_QUERY_BY_ID_ENDPOINT_URL, id)));
    return tree.path("dbId").asText();
  }

}