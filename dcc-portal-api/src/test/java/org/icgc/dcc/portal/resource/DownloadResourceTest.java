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

package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.Response.Status.OK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.oozie.client.WorkflowJob.Status;
import org.icgc.dcc.data.common.ExportedDataFileSystem;
import org.icgc.dcc.data.common.ExportedDataFileSystem.AccessPermission;
import org.icgc.dcc.data.downloader.ArchiveJobManager.JobProgress;
import org.icgc.dcc.data.downloader.ArchiveJobManager.JobStatus;
import org.icgc.dcc.data.downloader.DynamicDownloader;
import org.icgc.dcc.data.downloader.DynamicDownloader.DataType;
import org.icgc.dcc.portal.auth.openid.OpenIDAuthProvider;
import org.icgc.dcc.portal.auth.openid.OpenIDAuthenticator;
import org.icgc.dcc.portal.mapper.BadRequestExceptionMapper;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.DistributedCacheService;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.NotFoundException;
import org.icgc.dcc.portal.service.ServiceUnavailableException;
import org.icgc.dcc.portal.utils.HazelcastFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Stage;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class DownloadResourceTest extends ResourceTest {

  private final static String RESOURCE = "/v1/download";

  private final HazelcastInstance hazelcast = HazelcastFactory.createLocalHazelcastInstance();
  private final DistributedCacheService cacheService = new DistributedCacheService(hazelcast);

  @Mock
  private DonorService donorService;
  @Mock
  private DynamicDownloader downloader;

  @Mock
  private ExportedDataFileSystem fs;

  private final UUID sessionToken = UUID.randomUUID();
  private final User user = new User(null, sessionToken);

  @Before
  public void setUp() throws Exception {
    user.setDaco(true);
    cacheService.putUser(sessionToken, user);
  }

  @After
  public void tearDown() {
    hazelcast.shutdown();
  }

  @Override
  protected final void setUpResources() {
    addResource(new DownloadResource(donorService, downloader, fs, Stage.PRODUCTION));
    addProvider(BadRequestExceptionMapper.class);
    addProvider(new OpenIDAuthProvider(new OpenIDAuthenticator(cacheService), "openid"));
  }

  @Test
  public void testPublicDataAccessFile() throws IOException {
    when(fs.getPermission(any(File.class))).thenReturn(AccessPermission.UNCHECKED);
    when(fs.createInputStream((any(File.class)), anyInt())).thenReturn(new ByteArrayInputStream("test".getBytes()));

    ClientResponse response = client()
        .resource(RESOURCE)
        .queryParam("fn", "filename.txt.gz")
        .queryParam("filters", "")
        .get(ClientResponse.class);

    verify(fs, times(1)).createInputStream((any(File.class)), anyInt());
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test
  public void testOpenDataAccessFile() throws IOException {
    when(fs.getPermission(any(File.class))).thenReturn(AccessPermission.OPEN);
    when(fs.createInputStream((any(File.class)), anyInt())).thenReturn(new ByteArrayInputStream("test".getBytes()));

    ClientResponse response = client()
        .resource(RESOURCE)
        .queryParam("fn", "filename_open.txt.gz")
        .queryParam("filters", "")
        .get(ClientResponse.class);

    verify(fs, times(1)).createInputStream((any(File.class)), anyInt());
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test
  public void testControlledDataAccessFile() throws IOException {
    when(fs.getPermission(any(File.class))).thenReturn(AccessPermission.CONTROLLED);
    when(fs.createInputStream((any(File.class)), anyInt())).thenReturn(new ByteArrayInputStream("test".getBytes()));

    ClientResponse response = client()
        .resource(RESOURCE)
        .queryParam("fn", "filename_control.txt.gz")
        .queryParam("filters", "")
        .header("X-Auth-Token", sessionToken.toString())
        .get(ClientResponse.class);

    verify(fs, times(1)).createInputStream((any(File.class)), anyInt());
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test(expected = NotFoundException.class)
  public void testDeniedControlledDataAccessFile() throws IOException {
    when(fs.getPermission(any(File.class))).thenReturn(AccessPermission.CONTROLLED);
    when(fs.createInputStream((any(File.class)), anyInt())).thenReturn(new ByteArrayInputStream("test".getBytes()));

    client()
        .resource(RESOURCE)
        .queryParam("fn", "filename_control.txt.gz")
        .queryParam("filters", "")
        // no token
        .get(ClientResponse.class);
  }

  @Test
  public void testOpenDataAccessStream() throws IOException {
    when(
        downloader.streamArchiveInGzTar(any(OutputStream.class), anyString(),
            Matchers.<List<DataType>> any()))
        .thenReturn(true);
    String testId = "TESTID";
    JobStatus jobStatus =
        new JobStatus(Status.SUCCEEDED, ImmutableMap.<DataType, JobProgress> of(DataType.SSM_OPEN,
            new JobProgress(1, 1)), false, false);
    when(downloader.isServiceAvailable()).thenReturn(true);
    when(downloader.getStatus(anySetOf(String.class))).thenReturn(ImmutableMap.of(testId, jobStatus));

    ClientResponse response = client()
        .resource(RESOURCE + "/" + testId)
        .queryParam("filters", "")
        .queryParam("info", "[{\"key\":\"SSM\",\"value\":\"TSV\"}]")
        .get(ClientResponse.class);
    List<DataType> selection =
        ImmutableList.of(DataType.SSM_OPEN);
    verify(downloader).streamArchiveInGzTar(any(OutputStream.class), anyString(),
        argThat(new SelectionEntryArgumentMatcher(selection)));
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test(expected = NotFoundException.class)
  public void testDeniedControlledDataAccessStream() throws IOException {
    when(
        downloader.streamArchiveInGzTar(any(OutputStream.class), anyString(),
            Matchers.<List<DataType>> any()))
        .thenReturn(true);
    when(downloader.isServiceAvailable()).thenReturn(true);
    // // try to access control data without proper authentication
    client()
        .resource(RESOURCE)
        .queryParam("filters", "")
        .queryParam("info", "[{\"key\":\"SGV\",\"value\":\"TSV\"}]")
        .get(ClientResponse.class);
  }

  // we don't have controlled access authentication
  @Test
  @Ignore
  public void testcontrolAccessStream() throws IOException {
    when(
        downloader.streamArchiveInGzTar(any(OutputStream.class), anyString(),
            Matchers.<List<DataType>> any()))
        .thenReturn(true);
    when(downloader.isServiceAvailable()).thenReturn(true);
    ClientResponse response = client()
        .resource(RESOURCE)
        .queryParam("filters", "")
        .queryParam("info", "[{\"key\":\"ssm\",\"value\":\"TSV\"}]")
        .header("X-Auth-Token", sessionToken.toString())
        .get(ClientResponse.class);
    List<DataType> selection =
        ImmutableList.of(DataType.SSM_CONTROLLED);
    verify(downloader).streamArchiveInGzTar(any(OutputStream.class), anyString(),
        argThat(new SelectionEntryArgumentMatcher(selection)));
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test
  public void test_getArchive_found() throws IOException {
    when(
        downloader.streamArchiveInGzTar(any(OutputStream.class), anyString(),
            Matchers.<List<DataType>> any()))
        .thenReturn(true);

    String testId = "TESTID";
    JobStatus jobStatus =
        new JobStatus(Status.SUCCEEDED, ImmutableMap.<DataType, JobProgress> of(DataType.SSM_OPEN,
            new JobProgress(1, 1)), false, false);
    when(downloader.isServiceAvailable()).thenReturn(true);
    when(downloader.getStatus(anySetOf(String.class))).thenReturn(ImmutableMap.of(testId, jobStatus));

    ClientResponse response = client()
        .resource(RESOURCE + "/" + testId)
        .queryParam("filters", "")
        .queryParam("info", "")
        .get(ClientResponse.class);
    verify(downloader).streamArchiveInGzTar(any(OutputStream.class), anyString(),
        Matchers.<List<DataType>> any());
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  // @Test(expected = BadRequestException.class)
  public void test_getArchive_bad_request() {
    when(downloader.isServiceAvailable()).thenReturn(true);
    client()
        .resource(RESOURCE)
        .queryParam("filters", "")
        .queryParam("info", "")
        .get(ClientResponse.class);
  }

  @Test(expected = ServiceUnavailableException.class)
  public void testOverCapacity() {
    when(downloader.isServiceAvailable()).thenReturn(true);
    when(downloader.isOverCapacity()).thenReturn(true);
    client()
        .resource(RESOURCE + "/testid")
        .queryParam("filters", "")
        .queryParam("info", "")
        .get(ClientResponse.class);
  }

  @Test
  public void testGetStatusWithOverCapacity() {
    when(downloader.isServiceAvailable()).thenReturn(true);
    when(downloader.isOverCapacity()).thenReturn(true);
    Map<String, Object> response = client()
        .resource(RESOURCE + "/status")
        .get(new GenericType<Map<String, Object>>() {});
    assertThat(response.get("serviceStatus")).isEqualTo(false);
  }

  @Test
  public void testGetStatusWithUnavailable() {
    when(downloader.isServiceAvailable()).thenReturn(false);
    when(downloader.isOverCapacity()).thenReturn(false);
    Map<String, Object> response = client()
        .resource(RESOURCE + "/status")
        .queryParam("filters", "")
        .queryParam("info", "")
        .get(new GenericType<Map<String, Object>>() {});
    assertThat(response.get("serviceStatus")).isEqualTo(false);
  }

  private final class SelectionEntryArgumentMatcher extends ArgumentMatcher<List<DataType>> {

    List<DataType> selection;

    public SelectionEntryArgumentMatcher(List<DataType> selection) {
      this.selection = selection;
    }

    @Override
    public boolean matches(Object argument) {
      @SuppressWarnings("unchecked")
      List<DataType> selection = (List<DataType>) argument;
      return this.selection.equals(selection);
    }
  }

}
