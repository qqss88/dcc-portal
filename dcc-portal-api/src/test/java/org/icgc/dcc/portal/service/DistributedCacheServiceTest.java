package org.icgc.dcc.portal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.test.HazelcastFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;

import com.hazelcast.core.HazelcastInstance;

public class DistributedCacheServiceTest {

  private static final String OPEN_ID_IDENTIFIER = "openIDIdentifier";
  private static final String EMAIL_ADDRESS = "test@email.com";

  private final UUID sessionToken = UUID.randomUUID();
  private final User user = new User(null, sessionToken);

  private DiscoveryInformation discoveryInfo = getDiscoveryInfo();
  private DistributedCacheService cacheService;
  private HazelcastInstance hazelcast;

  @Before
  public void setUp() throws Exception {
    hazelcast = HazelcastFactory.createLocalHazelcastInstance();
    cacheService = new DistributedCacheService(hazelcast);
    cacheService.putUser(sessionToken, user);
    cacheService.putDiscoveryInfo(sessionToken, discoveryInfo);
  }

  @After
  public void tearDown() throws Exception {
    hazelcast.shutdown();
  }

  @Test
  public void testGetBySessionToken() throws Exception {
    val userOptional = cacheService.getUserBySessionToken(sessionToken);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetBySessionTokenInvalid() throws Exception {
    assertThat(cacheService.getUserBySessionToken(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @Test
  public void testRemoveUser() throws Exception {
    cacheService.removeUser(user);
    assertThat(cacheService.getUserBySessionToken(sessionToken).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetUserByOpenidIdentifierFound() throws Exception {
    user.setOpenIDIdentifier(OPEN_ID_IDENTIFIER);

    // must be pushed otherwise local changes won't be reflected in the cluster
    cacheService.putUser(sessionToken, user);
    val userOptional = cacheService.getUserByOpenidIdentifier(OPEN_ID_IDENTIFIER);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetUserByOpenidIdentifierNotFound() throws Exception {
    assertThat(cacheService.getUserByOpenidIdentifier(OPEN_ID_IDENTIFIER).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetUserByEmailFound() throws Exception {
    user.setEmailAddress(EMAIL_ADDRESS);
    cacheService.putUser(sessionToken, user);
    val userOptional = cacheService.getUserByEmail(EMAIL_ADDRESS);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetUserByEmailNotFound() throws Exception {
    assertThat(cacheService.getUserByEmail(EMAIL_ADDRESS).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetDiscoveryInfoFound() {
    val discoveryInfoOptional = cacheService.getDiscoveryInfo(sessionToken);

    assertThat(discoveryInfoOptional.isPresent()).isEqualTo(true);
    assertThat(discoveryInfoOptional.get()).isEqualTo(discoveryInfo);
  }

  @Test
  public void testGetDiscoveryInfoNotFound() {
    assertThat(cacheService.getDiscoveryInfo(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @Test
  public void testRemoveDiscoveryInfo() {
    assertThat(cacheService.getDiscoveryInfo(sessionToken).isPresent()).isEqualTo(true);
    cacheService.removeDiscoveryInfo(sessionToken);
    assertThat(cacheService.getDiscoveryInfo(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @SneakyThrows
  private static DiscoveryInformation getDiscoveryInfo() {
    // DiscoveryInformation does not override equals. All objects retrieved from hazelcast do not point to the same
    // local objects
    return new DiscoveryInfoMock(new URL("http://test.org"));
  }

  @EqualsAndHashCode(callSuper = false)
  public static class DiscoveryInfoMock extends DiscoveryInformation {

    public DiscoveryInfoMock(URL opEndpoint) throws DiscoveryException {
      super(opEndpoint);
    }

  }

}
