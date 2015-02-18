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
import static org.icgc.dcc.portal.model.IndexModel.Type.MUTATION_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.model.RequestType;
import org.dcc.portal.pql.es.utils.EsAstTransformator;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.meta.MutationCentricTypeModel;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.qe.QueryContext;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.terms.TermsFacet;
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
    es.execute(createIndexMappings(MUTATION_CENTRIC).withData(bulkFile(getClass())));
    visitor = new CreateFilterBuilderVisitor(es.client(), new MutationCentricTypeModel());
    queryContext = new QueryContext();
    queryContext.setType(MUTATION_CENTRIC);
    queryContext.setIndex(INDEX_NAME);
    listener = new PqlParseListener(queryContext);
    esAstTransformator = new EsAstTransformator();
  }

  @Test
  public void sortTest() {
    val result = executeQuery("select(start),sort(-start)");
    assertThat(result.getHits().getAt(0).getSortValues()[0]).isEqualTo(61020906L);

  }

  @Test
  public void selectTest() {
    val result = executeQuery("select(chromosome)");
    val hit = result.getHits().getAt(0);
    assertThat(hit.fields().size()).isEqualTo(1);
    assertThat(hit.field("chromosome").getValue()).isEqualTo("1");
  }

  @Test
  public void countTest() {
    val esAst = createTree("count()");
    queryContext.setRequestType(RequestType.COUNT);
    val request = visitor.visit(esAst, queryContext);
    val result = request.execute().actionGet();
    assertTotalHitsCount(result, 3);
  }

  @Test
  public void countTest_withFilter() {
    val esAst = createTree("count(), gt(start, 60000000)");
    queryContext.setRequestType(RequestType.COUNT);
    val request = visitor.visit(esAst, queryContext);
    val result = request.execute().actionGet();
    assertTotalHitsCount(result, 1);
  }

  @Test
  public void inTest() {
    val result = executeQuery("in(chromosome, '1', '2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void inTest_nested() {
    val result = executeQuery("in(sequencingStrategyNested, 'WGA', 'WGD')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void eqTest() {
    val result = executeQuery("eq(id, 'MU2')");
    assertTotalHitsCount(result, 1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("MU2");
  }

  @Test
  public void eqTest_nested() {
    val result = executeQuery("eq(functionalImpactNested, 'Low')");
    assertTotalHitsCount(result, 1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("MU1");
  }

  @Test
  public void neTest() {
    val result = executeQuery("ne(id, 'MU1')");
    assertTotalHitsCount(result, 2);
    for (val hit : result.getHits()) {
      assertThat(hit.getId()).isNotEqualTo("MU1");
    }
  }

  @Test
  public void neTest_nested() {
    val result = executeQuery("ne(functionalImpactNested, 'Low')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void gtTest() {
    val result = executeQuery("gt(testedDonorCount, 200)");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void gtTest_nested() {
    val result = executeQuery("gt(transcriptId, 'T2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void geTest() {
    val result = executeQuery("ge(testedDonorCount, 200)");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void geTest_nested() {
    val result = executeQuery("ge(transcriptId, 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void leTest() {
    val result = executeQuery("le(id, 'MU2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void leTest_nested() {
    val result = executeQuery("le(transcriptId, 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void ltTest() {
    val result = executeQuery("lt(id, 'MU2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void ltTest_nested() {
    val result = executeQuery("lt(transcriptId, 'T2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void andTest() {
    val result =
        executeQuery("and(eq(verificationStatusNested, 'tested'), eq(sequencingStrategyNested, 'WGE')))");
    assertTotalHitsCount(result, 1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("MU2");
  }

  @Test
  public void andTest_rootLevel() {
    val result = executeQuery("eq(verificationStatusNested, 'tested'), eq(sequencingStrategyNested, 'WGE')");
    assertTotalHitsCount(result, 1);
    assertThat(result.getHits().getAt(0).getId()).isEqualTo("MU2");
  }

  @Test
  public void orTest() {
    val result =
        executeQuery("or(eq(verificationStatusNested, 'tested'), eq(sequencingStrategyNested, 'WGA')))");
    assertTotalHitsCount(result, 3);
    containsOnlyIds(result, "MU1", "MU2", "MU3");
  }

  @Test
  public void nestedTest() {
    val result = executeQuery("nested(ssm_occurrence.observation, " +
        "eq(verificationStatusNested, 'tested'), lt(sequencingStrategyNested, 'WGE'))");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU2");
  }

  @Test
  public void facetsTest_noFilters() {
    val result = executeQuery("facets(verificationStatusNested)");
    assertTotalHitsCount(result, 3);

    val facet = getFacet(result);
    assertThat(facet.getMissingCount()).isEqualTo(0);
    assertThat(facet.getOtherCount()).isEqualTo(0);
    assertThat(facet.getTotalCount()).isEqualTo(6);

    for (val entry : facet.getEntries()) {
      if (entry.getTerm().toString().equals("tested")) {
        assertThat(entry.getCount()).isEqualTo(2);
      } else {
        assertThat(entry.getCount()).isEqualTo(4);
      }
    }
  }

  @Test
  public void facetsTest_noMatchFilter() {
    val result = executeQuery("facets(verificationStatus), in(transcriptId, 'T1', 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");

    val facet = result.getFacets().facet(TermsFacet.class, "verificationStatus");
    assertThat(facet.getMissingCount()).isEqualTo(0);
    assertThat(facet.getOtherCount()).isEqualTo(0);
    assertThat(facet.getTotalCount()).isEqualTo(2);

    assertThat(facet.getEntries()).hasSize(1);
    assertThat(facet.getEntries().get(0).getCount()).isEqualTo(2);
  }

  @Test
  public void facetsTest_matchFilter() {
    val result = executeQuery("facets(verificationStatusNested), eq(verificationStatusNested, 'tested')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");

    val facet = getFacet(result);
    assertThat(facet.getMissingCount()).isEqualTo(0);
    assertThat(facet.getOtherCount()).isEqualTo(0);
    assertThat(facet.getTotalCount()).isEqualTo(6);

    for (val entry : facet.getEntries()) {
      if (entry.getTerm().toString().equals("tested")) {
        assertThat(entry.getCount()).isEqualTo(2);
      } else {
        assertThat(entry.getCount()).isEqualTo(4);
      }
    }
  }

  @Test
  public void facetsTest_multiFilters() {
    val result =
        executeQuery("facets(verificationStatus), eq(verificationStatus, 'tested'), in(transcriptId, 'T1', 'T2')");
    assertTotalHitsCount(result, 0);

    val facet = result.getFacets().facet(TermsFacet.class, "verificationStatus");
    assertThat(facet.getMissingCount()).isEqualTo(0);
    assertThat(facet.getOtherCount()).isEqualTo(0);
    assertThat(facet.getTotalCount()).isEqualTo(2);

    assertThat(facet.getEntries()).hasSize(1);
    assertThat(facet.getEntries().get(0).getCount()).isEqualTo(2);
  }

  private SearchResponse executeQuery(String query) {
    ExpressionNode esAst = createTree(query);
    esAst = esAstTransformator.process(esAst, Type.MUTATION_CENTRIC);
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

  private static void assertTotalHitsCount(SearchResponse response, int expectedCount) {
    assertThat(response.getHits().getTotalHits()).isEqualTo(expectedCount);
  }

  private static TermsFacet getFacet(SearchResponse response) {
    return response.getFacets().facet(TermsFacet.class, "verificationStatusNested");
  }

}
