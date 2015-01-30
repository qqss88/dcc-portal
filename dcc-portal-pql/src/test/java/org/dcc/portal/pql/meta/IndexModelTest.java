/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.dcc.portal.pql.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.IndexModel.Type.DONOR_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.es.visitor.CreateFilterBuilderVisitor;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.qe.QueryContext;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

@Slf4j
public class IndexModelTest extends BaseElasticsearchTest {

  private PqlParseListener listener;
  private CreateFilterBuilderVisitor visitor;
  private QueryContext queryContext;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(DONOR_CENTRIC).withData(bulkFile(getClass())));
    visitor = new CreateFilterBuilderVisitor(es.client(), new IndexModel());
    queryContext = new QueryContext();
    queryContext.setType(DONOR_CENTRIC);
    queryContext.setIndex(INDEX_NAME);
    listener = new PqlParseListener(new QueryContext());
  }

  @Test
  public void nestedTest() {
    val result = executeQuery("eq(gene._gene_id, 'ENSG00000215529')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(3);
    containsOnlyIds(result, "DO2", "DO5", "DO9");
  }

  @Test
  public void equalityNestedFiltersTest() {
    val result = executeQuery("or(eq(gene._gene_id, 'ENSG00000176105'), eq(gene._gene_id, 'ENSG00000215529'))");
    assertThat(result.getHits().getTotalHits()).isEqualTo(3);
    containsOnlyIds(result, "DO2", "DO5", "DO9");
  }

  private SearchResponse executeQuery(String query) {
    val esAst = createTree(query);
    val request = visitor.visit(esAst, queryContext);
    log.debug("Request - {}", request);
    val result = request.execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

  private ExpressionNode createTree(String query) {
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    return listener.getEsAst();
  }

  private static void containsOnlyIds(SearchResponse response, String... ids) {
    val resopnseIds = Lists.<String> newArrayList();

    for (val hit : response.getHits()) {
      resopnseIds.add(hit.getId());
    }
    assertThat(resopnseIds).containsOnly(ids);
  }

}
