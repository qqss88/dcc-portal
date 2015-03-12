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

import static org.dcc.portal.pql.es.utils.Visitors.createAggregationsResolverVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createEmptyNodesCleanerVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createGeneSetFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createLocationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createRemoveAggregationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createScoreMutatationQueryVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createScoreSortVisitor;

import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.visitor.aggs.AggregationsResolverVisitor;
import org.dcc.portal.pql.es.visitor.aggs.RemoveAggregationFilterVisitor;
import org.dcc.portal.pql.es.visitor.special.EntitySetVisitor;
import org.dcc.portal.pql.es.visitor.special.GeneSetFilterVisitor;
import org.dcc.portal.pql.es.visitor.special.LocationFilterVisitor;
import org.dcc.portal.pql.es.visitor.special.ScoreSortVisitor;
import org.dcc.portal.pql.es.visitor.util.EmptyNodesCleanerVisitor;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * Performs series of transformations to resolve different processing rules and to optimize the AST
 */
@Slf4j
@NoArgsConstructor
public class EsAstTransformator {

  private final EmptyNodesCleanerVisitor emptyNodesCleaner = createEmptyNodesCleanerVisitor();
  private final AggregationsResolverVisitor facetsResolver = createAggregationsResolverVisitor();
  private final RemoveAggregationFilterVisitor removeAggsFilterVisitor = createRemoveAggregationFilterVisitor();
  private final GeneSetFilterVisitor geneSetFilterVisitor = createGeneSetFilterVisitor();
  private final LocationFilterVisitor locationFilterVisitor = createLocationFilterVisitor();
  private final ScoreSortVisitor scoreSortVisitor = createScoreSortVisitor();
  private final EntitySetVisitor entitySetVisitor = Visitors.createEntitySetVisitor();

  public ExpressionNode process(ExpressionNode esAst, QueryContext context) {
    log.debug("Running all ES AST Transformators. Original ES AST: {}", esAst);
    esAst = resoveSpecialCases(esAst, context);
    esAst = resolveFacets(esAst);
    esAst = optimize(esAst);
    esAst = score(esAst, context);

    // Must be run last. Read the class description
    esAst = esAst.accept(createScoreMutatationQueryVisitor(), Optional.of(context)).get();
    log.debug("ES AST after the transformations: {}", esAst);

    return esAst;
  }

  private ExpressionNode score(ExpressionNode esAst, QueryContext context) {
    return esAst.accept(Visitors.createScoreQueryVisitor(context.getType()), Optional.of(context));
  }

  private ExpressionNode resoveSpecialCases(ExpressionNode esAst, QueryContext context) {
    esAst = esAst.accept(entitySetVisitor, Optional.of(context)).get();
    esAst = esAst.accept(scoreSortVisitor, Optional.empty());
    esAst = esAst.accept(geneSetFilterVisitor, Optional.of(context)).get();
    esAst = esAst.accept(locationFilterVisitor, Optional.of(context)).get();

    return esAst;
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

  private ExpressionNode resolveFacets(ExpressionNode esAst) {
    return esAst.accept(facetsResolver, Optional.empty());
  }

}
