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
import static com.google.common.collect.Maps.uniqueIndex;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

@Service
@NoArgsConstructor
public class ReactomeService {

  private static final String REACTOME_ENDPOINT_URL =
      "http://reactomews.oicr.on.ca:8080/ReactomeRESTfulAPI/RESTfulWS/getUniProtRefSeqs";

  @Cacheable("reactomeIds")
  public Map<String, String> getProteinIdMap() {
    val map = Maps.<String, String> newHashMap();
    try {
      Resources.readLines(new URL(REACTOME_ENDPOINT_URL),
          UTF_8, new LineProcessor<Boolean>() {

            @Override
            public boolean processLine(String line) {
              String[] values = line.split("\t");
              map.put(values[0], checkNotNull(parseUniprotId(values[1])));
              return true;
            }

            @Override
            public Boolean getResult() {
              return true;
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Failed to get reactome protein list", e);
    }
    return map;
  }

  private String parseUniprotId(String uniprotId) {
    return uniprotId.split(":")[1];
  }

  public Map<String, String> matchProteinIds(List<String> ids) {
    val proteinMap = getProteinIdMap();
    return uniqueIndex(ids, id -> proteinMap.get(id));
  }
}
