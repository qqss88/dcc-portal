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

import static org.assertj.core.api.Assertions.fail;
import static org.icgc.dcc.common.core.util.FormatUtils._;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.junit.Test;

@Slf4j
public class PqlParseListenerTest {

  PqlParseListener listener = new PqlParseListener();

  @Test
  public void getEsAstTest() {
    val query = "select(gender, age), ne(gender, 'no_date'), and(gt(age, 20), lt(age, 60)), limit(5, 10)";
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    val esAst = listener.getEsAst();
    log.info("{}", esAst);

    val boolNode = esAst.getChild(0).getChild(0);

    // childrenContainValue(shouldNode, "six");
    // childrenContainValue(shouldNode, 6);
  }

  @Test
  public void getEsAstTest_equal() {
    val query = "eq(age, 100)";
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    val esAst = listener.getEsAst();
    log.info("{}", esAst);
  }

  @Test
  public void getEsAstTest_filter() {
    val query = "or(gt(age, 10), le(age, 55)), eq(age, 100)";
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    val esAst = listener.getEsAst();
    log.info("{}", esAst);
  }

  @Test
  public void getEsAstTest_select() {
    val query = "select(sex, drugs, rocknroll)";
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    val esAst = listener.getEsAst();
    log.info("{}", esAst);
    // FIXME: finish
  }

  /**
   * Checks if one of the children contains {@code value}. Fails if the value was found in more that one child.
   */
  private static void childrenContainValue(ExpressionNode parent, Object value) {
    boolean found = false;
    for (val child : parent.getChildren()) {

      if (child instanceof TermNode) {
        if (termNodeContainsValue((TermNode) child, value, found)) {
          found = true;
        }
      }
      else if (child instanceof NotNode) {
        if (termNodeContainsValue((TermNode) child.getChild(0), value, found)) {
          found = true;
        }
      }

    }

    if (!found) {
      fail(_("Value %s was not found in %s", value, parent));
    }

  }

  private static boolean termNodeContainsValue(TermNode node, Object value, boolean foundBefore) {
    val nodeName = node.getNameNode().getValue();
    val nodeValue = node.getValueNode().getValue();
    if (nodeName.equals(value) || nodeValue.equals(value)) {
      if (foundBefore) {
        fail(_("Parent contains more than one value of %s", value.toString()));
      } else {
        return true;
      }
    }

    return false;
  }

}
