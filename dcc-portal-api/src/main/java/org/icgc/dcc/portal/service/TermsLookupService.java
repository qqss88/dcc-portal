/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static lombok.AccessLevel.PRIVATE;
import static org.elasticsearch.index.query.FilterBuilders.termsLookupFilter;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.TermsLookupFilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Term lookup services
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TermsLookupService {

  /**
   * Constants.
   */
  public static final String TERMS_LOOKUP_PATH = "values";
  public static final String TERMS_LOOKUP_INDEX_NAME = "terms-lookup";

  /**
   * Dependencies.
   */
  @NonNull
  private final Client client;

  @PostConstruct
  public void init() {
    val indexName = TERMS_LOOKUP_INDEX_NAME;
    val index = client.admin().indices();

    log.info("Checking index '{}' for existence...", indexName);
    boolean exists = index.prepareExists(indexName)
        .execute()
        .actionGet()
        .isExists();

    if (exists) {
      log.info("Index '{}' exists. Nothing to do.", indexName);
      return;
    }

    try {
      log.info("Creating index '{}'...", indexName);
      checkState(index
          .prepareCreate(indexName)
          .setSettings(createSettings())
          .execute()
          .actionGet()
          .isAcknowledged(),
          "Index '%s' creation was not acknowledged!", indexName);

    } catch (Throwable t) {
      propagate(t);
    }
  }

  @SneakyThrows
  private void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Map<String, Object> keyValuePairs) {
    client.prepareIndex(TERMS_LOOKUP_INDEX_NAME, type.getName())
        .setId(id.toString())
        .setSource(keyValuePairs).execute().get();
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values) {
    createTermsLookup(type, id, Collections.singletonMap(TERMS_LOOKUP_PATH, (Object) values));
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values, @NonNull final Map<String, Object> additionalAttributes) {
    additionalAttributes.put(TERMS_LOOKUP_PATH, values);
    createTermsLookup(type, id, additionalAttributes);
  }

  public static TermsLookupFilterBuilder createTermsLookupFilter(@NonNull String fieldName,
      @NonNull TermLookupType type, @NonNull UUID id) {
    val key = id.toString();
    return termsLookupFilter(fieldName)
        // .cacheKey(key)
        .lookupId(key)
        .lookupIndex(TERMS_LOOKUP_INDEX_NAME)
        .lookupType(type.getName())
        .lookupPath(TERMS_LOOKUP_PATH);
  }

  private String createSettings() {
    // Ensure that we fully replicate across cluster
    val settings = MAPPER.createObjectNode();
    settings.put("index.auto_expand_replicas", "0-all");
    settings.put("index.number_of_shards", "1");

    return settings.toString();
  }

  /**
   * Supported index types.
   */
  @Getter
  @RequiredArgsConstructor(access = PRIVATE)
  public enum TermLookupType {

    GENE_IDS("gene-ids"),
    MUTATION_IDS("mutation-ids"),
    DONOR_IDS("donor-ids");

    @NonNull
    private final String name;

  }

}
