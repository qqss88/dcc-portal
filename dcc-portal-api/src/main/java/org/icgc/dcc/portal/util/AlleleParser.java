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

import lombok.NonNull;

import org.icgc.dcc.portal.model.AlleleMutation;

public class AlleleParser {

  private static final String INDEL_SYMBOL = ">";
  private static final String NO_ALLELE_SYMBOL = "-";

  @NonNull
  public static AlleleMutation parseAllele(String input) {
    String allele = input.trim();

    if (!allele.contains(INDEL_SYMBOL)) {
      return new AlleleMutation(NO_ALLELE_SYMBOL, allele, allele);
    }

    if (allele.substring(0, allele.indexOf(INDEL_SYMBOL)).length() == 1) {
      return allele.contains(NO_ALLELE_SYMBOL) ?
          new AlleleMutation(NO_ALLELE_SYMBOL, allele.substring(2), allele)
          : new AlleleMutation(NO_ALLELE_SYMBOL, allele.substring(3), NO_ALLELE_SYMBOL + INDEL_SYMBOL
              + allele.substring(3));
    } else {
      return allele.contains(NO_ALLELE_SYMBOL) ?
          new AlleleMutation(allele.substring(0, allele.indexOf(INDEL_SYMBOL)), NO_ALLELE_SYMBOL, allele)
          : new AlleleMutation(allele.substring(1, allele.indexOf(INDEL_SYMBOL)), NO_ALLELE_SYMBOL,
              allele.substring(1, allele.indexOf(INDEL_SYMBOL)) + INDEL_SYMBOL + NO_ALLELE_SYMBOL);
    }
  }

}
