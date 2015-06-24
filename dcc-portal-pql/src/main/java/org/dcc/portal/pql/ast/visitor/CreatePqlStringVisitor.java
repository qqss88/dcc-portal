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
package org.dcc.portal.pql.ast.visitor;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.dcc.portal.pql.util.Converters.asString;
import static org.dcc.portal.pql.util.Converters.isString;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;

import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.EqualityFilterNode;
import org.dcc.portal.pql.ast.filter.ExistsNode;
import org.dcc.portal.pql.ast.filter.GeNode;
import org.dcc.portal.pql.ast.filter.GtNode;
import org.dcc.portal.pql.ast.filter.InNode;
import org.dcc.portal.pql.ast.filter.LeNode;
import org.dcc.portal.pql.ast.filter.LtNode;
import org.dcc.portal.pql.ast.filter.MissingNode;
import org.dcc.portal.pql.ast.filter.NeNode;
import org.dcc.portal.pql.ast.filter.NestedNode;
import org.dcc.portal.pql.ast.filter.NotNode;
import org.dcc.portal.pql.ast.filter.OrNode;
import org.dcc.portal.pql.ast.function.CountNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;

public class CreatePqlStringVisitor extends PqlNodeVisitor<String, Void> {

  private static final String SEPARATOR = ",";

  @Override
  public String visitStatement(@NonNull StatementNode node, Optional<Void> context) {
    val result = new StringBuilder();
    result.append(visitChildren(node));

    return result.toString();
  }

  @Override
  public String visitEq(@NonNull EqNode node, Optional<Void> context) {
    return visitEqualityNode("eq(%s,%s)", node);
  }

  @Override
  public String visitNe(@NonNull NeNode node, Optional<Void> context) {
    return visitEqualityNode("ne(%s,%s)", node);
  }

  @Override
  public String visitGt(@NonNull GtNode node, Optional<Void> context) {
    return visitEqualityNode("gt(%s,%s)", node);
  }

  @Override
  public String visitGe(@NonNull GeNode node, Optional<Void> context) {
    return visitEqualityNode("ge(%s,%s)", node);
  }

  @Override
  public String visitLt(@NonNull LtNode node, Optional<Void> context) {
    return visitEqualityNode("lt(%s,%s)", node);
  }

  @Override
  public String visitLe(@NonNull LeNode node, Optional<Void> context) {
    return visitEqualityNode("le(%s,%s)", node);
  }

  @Override
  public String visitAnd(@NonNull AndNode node, Optional<Void> context) {
    return format("and(%s)", visitChildren(node));
  }

  @Override
  public String visitOr(@NonNull OrNode node, Optional<Void> context) {
    return format("or(%s)", visitChildren(node));
  }

  @Override
  public String visitNot(@NonNull NotNode node, Optional<Void> context) {
    return format("not(%s)", visitChildren(node));
  }

  @Override
  public String visitNested(@NonNull NestedNode node, Optional<Void> context) {
    return format("nested(%s,%s)", node.getPath(), visitChildren(node));
  }

  @Override
  public String visitExists(@NonNull ExistsNode node, Optional<Void> context) {
    return format("exists(%s)", node.getField());
  }

  @Override
  public String visitMissing(@NonNull MissingNode node, Optional<Void> context) {
    return format("missing(%s)", node.getField());
  }

  @Override
  public String visitIn(@NonNull InNode node, Optional<Void> context) {
    return format("in(%s,%s)", node.getField(), resolveValues(node.getValues()));
  }

  @Override
  public String visitCount(@NonNull CountNode node, Optional<Void> context) {
    return "count()";
  }

  @Override
  public String visitFacets(@NonNull FacetsNode node, Optional<Void> context) {
    return format("facets(%s)", COMMA.join(node.getFacets()));
  }

  @Override
  public String visitLimit(@NonNull LimitNode node, Optional<Void> context) {
    return format("limit(%s,%s)", node.getFrom(), node.getSize());
  }

  @Override
  public String visitSelect(@NonNull SelectNode node, Optional<Void> context) {
    return format("select(%s)", COMMA.join(node.getFields()));
  }

  @Override
  public String visitSort(@NonNull SortNode node, Optional<Void> context) {
    val fields = new StringBuilder();
    int index = 0;
    for (val entry : node.getFields().entrySet()) {
      fields.append(entry.getValue().getSign());
      fields.append(entry.getKey());

      if (!isLastIndex(index++, node.getFields().size())) {
        fields.append(SEPARATOR);
      }
    }

    return format("sort(%s)", fields.toString());
  }

  private static String resolveValues(List<? extends Object> values) {
    val result = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      val value = values.get(i);
      result.append(isString(value) ? asString(value) : value);
      if (!isLastIndex(i, values.size())) {
        result.append(SEPARATOR);
      }
    }

    return result.toString();
  }

  private static String visitEqualityNode(String template, EqualityFilterNode node) {
    val rawValue = node.getValue();
    val value = isString(rawValue) ? asString(rawValue) : valueOf(rawValue);

    return format(template, node.getField(), value);
  }

  private String visitChildren(PqlNode node) {
    val result = new StringBuilder();
    for (int i = 0; i < node.childrenCount(); i++) {
      val child = node.getChild(i);
      result.append(child.accept(this, Optional.empty()));
      if (!isLastIndex(i, node.childrenCount())) {
        result.append(SEPARATOR);
      }
    }

    return result.toString();
  }

  private static boolean isLastIndex(int i, int size) {
    return i == size - 1;
  }

}
