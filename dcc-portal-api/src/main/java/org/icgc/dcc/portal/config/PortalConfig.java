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

import static com.beust.jcommander.internal.Maps.newHashMap;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.icgc.dcc.portal.service.SessionService.DISCOVERY_INFO_CACHE_NAME;

import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.client.Client;
import org.icgc.dcc.common.client.api.ICGCClient;
import org.icgc.dcc.common.client.api.ICGCClientConfig;
import org.icgc.dcc.common.client.api.cms.CMSClient;
import org.icgc.dcc.common.client.api.cud.CUDClient;
import org.icgc.dcc.common.client.api.daco.DACOClient;
import org.icgc.dcc.common.client.api.shorturl.ShortURLClient;
import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.downloader.client.DownloaderClient;
import org.icgc.dcc.downloader.client.ExportedDataFileSystem;
import org.icgc.dcc.portal.auth.UserAuthProvider;
import org.icgc.dcc.portal.auth.UserAuthenticator;
import org.icgc.dcc.portal.auth.openid.DistributedConsumerAssociationStore;
import org.icgc.dcc.portal.auth.openid.DistributedNonceVerifier;
import org.icgc.dcc.portal.config.PortalProperties.CacheProperties;
import org.icgc.dcc.portal.config.PortalProperties.CrowdProperties;
import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.icgc.dcc.portal.config.PortalProperties.HazelcastProperties;
import org.icgc.dcc.portal.config.PortalProperties.ICGCProperties;
import org.icgc.dcc.portal.config.PortalProperties.MailProperties;
import org.icgc.dcc.portal.config.PortalProperties.OAuthProperties;
import org.icgc.dcc.portal.config.PortalProperties.WebProperties;
import org.icgc.dcc.portal.model.Settings;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.PhenotypeAnalysisRepository;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.repository.UserGeneSetRepository;
import org.icgc.dcc.portal.service.OccurrenceService;
import org.icgc.dcc.portal.service.SessionService;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.discovery.DiscoveryInformation;
import org.skife.jdbi.v2.DBI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;

import com.google.inject.Stage;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Lazy
@EnableAsync
@Configuration
public class PortalConfig {

  @Autowired
  private PortalProperties properties;

  @Autowired
  private OccurrenceService service;

  @Bean
  public DownloaderClient dynamicDownloader() {
    val download = properties.getDownload();
    if (!download.isEnabled()) {
      return null;
    }

    return new DownloaderClient(
        download.getUri() + download.getDynamicRootPath(),
        download.getQuorum(),
        download.getOozieUrl(),
        download.getAppPath(),
        download.getSupportEmailAddress(),
        download.getCapacityThreshold(),
        download.getReleaseName());
  }

  @PostConstruct
  public void initCache() {
    service.init();

  }

  @Bean
  public Stage stage() {
    return properties.getDownload().getStage();
  }

  @Bean
  @SneakyThrows
  public ExportedDataFileSystem exportedDataFileSystem() {
    val downloadConfig = properties.getDownload();
    if (!downloadConfig.isEnabled()) {
      return null;
    }

    val rootDir = downloadConfig.getUri() + downloadConfig.getStaticRootPath();
    return new ExportedDataFileSystem(rootDir, downloadConfig.getCurrentReleaseSymlink());
  }

  @Bean
  public UserAuthProvider openIdProvider(UserAuthenticator authenticator) {
    return new UserAuthProvider(authenticator, "OpenID");
  }

  @Bean
  public UserGeneSetRepository userGeneSetRepository(DBI dbi) {
    return dbi.open(UserGeneSetRepository.class);
  }

  @Bean
  public EnrichmentAnalysisRepository enrichmentAnalysisRepository(DBI dbi) {
    return dbi.open(EnrichmentAnalysisRepository.class);
  }

  @Bean
  public EntityListRepository entityListRepository(final DBI dbi) {
    return dbi.open(EntityListRepository.class);
  }

  @Bean
  public UnionAnalysisRepository unionAnalysisRepository(final DBI dbi) {
    return dbi.open(UnionAnalysisRepository.class);
  }

  @Bean
  public PhenotypeAnalysisRepository phenotypeAnalysisRepository(final DBI dbi) {
    return dbi.open(PhenotypeAnalysisRepository.class);
  }

  @Bean
  public Settings settings() {
    val crowd = properties.getCrowd();
    val release = properties.getRelease();
    val download = properties.getDownload();
    val setAnalysis = properties.getSetOperation();

    return Settings.builder()
        .ssoUrl(crowd.getSsoUrl())
        .ssoUrlGoogle(crowd.getSsoUrlGoogle())
        .releaseDate(release.getReleaseDate())
        .dataVersion(release.getDataVersion())
        .downloadEnabled(download.isEnabled())
        .maxNumberOfHits(setAnalysis.maxNumberOfHits)
        .maxMultiplier(setAnalysis.maxMultiplier)
        .build();
  }

  @Bean
  public ShortURLClient shortURLClient() {
    val icgc = checkNotNull(properties.getIcgc());
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
  public ICGCClient icgcClient(ICGCProperties icgc) {
    val icgcConfig = ICGCClientConfig.builder()
        .cgpServiceUrl(icgc.getCgpUrl())
        .cudServiceUrl(icgc.getCudUrl())
        .cmsServiceUrl(icgc.getCmsUrl())
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
  public CMSClient cmsClient(ICGCClient icgcClient) {
    return icgcClient.cms();
  }

  @Bean
  public HazelcastInstance hazelcastInstance() {
    if (isDistributed()) {
      return Hazelcast.newHazelcastInstance(getHazelcastConfig(properties.getHazelcast()));
    } else {
      return null;
    }
  }

  @Bean
  public ConsumerManager consumerManager() {
    val consumerManager = new ConsumerManager();

    if (isDistributed()) {
      consumerManager.setAssociations(new DistributedConsumerAssociationStore(hazelcastInstance()));
      consumerManager.setNonceVerifier(new DistributedNonceVerifier(hazelcastInstance()));
    } else {
      consumerManager.setAssociations(new InMemoryConsumerAssociationStore());
      consumerManager.setNonceVerifier(new InMemoryNonceVerifier());
    }

    return consumerManager;
  }

  @Bean
  public SessionService sessionService() {
    if (isDistributed()) {
      Map<UUID, User> usersCache = hazelcastInstance().getMap(SessionService.USERS_CACHE_NAME);
      Map<UUID, DiscoveryInformation> discoveryInfoCache = hazelcastInstance().getMap(DISCOVERY_INFO_CACHE_NAME);

      return new SessionService(usersCache, discoveryInfoCache);
    } else {
      Map<UUID, User> usersCache = newHashMap();
      Map<UUID, DiscoveryInformation> discoveryInfoCache = newHashMap();

      return new SessionService(usersCache, discoveryInfoCache);
    }
  }

  @Bean
  public MailProperties mailProperties() {
    return properties.getMail();
  }

  @Bean
  public ICGCProperties icgcProperties() {
    return properties.getIcgc();
  }

  @Bean
  public DownloadProperties downloadProperties() {
    return properties.getDownload();
  }

  @Bean
  public CrowdProperties crowdProperties() {
    return properties.getCrowd();
  }

  @Bean
  public CacheProperties cacheProperties() {
    return properties.getCache();
  }

  @Bean
  public WebProperties webProperties() {
    return properties.getWeb();
  }

  @Bean
  public OAuthProperties oauthProperties() {
    return properties.getOauth();
  }

  @Bean
  public QueryEngine queryEngine(@NonNull Client client, @Value("#{indexName}") String index) {
    return new QueryEngine(client, index);
  }

  @Bean
  public Mailer mailer(MailProperties mail) {
    return Mailer.builder()
        .enabled(mail.isEnabled())
        .recipient(mail.getRecipientEmail())
        .from(mail.getSenderName())
        .host(mail.getSmtpServer())
        .port(Integer.toString(mail.getSmtpPort()))
        .build();
  }

  //
  // Utilities
  //

  private boolean isDistributed() {
    return properties.getHazelcast().isEnabled();
  }

  private static Config getHazelcastConfig(HazelcastProperties hazelcastConfig) {
    val config = new Config();
    config.setProperty("hazelcast.logging.type", "slf4j");
    config.setGroupConfig(new GroupConfig(hazelcastConfig.getGroupName(), hazelcastConfig.getGroupPassword()));
    configureMapConfigs(hazelcastConfig, config.getMapConfigs());

    return config;
  }

  private static void configureMapConfigs(HazelcastProperties hazelcastConfig, Map<String, MapConfig> mapConfigs) {
    val usersMapConfig = new MapConfig();
    usersMapConfig.setName(SessionService.USERS_CACHE_NAME);
    usersMapConfig.setTimeToLiveSeconds(hazelcastConfig.getUsersCacheTTL());
    mapConfigs.put(SessionService.USERS_CACHE_NAME, usersMapConfig);

    val openidAuthMapConfig = new MapConfig();
    openidAuthMapConfig.setName(SessionService.DISCOVERY_INFO_CACHE_NAME);
    openidAuthMapConfig.setTimeToLiveSeconds(hazelcastConfig.getOpenidAuthTTL());
    mapConfigs.put(SessionService.DISCOVERY_INFO_CACHE_NAME, openidAuthMapConfig);
  }

}
