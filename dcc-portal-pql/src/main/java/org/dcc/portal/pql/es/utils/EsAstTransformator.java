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

import static org.dcc.portal.pql.es.visitor.Visitors.createRemoveAggregationFilterVisitor;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.visitor.AggregationsResolverVisitor;
import org.dcc.portal.pql.es.visitor.EmptyNodesCleanerVisitor;
import org.dcc.portal.pql.es.visitor.RemoveAggregationFilterVisitor;
import org.dcc.portal.pql.es.visitor.Visitors;
import org.icgc.dcc.portal.model.IndexModel.Type;

/**
 * Performs series of transformations to resolve different processing rules and to optimize the AST
 */
@Slf4j
public class EsAstTransformator {

  private final EmptyNodesCleanerVisitor emptyNodesCleaner = new EmptyNodesCleanerVisitor();
  private final AggregationsResolverVisitor facetsResolver = new AggregationsResolverVisitor();
  private final RemoveAggregationFilterVisitor removeAggsFilterVisitor = createRemoveAggregationFilterVisitor();

  public ExpressionNode process(ExpressionNode esAst, Type type) {
    log.debug("Running all ES AST Transformators. Original ES AST: {}", esAst);
    esAst = resolveFacets(esAst, type);
    esAst = optimize(esAst);
    esAst = score(esAst, type);
    log.debug("ES AST after the transformations: {}", esAst);

    return esAst;
  }

  private ExpressionNode score(ExpressionNode esAst, Type type) {
    return esAst.accept(Visitors.createScoreQueryVisitor(type), Optional.empty());
  }

  private ExpressionNode optimize(ExpressionNode esAst) {
    // Clean empty filter node
    esAst = esAst.accept(emptyNodesCleaner, Optional.empty());

    // Remove FilterAggregationNodes without filters
    val aggsNode = Nodes.getOptionalChild(esAst, AggregationsNode.class);
    if (aggsNode.isPresent()) {
      esAst = esAst.accept(removeAggsFilterVisitor, Optional.empty());
    }

    return esAst;
  }

  private ExpressionNode resolveFacets(ExpressionNode esAst, Type type) {
    return facetsResolver.resolveAggregations(esAst, type);
  }

}
