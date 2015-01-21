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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.val;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.icgc.dcc.portal.pql.antlr4.PqlLexer;
import org.icgc.dcc.portal.pql.antlr4.PqlParser;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FilterContext;

import com.google.common.collect.ImmutableList;

@NoArgsConstructor(access = PRIVATE)
public class ParseTrees {

  /**
   * Index of value in a terminal node
   */
  private static final int NODE_VALUE_INDEX = 4;

  /**
   * Index of name in a terminal node
   */
  private static final int NODE_NAME_INDEX = 2;

  /**
   * Number of children in a valid 'equal' or 'not equal' parse tree node
   */
  private static final int NODE_CHILDREN_NUMBER = 6;

  public static ParseTree createParseTree(String query) {
    val parser = getParser(query);

    return parser.statement();
  }

  public static PqlParser getParser(String query) {
    val inputStream = new ANTLRInputStream(query);
    val lexer = new PqlLexer(inputStream);
    val tokenStream = new CommonTokenStream((lexer));

    return new PqlParser(tokenStream);
  }

  public static Iterable<ParseTree> getChildren(ParseTree item) {
    val result = ImmutableList.<ParseTree> builder();
    for (int i = 0; i < item.getChildCount(); i++) {
      result.add(item.getChild(i));
    }

    return result.build();
  }

  public static Pair<String, Object> getPair(FilterContext nodeContext) {
    // ParseTrees.checkNodeValidity(nodeContext);
    val name = nodeContext.getChild(ParseTrees.NODE_NAME_INDEX).getText();
    val value = nodeContext.getChild(ParseTrees.NODE_VALUE_INDEX).getText();
    checkState(!isNullOrEmpty(name), "Could not get name from the expression %s", nodeContext.toStringTree());
    checkState(!isNullOrEmpty(value), "Could not get value from the expression %s", nodeContext.toStringTree());

    return new Pair<String, Object>(name, ParseTrees.getTypeSafeValue(value));
  }

  private static void checkNodeValidity(FilterContext nodeContext) {
    checkState(nodeContext.getChildCount() == ParseTrees.NODE_CHILDREN_NUMBER,
        "Equal node is malformed. Expected {} children, but found {}", ParseTrees.NODE_CHILDREN_NUMBER,
        nodeContext.getChildCount());
  }

  private static final Object getTypeSafeValue(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
    }

    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
    }

    return value;
  }

}
