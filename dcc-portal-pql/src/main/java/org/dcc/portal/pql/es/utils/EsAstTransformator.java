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

import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.Visitors.createAggregationsResolverVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createEmptyNodesCleanerVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createEntitySetVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createFieldsToSourceVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createGeneSetFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createLocationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createMissingAggregationVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createNestedAggregationVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createQuerySimplifierVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createRemoveAggregationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createScoreSortVisitor;
import static org.dcc.portal.pql.meta.IndexModel.getDonorCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getGeneCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getObservationCentricTypeModel;

import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.visitor.aggs.Context;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * Performs series of transformations to resolve different processing rules and to optimize the AST
 */
@Slf4j
@NoArgsConstructor
public class EsAstTransformator {

  private static final Optional<Context> DONOR_RESOLVE_FACETS_CONTEXT = Optional.of(new Context(null,
      getDonorCentricTypeModel()));
  private static final Optional<Context> GENE_RESOLVE_FACETS_CONTEXT = Optional.of(new Context(null,
      getGeneCentricTypeModel()));
  private static final Optional<Context> MUTATION_RESOLVE_FACETS_CONTEXT = Optional.of(new Context(null,
      getMutationCentricTypeModel()));
  private static final Optional<Context> OBSERVATION_RESOLVE_FACETS_CONTEXT = Optional.of(new Context(null,
      getObservationCentricTypeModel()));

  public ExpressionNode process(@NonNull ExpressionNode esAst, @NonNull QueryContext context) {
    log.debug("Running all ES AST Transformators. Original ES AST: {}", esAst);
    esAst = resolveSpecialCases(esAst, context);
    esAst = resolveFacets(esAst, context.getTypeModel());
    esAst = score(esAst, context);
    esAst = optimize(esAst);
    log.debug("ES AST after the transformations: {}", esAst);

    return esAst;
  }

  public ExpressionNode score(ExpressionNode esAst, QueryContext context) {
    log.debug("[score] Before: {}", esAst);
    val result = esAst.accept(Visitors.createScoreQueryVisitor(context.getType()), Optional.of(context));
    log.debug("[score] After: {}", result);

    return result;
  }

  public ExpressionNode resolveSpecialCases(ExpressionNode esAst, QueryContext context) {
    log.debug("[resoveSpecialCases] Before: {}", esAst);
    esAst = esAst.accept(createFieldsToSourceVisitor(), Optional.of(context)).get();
    esAst = esAst.accept(createEntitySetVisitor(), Optional.of(context)).get();
    esAst = esAst.accept(createScoreSortVisitor(), Optional.empty());
    esAst = esAst.accept(createGeneSetFilterVisitor(), Optional.of(context)).get();
    esAst = esAst.accept(createLocationFilterVisitor(), Optional.of(context)).get();
    log.debug("[resoveSpecialCases] After: {}", esAst);

    return esAst;
  }

  public ExpressionNode optimize(ExpressionNode esAst) {
    log.debug("[optimize] Before: {}", esAst);
    esAst = esAst.accept(createEmptyNodesCleanerVisitor(), Optional.empty());

    // Remove FilterAggregationNodes without filters
    val aggsNode = Nodes.getOptionalChild(esAst, AggregationsNode.class);
    if (aggsNode.isPresent()) {
      esAst = esAst.accept(createRemoveAggregationFilterVisitor(), Optional.empty()).get();
    }

    esAst = esAst.accept(createQuerySimplifierVisitor(), Optional.empty()).get();
    log.debug("[optimize] After: {}", esAst);

    return esAst;
  }

  public ExpressionNode resolveFacets(ExpressionNode esAst, TypeModel typeModel) {
    log.debug("[resolveFacets] Before: {}", esAst);
    esAst = esAst.accept(createAggregationsResolverVisitor(), createResolveFacetsContext(typeModel.getType())).get();
    esAst = esAst.accept(createMissingAggregationVisitor(), Optional.empty());
    esAst = esAst.accept(createNestedAggregationVisitor(), Optional.of(typeModel));
    log.debug("[resolveFacets] After: {}", esAst);

    return esAst;
  }

  private Optional<Context> createResolveFacetsContext(Type indexType) {
    switch (indexType) {
    case DONOR_CENTRIC:
      return DONOR_RESOLVE_FACETS_CONTEXT;
    case GENE_CENTRIC:
      return GENE_RESOLVE_FACETS_CONTEXT;
    case MUTATION_CENTRIC:
      return MUTATION_RESOLVE_FACETS_CONTEXT;
    case OBSERVATION_CENTRIC:
      return OBSERVATION_RESOLVE_FACETS_CONTEXT;
    default:
      throw new IllegalArgumentException(format("Unknown index type '%s'", indexType.getId()));
    }
  }

}
