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

import static com.google.common.collect.Lists.newArrayList;
import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import lombok.val;

import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.browser.ds.GeneDataSource;
import org.icgc.dcc.portal.browser.ds.MutationDataSource;
import org.icgc.dcc.portal.browser.model.DataSource;
import org.icgc.dcc.portal.mapper.BadRequestExceptionMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class BrowserResourceTest extends ResourceTest {

  private final static String RESOURCE = "/browser";

  @Mock
  private Client client;
  @Mock
  private GeneDataSource geneDataSource;
  @Mock
  private MutationDataSource mutationDataSource;

  @Override
  protected final void setUpResources() {
    val resource = new BrowserResource();
    resource.setClient(client);
    resource.setIndexName("indexName");
    resource.setDataSources(dataSources());

    addResource(resource);
    addProvider(BadRequestExceptionMapper.class);
  }

  @Test
  public final void test_getGene_BadRequest() {
    val response = client()
        .resource(RESOURCE)
        .path("gene")
        .queryParam("segment", "17:0-300009999")
        .queryParam("resource", "gene")
        .get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
  }

  private List<DataSource> dataSources() {
    DataSource gene = new DataSource();
    gene.setUri("gene");
    gene.setClassName(geneDataSource.getClass().getName());

    DataSource mutation = new DataSource();
    mutation.setUri("mutation");
    mutation.setClassName(mutationDataSource.getClass().getName());

    List<DataSource> dataSources = newArrayList(gene, mutation);
    return dataSources;
  }

}
