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
package org.dcc.portal.pql.es.visitor.special;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.utils.TestingHelpers.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.AndNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.qe.QueryContext;
import org.junit.Test;

@Slf4j
public class LocationFilterVisitorTest {

  private static final String GENE_FILTER = "in(gene.location, ['chr12:123-456'])";
  private static final String MUTATION_FILTER = "in(mutation.location, ['chr12:123-456'])";
  private static final String CHROMOSOME_VALUE = "12";
  private static final long CHROMOSOME_START = 123L;
  private static final long CHROMOSOME_END = 456L;

  LocationFilterVisitor visitor = new LocationFilterVisitor();
  QueryContext queryContext;

  @Test
  public void geneLocation_donor() {
    val root = createEsAst(GENE_FILTER, DONOR_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", DONOR_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "gene", GENE_CENTRIC);
  }

  @Test
  public void mutationLocation_donor() {
    val root = createEsAst(MUTATION_FILTER, DONOR_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", DONOR_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "gene.ssm", MUTATION_CENTRIC);
  }

  @Test
  public void geneLocation_gene() {
    val root = createEsAst(GENE_FILTER, GENE_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", GENE_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "", GENE_CENTRIC);
  }

  @Test
  public void mutationLocation_gene() {
    val root = createEsAst(MUTATION_FILTER, GENE_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", GENE_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "donor.ssm", MUTATION_CENTRIC);
  }

  @Test
  public void geneLocation_mutation() {
    val root = createEsAst(GENE_FILTER, MUTATION_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "transcript.gene", GENE_CENTRIC);
  }

  @Test
  public void mutationLocation_mutation() {
    val root = createEsAst(MUTATION_FILTER, MUTATION_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    assertGeneLocation(result, "", MUTATION_CENTRIC);
  }

  @Test
  public void nestedTest() {
    val root = createEsAst(format("nested(transcript, %s)", GENE_FILTER), MUTATION_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
    log.debug("After visitor: {}", result);

    // Query - Filter - Bool - Must - Nested - Bool - Must - And
    val andNode = (AndNode) result.getFirstChild().getFirstChild().getFirstChild().getFirstChild().getFirstChild()
        .getFirstChild().getFirstChild().getFirstChild().getFirstChild();
    val nestedNode = Nodes.findParent(andNode, NestedNode.class).get();
    assertThat(nestedNode.getPath()).isEqualTo("transcript");
    assertThat(Nodes.findParent(nestedNode, NestedNode.class).isPresent()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void wrongNestingLevel() {
    val root = createEsAst(format("nested(transcript.ssm, %s)", GENE_FILTER), MUTATION_CENTRIC);
    root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
  }

  private static void assertGeneLocation(ExpressionNode result, String prefix, Type type) {
    // QueryNode - FilterNode - BoolNode - MustBoolNode - NestedNode - AndNode
    AndNode andNode = null;
    if (prefix.equals("")) {
      andNode =
          (AndNode) result.getFirstChild().getFirstChild().getFirstChild().getFirstChild().getFirstChild()
              .getFirstChild();
    } else {
      val nestedNode =
          (NestedNode) result.getFirstChild().getFirstChild().getFirstChild().getFirstChild().getFirstChild();

      if (prefix.equals("transcript.gene")) {
        assertThat(nestedNode.getPath()).isEqualTo("transcript");
      } else {
        assertThat(nestedNode.getPath()).isEqualTo(prefix);
      }

      andNode = (AndNode) nestedNode.getFirstChild().getFirstChild();
    }

    assertThat(andNode.childrenCount()).isEqualTo(3);

    val termNode = (TermNode) andNode.getChild(0);
    assertThat(termNode.getNameNode().getValue()).isEqualTo(formatField(prefix, "chromosome"));
    assertThat(termNode.getValueNode().getValue()).isEqualTo(CHROMOSOME_VALUE);

    // GreaterEqual chromosome start
    RangeNode rangeNode = (RangeNode) andNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo(resolveStart(prefix, type));
    val geNode = (GreaterEqualNode) rangeNode.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(CHROMOSOME_START);

    // LessEqual chromosome end
    rangeNode = (RangeNode) andNode.getChild(2);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo(resolveEnd(prefix, type));
    val leNode = (LessEqualNode) rangeNode.getFirstChild();
    assertThat(leNode.getValue()).isEqualTo(CHROMOSOME_END);
  }

  private static String resolveStart(String prefix, Type type) {
    return type == GENE_CENTRIC ? formatField(prefix, "start") : formatField(prefix, "chromosome_start");
  }

  private static String resolveEnd(String prefix, Type type) {
    return type == GENE_CENTRIC ? formatField(prefix, "end") : formatField(prefix, "chromosome_end");
  }

  private static String formatField(String prefix, String field) {
    return prefix.isEmpty() ? field : format("%s.%s", prefix, field);
  }

}
