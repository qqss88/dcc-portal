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

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.MustBoolNode;
import org.dcc.portal.pql.es.ast.MustNotBoolNode;
import org.dcc.portal.pql.es.ast.NotNode;
import org.dcc.portal.pql.es.ast.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.TermNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.junit.Test;

@Slf4j
public class PqlParseListenerTest {

  PqlParseListener listener = new PqlParseListener();

  @Test
  public void getEsAstTest() {
    val query =
        "and(eq(one, 1), ne(two, 2), eq(five, 5))&or(ne(six, 6), eq(three, 3), ne(four, 4))&and(eq(seven, 7), eq(eight, 8))&or(eq(nine, 9), eq(ten, 10))";
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.program();

    val esAst = listener.getEsAst();
    log.info("{}", esAst);

    val boolNode = esAst.getChild(0).getChild(0);
    // must, mustNot, should
    assertThat(boolNode.getChildren().size()).isEqualTo(3);

    val shouldNode = (ShouldBoolNode) boolNode.getChild(0);
    assertThat(shouldNode.getChildren().size()).isEqualTo(5);

    // ne(six, 6)
    val sixChild = (TermNode) ((NotNode) shouldNode.getChild(0)).getChild(0);
    assertThat(sixChild.getNameNode().getPayload()).isEqualTo("six");
    assertThat(sixChild.getValueNode().getPayload()).isEqualTo(6);

    // eq(three, 3)
    val threeChild = (TermNode) shouldNode.getChild(1);
    assertThat(threeChild.getNameNode().getPayload()).isEqualTo("three");
    assertThat(threeChild.getValueNode().getPayload()).isEqualTo(3);

    // ne(four, 4)
    val fourChild = (TermNode) ((NotNode) shouldNode.getChild(2)).getChild(0);
    assertThat(fourChild.getNameNode().getPayload()).isEqualTo("four");
    assertThat(fourChild.getValueNode().getPayload()).isEqualTo(4);

    // eq(nine, 9)
    val nineChild = (TermNode) shouldNode.getChild(3);
    assertThat(nineChild.getNameNode().getPayload()).isEqualTo("nine");
    assertThat(nineChild.getValueNode().getPayload()).isEqualTo(9);

    // eq(ten, 10)
    val tenChild = (TermNode) shouldNode.getChild(4);
    assertThat(tenChild.getNameNode().getPayload()).isEqualTo("ten");
    assertThat(tenChild.getValueNode().getPayload()).isEqualTo(10);

    val mustNode = (MustBoolNode) boolNode.getChild(1);
    assertThat(mustNode.getChildren().size()).isEqualTo(4);

    // eq(one, 1)
    val oneChild = (TermNode) mustNode.getChild(0);
    assertThat(oneChild.getNameNode().getPayload()).isEqualTo("one");
    assertThat(oneChild.getValueNode().getPayload()).isEqualTo(1);

    // eq(file, 5)
    val fiveChild = (TermNode) mustNode.getChild(1);
    assertThat(fiveChild.getNameNode().getPayload()).isEqualTo("five");
    assertThat(fiveChild.getValueNode().getPayload()).isEqualTo(5);

    // eq(seven, 7)
    val sevenChild = (TermNode) mustNode.getChild(2);
    assertThat(sevenChild.getNameNode().getPayload()).isEqualTo("seven");
    assertThat(sevenChild.getValueNode().getPayload()).isEqualTo(7);

    // eq(eight, 8)
    val eightChild = (TermNode) mustNode.getChild(3);
    assertThat(eightChild.getNameNode().getPayload()).isEqualTo("eight");
    assertThat(eightChild.getValueNode().getPayload()).isEqualTo(8);

    val mustNotNode = (MustNotBoolNode) boolNode.getChild(2);
    assertThat(mustNotNode.getChildren().size()).isEqualTo(1);

    // ne(two, 2)
    val twoChild = (TermNode) mustNotNode.getChild(0);
    assertThat(twoChild.getNameNode().getPayload()).isEqualTo("two");
    assertThat(twoChild.getValueNode().getPayload()).isEqualTo(2);
  }

}
