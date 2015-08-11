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
package org.icgc.dcc.portal.util;

import static lombok.AccessLevel.PRIVATE;

import javax.annotation.Nullable;

import org.dcc.portal.pql.exception.SemanticException;

import lombok.NoArgsConstructor;

/**
 * checkState helpers that throw SemanticExceptions rather than IllegalArgumentExceptions.
 */
@NoArgsConstructor(access = PRIVATE)
public final class SemanticUtils {

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   *
   * @param expression a boolean expression
   * @throws SemanticException if {@code expression} is false
   */
  public static void checkState(boolean expression) {
    if (!expression) {
      throw new SemanticException("Expression returned false.");
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   *
   * @param expression a boolean expression
   * @param errorMessage the exception message to use if the check fails; will be converted to a string using
   * {@link String#valueOf(Object)}
   * @throws SemanticException if {@code expression} is false
   */
  public static void checkState(
      boolean expression, @Nullable Object errorMessage) {
    if (!expression) {
      throw new SemanticException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   *
   * @param expression a boolean expression
   * @param errorMessageTemplate a template for the exception message should the check fail. The message is formed by
   * replacing each {@code %s} placeholder in the template with an argument. These are matched by position - the first
   * {@code %s} gets {@code errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
   * square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message template. Arguments are converted to
   * strings using {@link String#valueOf(Object)}.
   * @throws SemanticException if {@code expression} is false
   * @throws NullPointerException if the check fails and either {@code
   *     errorMessageTemplate} or {@code errorMessageArgs} is null (don't let this happen)
   */
  public static void checkState(boolean expression,
      @Nullable String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    if (!expression) {
      throw new SemanticException(
          String.format(errorMessageTemplate, errorMessageArgs));
    }
  }

}