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

package org.icgc.dcc.portal.config;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.icgc.dcc.portal.util.VersionUtils.getApiVersion;
import static org.icgc.dcc.portal.util.VersionUtils.getApplicationVersion;
import static org.icgc.dcc.portal.util.VersionUtils.getCommitId;

import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.icgc.dcc.portal.model.Versions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Slf4j
@Lazy
@Configuration
public class SearchConfig {

  @Autowired
  private PortalProperties properties;

  @Bean(destroyMethod = "close")
  public Client client() {
    // TransportClient is thread-safe so @Singleton is appropriate
    val configuration = properties.getElastic();
    val client = new TransportClient();
    for (val nodeAddress : configuration.getNodeAddresses()) {
      client.addTransportAddress(new InetSocketTransportAddress(
          nodeAddress.getHost(),
          nodeAddress.getPort()));
    }

    return client;
  }

  @Bean
  public String indexName() {
    String indexName = properties.getElastic().getIndexName();
    return resolveIndexName(indexName);
  }

  private String resolveIndexName(String indexName) {

    // Get cluster state
    ClusterState clusterState = client().admin().cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState();

    val aliases = clusterState.getMetaData().aliases().get(indexName);
    if (aliases != null) {
      Set<String> indexNames = newHashSet(aliases.keys().toArray(String.class));
      checkState(indexNames.size() == 1, "Expected alias to point to a single index but instead it points to '%s'",
          indexNames);

      String realIndexName = getFirst(indexNames, null);
      log.warn("Redirecting configured index alias of '{}' with real index name '{}'", indexName, realIndexName);
      indexName = realIndexName;
    }

    return indexName;
  }

  @Bean
  public Map<String, String> releaseIndexMetadata() {
    String indexStr = resolveIndexName(properties.getElastic().getIndexName());
    return indexMetadata(indexStr);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, String> indexMetadata(String indexName) {
    // Get cluster state
    ClusterState clusterState = client().admin().cluster()
        .prepareState()
        .setIndices(indexName)
        .execute()
        .actionGet()
        .getState();

    IndexMetaData indexMetaData = clusterState.getMetaData().index(indexName);
    checkState(indexMetaData != null, "Index meta data is null. Ensure that index '%s' exists.", indexName);

    // Get the first mappings. This is arbitrary since they all contain the same metadata
    MappingMetaData mappingMetaData = indexMetaData.getMappings().values().iterator().next().value;

    Map<String, Object> source = mappingMetaData.sourceAsMap();
    Map<String, String> meta = (Map<String, String>) source.get("_meta");
    if (meta == null) {
      meta = newHashMap();
    }

    return meta;
  }

  @Bean
  public Versions versions() {
    return new Versions(
        getApiVersion(),
        getApplicationVersion(),
        getCommitId(),
        firstNonNull(releaseIndexMetadata().get("git.commit.id.abbrev"), "unknown"),
        indexName());
  }

}