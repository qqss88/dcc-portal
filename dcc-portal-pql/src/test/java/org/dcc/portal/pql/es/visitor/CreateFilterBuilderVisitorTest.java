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
package org.dcc.portal.pql.es.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.icgc.dcc.portal.model.IndexModel.Type.DONOR_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.model.RequestType;
import org.dcc.portal.pql.es.utils.EsAstTransformator;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.qe.QueryContext;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

@Slf4j
public class CreateFilterBuilderVisitorTest extends BaseElasticsearchTest {

  private PqlParseListener listener;
  private CreateFilterBuilderVisitor visitor;
  private QueryContext queryContext;
  private EsAstTransformator esAstTransformator;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(DONOR_CENTRIC).withData(bulkFile(getClass())));
    visitor = new CreateFilterBuilderVisitor(es.client(), new IndexModel());
    queryContext = new QueryContext();
    queryContext.setType(DONOR_CENTRIC);
    queryContext.setIndex(INDEX_NAME);
    listener = new PqlParseListener(new QueryContext());
    esAstTransformator = new EsAstTransformator();
  }

  @Test
  public void sortTest() {
    val result = executeQuery("select(donor_age_at_enrollment),sort(-donor_age_at_enrollment)");
    assertThat(result.getHits().getAt(0).getSortValues()[0]).isEqualTo(173L);

  }

  @Test
  public void selectTest() {
    val result = executeQuery("select(donor_age_at_enrollment)");
    val hit = result.getHits().getAt(0);
    assertThat(hit.fields().size()).isEqualTo(1);
    assertThat(hit.field("donor_age_at_enrollment").getValue()).isEqualTo(77);
  }

  @Test
  public void countTest() {
    val esAst = createTree("count()");
    queryContext.setRequestType(RequestType.COUNT);
    val request = visitor.visit(esAst, queryContext);
    val result = request.execute().actionGet();
    assertThat(result.getHits().getTotalHits()).isEqualTo(9);
  }

  @Test
  public void countTest_withFilter() {
    val esAst = createTree("count(), gt(donor_age_at_enrollment, 100)");
    queryContext.setRequestType(RequestType.COUNT);
    val request = visitor.visit(esAst, queryContext);
    val result = request.execute().actionGet();
    assertThat(result.getHits().getTotalHits()).isEqualTo(4);
  }

  @Test
  public void inTest() {
    val result = executeQuery("in(_donor_id, 'DO1', 'DO2')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(2);
    containsOnlyIds(result, "DO1", "DO2");
  }

  @Test
  public void inTest_nested() {
    val result = executeQuery("in(gene.ssm._mutation_id, 'MU7', 'MU27')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(2);
    containsOnlyIds(result, "DO5", "DO8");
  }

  @Test
  public void eqTest() {
    val result = executeQuery("eq(_donor_id, 'DO1')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("DO1");
  }

  @Test
  public void eqTest_nested() {
    val result = executeQuery("eq(gene.ssm.observation._sample_id, 'SA35')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("DO7");
  }

  @Test
  public void neTest() {
    val result = executeQuery("ne(_donor_id, 'DO1')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(8);
    for (val hit : result.getHits()) {
      assertThat(hit.getId()).isNotEqualTo("DO1");
    }
  }

  @Test
  public void neTest_nested() {
    val result = executeQuery("ne(gene._summary._ssm_count, 1)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(3);
    containsOnlyIds(result, "DO3", "DO6", "DO7");
  }

  @Test
  public void gtTest() {
    val result = executeQuery("gt(_donor_id, 'DO5')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(4);
    containsOnlyIds(result, "DO6", "DO7", "DO8", "DO9");
  }

  @Test
  public void gtTest_nested() {
    val result = executeQuery("gt(gene.ssm.chromosome_start, 238855352)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    containsOnlyIds(result, "DO5");
  }

  @Test
  public void geTest() {
    val result = executeQuery("ge(gene.ssm.observation.quality_score, 49)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(3);
    containsOnlyIds(result, "DO2", "DO3", "DO9");
  }

  @Test
  public void geTest_nested() {
    val result = executeQuery("ge(gene.ssm.chromosome_start, 238855352)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(2);
    containsOnlyIds(result, "DO5", "DO7");
  }

  @Test
  public void leTest() {
    val result = executeQuery("le(_donor_id, 'DO5')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(5);
    containsOnlyIds(result, "DO1", "DO2", "DO3", "DO4", "DO5");
  }

  @Test
  public void leTest_nested() {
    val result = executeQuery("le(gene.ssm.chromosome_start, 1672334)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(2);
    containsOnlyIds(result, "DO2", "DO9");
  }

  @Test
  public void ltTest() {
    val result = executeQuery("lt(_donor_id, 'DO5')");
    assertThat(result.getHits().getTotalHits()).isEqualTo(4);
    containsOnlyIds(result, "DO1", "DO2", "DO3", "DO4");
  }

  @Test
  public void ltTest_nested() {
    val result = executeQuery("lt(gene.ssm.chromosome_start, 1672334)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    containsOnlyIds(result, "DO9");
  }

  @Test
  public void andTest() {
    val result =
        executeQuery("and(ge(gene.ssm.observation.quality_score, 49), ge(gene.ssm.observation.probability, 49)))");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("DO9");
  }

  @Test
  public void andTest_rootLevel() {
    val result = executeQuery("ge(gene.ssm.observation.quality_score, 49), ge(gene.ssm.observation.probability, 49)");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("DO9");
  }

  @Test
  public void orTest() {
    val result =
        executeQuery("or(ge(gene.ssm.observation.quality_score, 49), ge(gene.ssm.observation.probability, 49)))");
    assertThat(result.getHits().getTotalHits()).isEqualTo(6);
    containsOnlyIds(result, "DO2", "DO3", "DO4", "DO5", "DO8", "DO9");
  }

  @Test
  public void nestedTest() {
    val result = executeQuery("nested(gene.ssm.observation, " +
        "ge(gene.ssm.observation.quality_score, 49), ge(gene.ssm.observation.probability, 27))");
    assertThat(result.getHits().getTotalHits()).isEqualTo(1);
    containsOnlyIds(result, "DO2");
  }

  @Test
  public void facetsTest_noFilters() {
    val result = executeQuery("facets(donor_sex)");
    val facet = result.getFacets().facet("donor_sex");
    log.info("Facet - {}", facet);
    fail("Implement");
  }

  @Test
  public void facetsTest_noMatchFilter() {
    val result = executeQuery("facets(donor_sex), eq(project._project_id, 'PACA-AU')");
    val facet = result.getFacets().facet("donor_sex");
    log.info("Facet - {}", facet);
    fail("Implement");
  }

  @Test
  public void facetsTest_matchFilter() {
    val result = executeQuery("facets(donor_sex), eq(donor_sex, 'female')");
    val facet = result.getFacets().facet("donor_sex");
    log.info("Facet - {}", facet);
    fail("Implement");
  }

  @Test
  public void facetsTest_multiFilters() {
    val result = executeQuery("facets(donor_sex), eq(donor_sex, 'female'), eq(project._project_id, 'PACA-AU')");
    val facet = result.getFacets().facet("donor_sex");
    log.info("Facet - {}", facet);
    fail("Implement");
  }

  private SearchResponse executeQuery(String query) {
    ExpressionNode esAst = createTree(query);
    esAst = esAstTransformator.process(esAst, Type.DONOR_CENTRIC);
    log.debug("ES AST: {}", esAst);
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
