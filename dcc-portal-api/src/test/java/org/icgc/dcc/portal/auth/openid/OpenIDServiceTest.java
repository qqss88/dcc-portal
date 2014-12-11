package org.icgc.dcc.portal.auth.openid;

import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.portal.service.AuthService;
import org.icgc.dcc.portal.service.SessionService;
import org.icgc.dcc.portal.test.HazelcastFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryInformation;

import com.hazelcast.core.HazelcastInstance;

@RunWith(MockitoJUnitRunner.class)
// FIXME How to test?
public class OpenIDServiceTest {

  @SuppressWarnings("unused")
  private static final String SERVER_HOST = "localhost";
  @SuppressWarnings("unused")
  private static final int SERVER_PORT = 8080;
  private static final String OPENID_IDENTIFIER = "https://me.yahoo.com";
  @SuppressWarnings("unused")
  private static final String CURRENT_URL = "/";
  private static final String OP_ENDPOINT = "https://open.login.yahooapis.com/openid/op/auth";

  @SuppressWarnings("unused")
  private OpenIDAuthService openidService;
  private final HazelcastInstance hazelcast = HazelcastFactory.createLocalHazelcastInstance();
  private final SessionService distributedCacheService = new SessionService();

  @Mock
  private ConsumerManager consumerManager;

  @Mock
  private AuthService authService;

  @Mock
  private List<Object> discoveries;

  String serverName = "localhost";

  UUID sessionToken = UUID.randomUUID();

  String returnToUrl = "/donors/DO1";

  @Before
  public void setUp() throws Exception {

    openidService = new OpenIDAuthService(distributedCacheService, consumerManager, authService);

    val discoveryInfo = createDiscoveryInfo();
    when(consumerManager.discover(OPENID_IDENTIFIER)).thenReturn(discoveries);
    when(consumerManager.associate(discoveries)).thenReturn(discoveryInfo);
  }

  @After
  public void tearDown() throws Exception {
    hazelcast.shutdown();
  }

  @Test
  public void createAuthRequestTest() {
  }

  @Test
  public void verifyTest() {
  }

  @SuppressWarnings("unused")
  private static ConsumerManager createConsumerManager(HazelcastInstance hazelcast) {
    val consumerManager = new ConsumerManager();
    consumerManager.setAssociations(new DistributedConsumerAssociationStore(hazelcast));
    consumerManager.setNonceVerifier(new DistributedNonceVerifier(hazelcast));

    return consumerManager;
  }

  @SneakyThrows
  private static DiscoveryInformation createDiscoveryInfo() {
    return new DiscoveryInformation(new URL(OP_ENDPOINT));
  }

}
