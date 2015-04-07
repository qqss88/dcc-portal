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
package org.dcc.portal.pql.utils;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.es.utils.ParseTrees.getParser;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.antlr.v4.runtime.tree.ParseTree;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.qe.QueryContext;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class TestingHelpers {

  public static ParseTree createParseTree(String query) {
    val parser = getParser(query);

    return parser.statement();
  }

  public static ExpressionNode createEsAst(@NonNull String query, PqlParseListener listener) {
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();
    val esAst = listener.getEsAst();
    log.debug("ES AST: - {}", esAst);

    return esAst;
  }

  public static ExpressionNode createEsAst(@NonNull String query) {
    return createEsAst(query, new PqlParseListener(initQueryContext()));
  }

  public static ExpressionNode createEsAst(@NonNull String query, @NonNull Type type) {
    return createEsAst(query, new PqlParseListener(initQueryContext(type)));
  }

  public static QueryContext initQueryContext() {
    return new QueryContext("", Type.DONOR_CENTRIC);
  }

  public static QueryContext initQueryContext(Type type) {

    switch (type) {
    case DONOR_CENTRIC:
      return new QueryContext("", Type.DONOR_CENTRIC);
    case GENE_CENTRIC:
      return new QueryContext("", Type.GENE_CENTRIC);
    case MUTATION_CENTRIC:
      return new QueryContext("", Type.MUTATION_CENTRIC);
    }

    throw new IllegalArgumentException(format("Type %s is not supported", type.getId()));
  }

  public static void assertTermNode(@NonNull ExpressionNode node, @NonNull String field, @NonNull Object value) {
    val termNode = (TermNode) node;
    assertThat(termNode.getNameNode().getValueAsString()).isEqualTo(field);
    assertThat(termNode.getValueNode().getValue()).isEqualTo(value);
  }

  public static MustBoolNode assertBoolAndGetMustNode(@NonNull ExpressionNode node) {
    val boolNode = (BoolNode) node;
    assertThat(boolNode.childrenCount()).isEqualTo(1);

    return (MustBoolNode) boolNode.getFirstChild();
  }

  public static NestedNode assertAndGetNestedNode(@NonNull ExpressionNode node, @NonNull String path) {
    val nestedNode = (NestedNode) node;
    assertThat(nestedNode.getPath()).isEqualTo(path);
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.AVG);

    return nestedNode;
  }

}
