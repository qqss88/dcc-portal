/* Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class DiagramRepositoryTest extends BaseRepositoryTest {

  private static final String FILTER = "{diagram_id:{is:\"REACT_163914\"}}";

  DiagramRepository diagramRepository;

  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(Kind.DIAGRAM);

  @Before
  public void setUp() throws Exception {
    es.execute(
        createIndexMapping(Type.DIAGRAM)
            .withData(bulkFile(getClass())));
    diagramRepository = new DiagramRepository(es.client(), INDEX);
  }

  @Test
  public void testFindAll() throws Exception {
    Query query = Query.builder().from(1).size(10).build();
    SearchResponse response = diagramRepository.findAll(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(1);
  }

  @Test
  public void testFindAllWithIsFilters() throws Exception {
    FiltersParam filter = new FiltersParam(FILTER);
    Query query = Query.builder().from(1).size(10).filters(filter.get())
        .build();
    SearchResponse response = diagramRepository.findAll(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);
    log.info(hits.getHits()[0].field(FIELDS.get("id")).getValue());
    assertThat(hits.getHits()[0].field(FIELDS.get("id")).getValue()).isEqualTo("REACT_163914");
  }

  @Test
  public void testFind() throws Exception {
    String id = "REACT_163914";
    Query query = Query.builder().build();
    Map<String, Object> response = diagramRepository.findOne(id, query);
    assertThat(response.get(FIELDS.get("diagram_id"))).isEqualTo(id);
  }

}
