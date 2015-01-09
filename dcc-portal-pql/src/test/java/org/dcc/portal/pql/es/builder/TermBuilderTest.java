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
package org.dcc.portal.pql.es.builder;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionType;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.junit.Test;

public class TermBuilderTest {

  @Test
  public void termBuilderTest() {
    val termBuilder = Builders.createTermBuilder();
    val node = termBuilder.name("sex").value("male").build();

    assertThat(node.getChildrenCount()).isEqualTo(2);

    val name = node.getChild(0);
    assertThat(name).isInstanceOf(TerminalNode.class);
    assertThat(name.getParent()).isEqualTo(node);
    assertThat(name.getPayload().toString()).isEqualTo("sex");

    val value = node.getChild(1);
    assertThat(value).isInstanceOf(TerminalNode.class);
    assertThat(value.getParent()).isEqualTo(node);
    assertThat(value.getPayload().toString()).isEqualTo("male");

    assertThat(node.getParent()).isNull();
    assertThat((ExpressionType) node.getPayload()).isEqualTo(ExpressionType.TERM);
  }

  @Test
  public void partialTermNodeTest() {
    catchException(Builders.createTermBuilder().name("sex")).build();
    assertThat(caughtException()).isInstanceOf(NullPointerException.class);
    catchException(Builders.createTermBuilder().value("male")).build();
    assertThat(caughtException()).isInstanceOf(NullPointerException.class);
  }

}
