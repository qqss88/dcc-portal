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
package org.dcc.portal.pql.qe;

import lombok.NonNull;
import lombok.Value;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.EsAstTransformator;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.es.visitor.CreateFilterBuilderVisitor;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;

@Value
public class QueryEngine {

  @NonNull
  Client client;

  @NonNull
  String index;
  private final EsAstTransformator esAstTransformator = new EsAstTransformator();

  public SearchRequestBuilder execute(@NonNull String query, @NonNull QueryContext context) {
    context.setIndex(index);

    ExpressionNode esAst = resolveEsAst(query, context);
    esAst = esAstTransformator.process(esAst, context.getType());

    // FIXME: Check if following can be put into the esAstTransformator
    val esVisitor = new CreateFilterBuilderVisitor(client, context.getTypeModel());

    return esVisitor.visit(esAst, context);
  }

  private static ExpressionNode resolveEsAst(String query, QueryContext context) {
    val parser = ParseTrees.getParser(query);
    val pqlListener = new PqlParseListener(context);
    parser.addParseListener(pqlListener);
    parser.statement();

    return pqlListener.getEsAst();
  }

}
