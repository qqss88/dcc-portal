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
package org.dcc.portal.pql.es.utils;

import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.EmptyNodesCleanerVisitor;
import org.dcc.portal.pql.es.visitor.FacetsResolverVisitor;
import org.icgc.dcc.portal.model.IndexModel.Type;

/**
 * Performs series of transformations to optimize the AST
 */
@Slf4j
public class EsAstTransformator {

  private final EmptyNodesCleanerVisitor emptyNodesCleaner = new EmptyNodesCleanerVisitor();
  private final FacetsResolverVisitor facetsResolver = new FacetsResolverVisitor();

  public ExpressionNode process(ExpressionNode esAst, Type type) {
    log.debug("Running all ES AST Transformators. Original ES AST: {}", esAst);
    esAst = resolveFacets(esAst, type);
    esAst = optimize(esAst);
    log.debug("ES AST after the transformations: {}", esAst);

    return esAst;
  }

  public ExpressionNode optimize(ExpressionNode esAst) {
    return esAst.accept(emptyNodesCleaner);
  }

  public ExpressionNode resolveFacets(ExpressionNode esAst, Type type) {
    return facetsResolver.resolveFacets(esAst, type);
  }

}
