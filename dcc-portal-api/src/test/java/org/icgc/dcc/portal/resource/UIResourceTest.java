/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import lombok.val;

import org.icgc.dcc.portal.mapper.BadRequestExceptionMapper;
import org.icgc.dcc.portal.mapper.IllegalArgumentExceptionMapper;
import org.icgc.dcc.portal.service.ReactomeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class UIResourceTest extends ResourceTest {

  private final static String RESOURCE = "/v1/ui";
  private final static int BAD_REQUEST_CODE = ClientResponse.Status.BAD_REQUEST.getStatusCode();
  private final static int OK_CODE = ClientResponse.Status.OK.getStatusCode();
  private static InputStream TEST_STREAM = null;

  @Mock
  private ReactomeService service;

  @InjectMocks
  private UIResource resource;

  @Override
  protected final void setUpResources() {
    addResource(resource);
    addProvider(BadRequestExceptionMapper.class);
    addProvider(IllegalArgumentExceptionMapper.class);
    try {
      TEST_STREAM =
          (new URL(ReactomeService.REACTOME_BASE_URL + "pathwayDiagram/109581/XML"))
              .openStream();
    } catch (IOException e) {
      // Another test will catch this
    }
  }

  @Test
  public void testSuccessfulPathwayFetch() {
    when(service.getPathwayDiagramStream(any(String.class))).thenReturn(TEST_STREAM);
    val response = generateResponse("REACT_578.8");
    assertThat(response.getStatus()).isEqualTo(OK_CODE);
    assertThat(response.toString()).isNotEmpty();
  }

  @Test
  public void testBadId() {
    when(service.getPathwayDiagramStream(any(String.class))).thenReturn(TEST_STREAM);
    val response = generateResponse("aly");
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_CODE);
  }

  @Test
  public void testTruncatedId() {
    when(service.getPathwayDiagramStream(any(String.class))).thenReturn(TEST_STREAM);
    val response = generateResponse("REACT_");
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_CODE);
  }

  private ClientResponse generateResponse(String id) {
    return client()
        .resource(RESOURCE).path("/reactome-pathway-diagram")
        .queryParam("pathwayId", id)
        .get(ClientResponse.class);
  }

}
