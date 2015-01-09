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

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.ExpressionType;
import org.dcc.portal.pql.es.internal.ast.ExpressionNodeImpl;
import org.junit.Test;

public class BoolBuilderTest {

  @Test
  public void mustTermTest() {
    ExpressionNodeImpl maleTermNode = Builders.createTermBuilder().name("sex").value("male").build();
    ExpressionNodeImpl femaleTermNode = Builders.createTermBuilder().name("sex").value("female").build();

    val boolNode = Builders.createRoot()
        .mustTerm(maleTermNode)
        .mustTerm(femaleTermNode)
        .build();
    assertThat(boolNode.getType()).isEqualTo(ExpressionType.BOOL);

    // One
    assertThat(boolNode.getParent()).isNull();
    assertThat(boolNode.getChildrenCount()).isEqualTo(1);

    val mustNode = (ExpressionNode) boolNode.getChild(0);
    assertThat(mustNode.getType()).isEqualTo(ExpressionType.MUST);
    assertThat(mustNode.getChildrenCount()).isEqualTo(2);
    assertThat(mustNode.getChild(0)).isEqualTo(maleTermNode);
    assertThat(mustNode.getChild(1)).isEqualTo(femaleTermNode);
  }

}
