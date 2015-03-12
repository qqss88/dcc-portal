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
package org.dcc.portal.pql.convert;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.convert.model.JqlEntry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Converts JSON-like queries to PQL ones.
 */
@Slf4j
public class Jql2PqlConverter {

  private final ObjectMapper mapper = createObjectMapper();

  @SneakyThrows
  public String convert(@NonNull String jql) {
    val entry = mapper.readValue(jql, JqlEntry.class);
    log.debug("Parsed Entry: {}", entry);

    return entry.toPql();
  }

  private ObjectMapper createObjectMapper() {
    return registerJqlDeserializer(new ObjectMapper());
  }

  private ObjectMapper registerJqlDeserializer(ObjectMapper mapper) {
    val module = new SimpleModule();
    module.addDeserializer(JqlEntry.class, new JqlDeserializer());
    mapper.registerModule(module);

    return configureMapper(mapper);
  }

  private ObjectMapper configureMapper(ObjectMapper mapper) {
    return mapper;
  }

}
