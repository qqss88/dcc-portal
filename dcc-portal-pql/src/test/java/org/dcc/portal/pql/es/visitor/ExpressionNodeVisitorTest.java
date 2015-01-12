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
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.builder.Builders;
import org.dcc.portal.pql.utils.TestingHelper;
import org.elasticsearch.index.query.FilterBuilder;
import org.junit.Test;

@Slf4j
public class ExpressionNodeVisitorTest {

  ExpressionNodeVisitor<FilterBuilder> visitor = new ExpressionNodeVisitorImpl();

  @Test
  public void visitTermTest() {
    val termNode = Builders.termNode("sex", "male");
    val json = TestingHelper.getJson(termNode.accept(visitor));
    val value = json.path("term").path("sex").asText();
    assertThat(value).isEqualTo("male");
  }

  @Test
  public void visitMustTest() {
    val termNode = Builders.termNode("sex", "male");
    val mustNode = Builders.mustNode(null, termNode);
    val json = mustNode.accept(visitor);
    assertThat(json).isNull();
  }

  @Test
  public void visitBoolTest() {
    val boolNode = TestingHelper.createTwoTermsTree();
    val json = TestingHelper.getJson(boolNode.accept(visitor));
    log.info("{}", json);
    val mustArray = json.path("bool").path("must");
    assertThat(mustArray.isArray()).isTrue();
    // FIXME: FINISH
  }

}
