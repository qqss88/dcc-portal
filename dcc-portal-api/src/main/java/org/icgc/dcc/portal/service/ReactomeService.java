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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

@Slf4j
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
              map.put(values[0], values[1]);
              return true;
            }

            @Override
            public Boolean getResult() {
              return true;
            }
          });
    } catch (MalformedURLException e) {
      log.error("Error getting Reactome Protein list");
      e.printStackTrace();
    } catch (IOException e) {
      log.error("Error getting Reactome Protein list");
      e.printStackTrace();
    }
    return map;
  }

  public Map<String, String> matchProteinIds(List<String> ids) {
    val proteinMap = getProteinIdMap();
    val map = Maps.<String, String> newHashMap();
    ids.forEach(id -> {
      map.put(id, proteinMap.get(id));
    });
    return map;
  }
}
