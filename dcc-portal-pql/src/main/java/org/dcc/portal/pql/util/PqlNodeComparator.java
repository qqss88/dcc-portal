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
package org.dcc.portal.pql.util;

import static org.dcc.portal.pql.ast.Type.LIMIT;
import static org.dcc.portal.pql.ast.Type.SORT;

import java.util.Comparator;

import lombok.val;

import org.dcc.portal.pql.ast.PqlNode;

/**
 * This comparator makes sure the top-level nodes comply the ordering imposed by the PQL syntax. Specifically, all other
 * nodes must appear before 'sort' and 'limit', and 'sort' must appear before 'limit'. Usually this is used before
 * turning a StatementNode into a PQL string (otherwise the generated PQL statement wouldn't be correct).
 */
public class PqlNodeComparator implements Comparator<PqlNode> {

  private static final int DONT_CARE = 0;
  private static final int SWAP = 1;
  private static final int NO_SWAP = -1;

  @Override
  public int compare(PqlNode node1, PqlNode node2) {
    val left = node1.type();
    val right = node2.type();

    if (left == SORT && right == LIMIT) {
      return NO_SWAP;
    } else if (left == LIMIT && right == SORT) {
      return SWAP;
    } else if (left == LIMIT || left == SORT) {
      return SWAP;
    } else if (right == LIMIT || right == SORT) {
      return NO_SWAP;
    }

    return DONT_CARE;
  }

}
