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

import static org.dcc.portal.pql.es.utils.Visitors.checkOptional;
import static org.dcc.portal.pql.meta.AbstractTypeModel.BIOLOGICAL_PROCESS;
import static org.dcc.portal.pql.meta.AbstractTypeModel.CELLULAR_COMPONENT;
import static org.dcc.portal.pql.meta.AbstractTypeModel.GENE_CURATED_SET_ID;
import static org.dcc.portal.pql.meta.AbstractTypeModel.GENE_GO_TERM_ID;
import static org.dcc.portal.pql.meta.AbstractTypeModel.GENE_PATHWAY_ID;
import static org.dcc.portal.pql.meta.AbstractTypeModel.GENE_SET_ID;
import static org.dcc.portal.pql.meta.AbstractTypeModel.MOLECULAR_FUNCTION;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.OrNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.AbstractTypeModel;
import org.dcc.portal.pql.qe.QueryContext;

/**
 * Resolves GeneSet filters.
 */
@Slf4j
public class GeneSetFilterVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val filterNode = Nodes.getOptionalChild(node, FilterNode.class);
    if (filterNode.isPresent()) {
      filterNode.get().accept(this, context);
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    log.debug("[visitTerms]. Field name: {}", node.getField());
    if (node.getField().equals(GENE_GO_TERM_ID)) {
      return resolveGoTermArray(node, context.get().getTypeModel());
    }

    if (node.getField().equals(GENE_SET_ID)) {
      return resolveGeneSetIdArray(node, context.get().getTypeModel());
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitAnd(@NonNull AndNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitOr(@NonNull OrNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRange(@NonNull RangeNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitExists(@NonNull ExistsNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  private Optional<ExpressionNode> visitChildren(ExpressionNode parent, Optional<QueryContext> context) {
    for (int i = 0; i < parent.childrenCount(); i++) {
      val child = parent.getChild(i);
      val childResult = child.accept(this, context);
      if (childResult.isPresent()) {
        parent.setChild(i, childResult.get());
      }
    }

    return Optional.empty();
  }

  private Optional<ExpressionNode> resolveGeneSetIdArray(TermsNode termsNode, AbstractTypeModel typeModel) {
    val orNode = new OrNode(createGoTermChildren(termsNode, typeModel));
    orNode.addChildren(createPathwayAndCuratedSetIdNodes(termsNode, typeModel));
    val fullyQualifiedName = typeModel.getInternalField(CELLULAR_COMPONENT);

    if (typeModel.isNested(fullyQualifiedName)) {
      return createNestedNodeOptional(typeModel.getNestedPath(fullyQualifiedName), orNode);
    }

    return Optional.of(orNode);
  }

  private ExpressionNode[] createPathwayAndCuratedSetIdNodes(TermsNode termsNode, AbstractTypeModel typeModel) {
    val pathwayTermsNode = new TermsNode(typeModel.getField(GENE_PATHWAY_ID), termsNode.getChildrenArray());
    val curatedSetIdTermsNode = new TermsNode(typeModel.getField(GENE_CURATED_SET_ID), termsNode.getChildrenArray());

    return new ExpressionNode[] { pathwayTermsNode, curatedSetIdTermsNode };
  }

  private static Optional<ExpressionNode> createNestedNodeOptional(String path, ExpressionNode... children) {
    return Optional.of(new NestedNode(path, children));
  }

  private Optional<ExpressionNode> resolveGoTermArray(TermsNode termsNode, AbstractTypeModel typeModel) {
    val orNode = new OrNode(createGoTermChildren(termsNode, typeModel));
    val fullyQualifiedName = typeModel.getInternalField(CELLULAR_COMPONENT);

    if (typeModel.isNested(fullyQualifiedName)) {
      return createNestedNodeOptional(typeModel.getNestedPath(fullyQualifiedName), orNode);
    }

    return Optional.of(orNode);
  }

  private static ExpressionNode[] createGoTermChildren(TermsNode termsNode, AbstractTypeModel typeModel) {
    ExpressionNode[] children = termsNode.getChildrenArray();

    return new ExpressionNode[] {
        new TermsNode(typeModel.getInternalField(CELLULAR_COMPONENT), children),
        new TermsNode(typeModel.getInternalField(BIOLOGICAL_PROCESS), children),
        new TermsNode(typeModel.getInternalField(MOLECULAR_FUNCTION), children) };
  }
}
