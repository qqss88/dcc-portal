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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.portal.util.VersionUtils.getApiVersion;
import static org.icgc.dcc.portal.util.VersionUtils.getApplicationVersion;
import static org.icgc.dcc.portal.util.VersionUtils.getCommitId;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.icgc.dcc.common.client.api.ICGCClient;
import org.icgc.dcc.common.client.api.ICGCClientConfig;
import org.icgc.dcc.common.client.api.cud.CUDClient;
import org.icgc.dcc.common.client.api.daco.DACOClient;
import org.icgc.dcc.common.client.api.shorturl.ShortURLClient;
import org.icgc.dcc.data.common.ExportedDataFileSystem;
import org.icgc.dcc.data.downloader.DynamicDownloader;
import org.icgc.dcc.portal.auth.openid.DistributedConsumerAssociationStore;
import org.icgc.dcc.portal.auth.openid.DistributedNonceVerifier;
import org.icgc.dcc.portal.auth.openid.OpenIDAuthProvider;
import org.icgc.dcc.portal.auth.openid.OpenIDAuthenticator;
import org.icgc.dcc.portal.browser.model.DataSource;
import org.icgc.dcc.portal.model.Settings;
import org.icgc.dcc.portal.model.Versions;
import org.icgc.dcc.portal.service.DistributedCacheService;
import org.openid4java.consumer.ConsumerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.google.inject.Stage;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Slf4j
@Lazy
@Configuration
public class PortalConfig {

  @Autowired
  private DataPortalConfiguration config;

  @Bean
  public Client client() {
    // TransportClient is thread-safe so @Singleton is appropriate
    val configuration = config.getElastic();
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
    String indexName = config.getElastic().getIndexName();

    // Get cluster state
    ClusterState clusterState = client().admin().cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState();

    Map<String, AliasMetaData> aliases = clusterState.getMetaData().aliases().get(indexName);
    if (aliases != null) {
      Set<String> indexNames = aliases.keySet();
      checkState(indexNames.size() == 1, "Expected alias to point to a single index but instead it points to '%s'",
          indexNames);

      String realIndexName = getFirst(indexNames, null);
      log.warn("Redirecting configured index alias of '{}' with real index name '{}'", indexName, realIndexName);
      indexName = realIndexName;
    }

    return indexName;
  }

  @Bean
  public DynamicDownloader dynamicDownloader() {
    val downloadConfig = config.getDownload();
    if (!downloadConfig.isEnabled()) {
      return null;
    }

    return new DynamicDownloader(
        downloadConfig.getUri() + downloadConfig.getDynamicRootPath(),
        downloadConfig.getQuorum(),
        downloadConfig.getOozieUrl(),
        downloadConfig.getAppPath(),
        downloadConfig.getSupportEmailAddress(),
        downloadConfig.getCapacityThreshold(),
        downloadConfig.getReleaseName());

  }

  @Bean
  public Stage stage() {
    return config.getDownload().getStage();
  }

  @Bean
  @SneakyThrows
  public ExportedDataFileSystem exportedDataFileSystem() {
    val downloadConfig = config.getDownload();
    if (!downloadConfig.isEnabled()) {
      return null;
    }

    val rootDir = downloadConfig.getUri() + downloadConfig.getStaticRootPath();
    return new ExportedDataFileSystem(rootDir, downloadConfig.getCurrentReleaseSymlink());
  }

  @Bean
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public Map<String, String> indexMetadata() {
    // Get cluster state
    ClusterState clusterState = client().admin().cluster()
        .prepareState()
        .setFilterIndices(indexName())
        .execute()
        .actionGet()
        .getState();

    IndexMetaData indexMetaData = clusterState.getMetaData().index(indexName());
    checkState(indexMetaData != null, "Index meta data is null. Ensure that index '%s' exists.", indexName());

    // Get the first mappings. This is arbitrary since they all contain the same metadata
    MappingMetaData mappingMetaData = indexMetaData.getMappings().values().asList().get(0);

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
        firstNonNull(indexMetadata().get("git.commit.id.abbrev"), "unknown"),
        indexName());
  }

  @Bean
  @Qualifier
  public List<DataSource> dataSources() {
    return config.getBrowser().getDataSources();
  }

  @Bean
  public MailConfiguration mailConfig() {
    return config.getMail();
  }

  @Bean
  public OpenIDAuthProvider openIdProvider(OpenIDAuthenticator authenticator) {
    return new OpenIDAuthProvider(authenticator, "OpenID");
  }

  @Bean
  public Settings settings() {
    return Settings.builder()
        .ssoUrl(config.getCrowd().getSsoUrl())
        .releaseDate(config.getRelease().getReleaseDate())
        .build();
  }

  @Bean
  public ShortURLClient shortURLClient() {
    val icgc = checkNotNull(config.getIcgc());
    val icgcConfig = ICGCClientConfig.builder()
        .shortServiceUrl(icgc.getShortUrl())
        .consumerKey(icgc.getConsumerKey())
        .consumerSecret(icgc.getConsumerSecret())
        .accessToken(icgc.getAccessToken())
        .accessSecret(icgc.getAccessSecret())
        .strictSSLCertificates(icgc.getEnableStrictSSL())
        .requestLoggingEnabled(icgc.getEnableHttpLogging())
        .build();

    return ICGCClient.create(icgcConfig).shortUrl();
  }

  @Bean
  public ICGCConfiguration icgcConfiguration() {
    return config.getIcgc();
  }

  @Bean
  public ICGCClient icgcClient(ICGCConfiguration icgc) {
    val icgcConfig = ICGCClientConfig.builder()
        .cgpServiceUrl(icgc.getCgpUrl())
        .cudServiceUrl(icgc.getCudUrl())
        .cudAppId(icgc.getCudAppId())
        .consumerKey(icgc.getConsumerKey())
        .consumerSecret(icgc.getConsumerSecret())
        .accessToken(icgc.getAccessToken())
        .accessSecret(icgc.getAccessSecret())
        .strictSSLCertificates(icgc.getEnableStrictSSL())
        .requestLoggingEnabled(icgc.getEnableHttpLogging())
        .build();

    return ICGCClient.create(icgcConfig);
  }

  @Bean
  public DACOClient dacoClient(ICGCClient icgcClient) {
    return icgcClient.daco();
  }

  @Bean
  public CUDClient cudClient(ICGCClient icgcClient) {
    return icgcClient.cud();
  }

  @Bean
  public HazelcastInstance hazelcastInstance() {
    return Hazelcast.newHazelcastInstance(getHazelcastConfig(config.getHazelcast()));
  }

  @Bean
  public ConsumerManager consumerManager(HazelcastInstance hazelcast) {
    val consumerManager = new ConsumerManager();
    consumerManager.setAssociations(new DistributedConsumerAssociationStore(hazelcast));
    consumerManager.setNonceVerifier(new DistributedNonceVerifier(hazelcast));

    return consumerManager;
  }

  @Bean
  public DistributedCacheService distributedCacheService(HazelcastInstance hazelcast) {
    return new DistributedCacheService(hazelcast);
  }

  @Bean
  public CrowdConfiguration crowdConfig() {
    return config.getCrowd();
  }

  @Bean
  public CacheConfiguration cacheConfiguration() {
    return config.getCache();
  }

  @Bean
  public WebConfiguration webConfiguration() {
    return config.getWeb();
  }

  private static Config getHazelcastConfig(HazelcastConfiguration hazelcastConfig) {
    val config = new Config();
    config.setGroupConfig(new GroupConfig(hazelcastConfig.getGroupName(), hazelcastConfig.getGroupPassword()));
    configureMapConfigs(hazelcastConfig, config.getMapConfigs());

    return config;
  }

  private static void configureMapConfigs(HazelcastConfiguration hazelcastConfig, Map<String, MapConfig> mapConfigs) {
    val usersMapConfig = new MapConfig();
    usersMapConfig.setName(DistributedCacheService.USERS_CACHE_NAME);
    usersMapConfig.setTimeToLiveSeconds(hazelcastConfig.getUsersCacheTTL());
    mapConfigs.put(DistributedCacheService.USERS_CACHE_NAME, usersMapConfig);

    val openidAuthMapConfig = new MapConfig();
    openidAuthMapConfig.setName(DistributedCacheService.DISCOVERY_INFO_CACHE_NAME);
    openidAuthMapConfig.setTimeToLiveSeconds(hazelcastConfig.getOpenidAuthTTL());
    mapConfigs.put(DistributedCacheService.DISCOVERY_INFO_CACHE_NAME, openidAuthMapConfig);
  }

}