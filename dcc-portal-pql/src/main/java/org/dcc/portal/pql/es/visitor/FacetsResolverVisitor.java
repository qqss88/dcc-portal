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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.Nodes.getChildOptional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FacetsNode;
import org.dcc.portal.pql.es.ast.FilterNode;
import org.dcc.portal.pql.es.ast.QueryNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TermsFacetNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.icgc.dcc.portal.model.IndexModel.Type;

import com.google.common.base.Optional;

/**
 * Processes facets. Moves FilterNode to a QueryNode. Copies the FilterNode to facet's filter node except filters for
 * facets being calculated.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class FacetsResolverVisitor extends NodeVisitor<ExpressionNode> {

  private Type type;
  private final FacetFiltersVisitor facetsFilterVisitor = new FacetFiltersVisitor();

  public ExpressionNode resolveFacets(@NonNull ExpressionNode sourceAst, @NonNull Type type) {
    checkArgument(sourceAst instanceof RootNode, "Source AST must be an instance of RootNode");
    this.type = type;
    val result = sourceAst.accept(this);

    return result;
  }

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode rootNode) {
    log.debug("Resolving facets for type {}. Source AST: {}", type.getId(), rootNode);

    if (!hasFacets(rootNode)) {
      log.debug("The source AST does not contain a FacetsNode. Returning the original source AST back.");
      return rootNode;
    }

    val filterNodeOpt = getFilterNodeOptional(rootNode);
    if (!filterNodeOpt.isPresent()) {
      log.debug("The source AST does not contain any FilterNodes. Returning the original source AST back.");
      return rootNode;
    }

    // FilterNode is altered during the facets processing. We need to clone the structure to have the original filter.
    val filterNodeClone = Nodes.cloneNode(filterNodeOpt.get());
    val facetsNode = getFacetsNodeOptional(rootNode).get();
    for (val child : facetsNode.getChildren()) {
      child.accept(this);
    }
    moveFiltersToQuery(rootNode, filterNodeClone);

    return rootNode;
  }

  @Override
  public ExpressionNode visitTermsFacet(TermsFacetNode node) {
    log.debug("Visiting TermsFacetNode. {}", node);
    val rootNode = node.getParent().getParent();
    val filtersNodeOpt = getChildOptional(rootNode, FilterNode.class);
    if (filtersNodeOpt.isPresent()) {

      // The FilterNode must be added to the TermsFacetNode before FilterNode processing. During the FilterNode
      // processing we are looking for TermsFilterNode to set the global scope of the query. If FilterNode is not added
      // it's not possible to find TermsFacetNode.
      node.addChildren(filtersNodeOpt.get());
      processFilter(node.getField(), filtersNodeOpt.get());
    }

    return node;
  }

  private void processFilter(String facetField, FilterNode filterNode) {
    facetsFilterVisitor.visit(facetField, filterNode);
  }

  private Optional<FacetsNode> getFacetsNodeOptional(ExpressionNode rootNode) {
    return getChildOptional(rootNode, FacetsNode.class);
  }

  private boolean hasFacets(ExpressionNode rootNode) {
    return getChildOptional(rootNode, FacetsNode.class).isPresent() ? true : false;
  }

  private static void moveFiltersToQuery(ExpressionNode rootNode, ExpressionNode filters) {
    val filterNodeIndex = getFilterNodeIndex(rootNode);
    rootNode.removeChild(filterNodeIndex);
    rootNode.addChildren(new QueryNode(filters));
  }

  private static int getFilterNodeIndex(ExpressionNode rootNode) {
    for (int i = 0; i < rootNode.childrenCount(); i++) {
      if (rootNode.getChild(i) instanceof FilterNode) {
        return i;
      }
    }

    throw new IllegalStateException(format("Could not find FilterNode in RootNode: %s", rootNode));
  }

  private static Optional<FilterNode> getFilterNodeOptional(ExpressionNode rootNode) {
    return getChildOptional(rootNode, FilterNode.class);
  }

}
