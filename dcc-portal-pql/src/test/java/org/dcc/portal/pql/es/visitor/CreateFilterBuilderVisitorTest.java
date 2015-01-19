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
import static org.dcc.portal.pql.utils.TestingHelpers.getJson;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.utils.BaseRepositoryTest;
import org.dcc.portal.pql.utils.TestingHelpers;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class CreateFilterBuilderVisitorTest extends BaseRepositoryTest {

  private static final PqlParseListener LISTENER = new PqlParseListener();
  private CreateFilterBuilderVisitor visitor;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(Type.GENE, Type.GENE_CENTRIC).withData(bulkFile(getClass())));
    visitor = new CreateFilterBuilderVisitor(null, es.client());
  }

  @Test
  public void visitTest() {
    val query = "or(eq(one, 1), ne(two, 2))&and(eq(three, 3), ne(four, 4))";
    val esAst = createTree(query);
    val result = visitor.visit(esAst);

    assertThat(result).isNotNull();
    log.info("{}", result);
  }

  @Test
  public void visitTermTest() {
    val termNode = new TermNode("sex", "male");
    val json = getJson(termNode.accept(visitor));
    val value = json.path("term").path("sex").asText();
    assertThat(value).isEqualTo("male");
  }

  @Test
  public void visitRangeTest() {
    val query = "and(ge(age, 10), le(age, 50))";
    val esAst = createTree(query);
    val rangeNode = esAst.getChild(0).getChild(0).getChild(0).getChild(0);
    val eqQuery = getJson(rangeNode.accept((visitor)));
    log.info("ES query - {}", eqQuery);
    val ageNode = eqQuery.path("range").path("age");
    assertThat(ageNode.path("from").asInt()).isEqualTo(10);
    assertThat(ageNode.path("to")).isEmpty();
    assertThat(ageNode.path("include_lower").asBoolean()).isTrue();
    assertThat(ageNode.path("include_upper").asBoolean()).isTrue();
  }

  @Test
  public void visitBoolTest() {
    val query =
        "and(eq(one, 1), ne(two, 2), gt(five, 5))&or(ge(six, 6), eq(three, 3), ne(four, 4))&and(lt(seven, 7), le(eight, 8))&or(lt(nine, 9), le(ten, 10))";
    val tree = createTree(query);
    val boolNode = tree.getChild(0).getChild(0);
    log.info("Bool Tree: {}", boolNode);
    val json = TestingHelpers.getJson(boolNode.accept(visitor));
    log.info("{}", json);
    val mustArray = json.path("bool").path("must");
    log.info("{}", mustArray);
    // FIXME: Finish
  }

  @Test
  public void visitBoolTest_greater() {
    val query = "and(ge(age, 10), le(age, 50))";
    val tree = createTree(query);
    val boolNode = tree.getChild(0).getChild(0);
    log.info("Bool Tree: {}", boolNode);
    val json = TestingHelpers.getJson(boolNode.accept(visitor));
    log.info("{}", json);

    val ageNode1 = json.path("bool").path("must").get(0).path("range").path("age");
    assertThat(ageNode1.path("from").asInt()).isEqualTo(10);
    assertThat(ageNode1.path("to")).isEmpty();
    assertThat(ageNode1.path("include_lower").asBoolean()).isTrue();
    assertThat(ageNode1.path("include_upper").asBoolean()).isTrue();

    val ageNode2 = json.path("bool").path("must").get(1).path("range").path("age");
    assertThat(ageNode2.path("from")).isEmpty();
    assertThat(ageNode2.path("to").asInt()).isEqualTo(50);
    assertThat(ageNode2.path("include_lower").asBoolean()).isTrue();
    assertThat(ageNode2.path("include_upper").asBoolean()).isTrue();
  }

  private static ExpressionNode createTree(String query) {
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(LISTENER);
    parser.program();

    return LISTENER.getEsAst();
  }

}
