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
package org.icgc.dcc.portal.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.primitives.Ints.tryParse;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.portal.service.BadRequestException;

/**
 * Represents a Chromosome, with a name and the allowed max position.
 */
@RequiredArgsConstructor
@Getter
public enum Chromosome {

  CHR1("1", 249250621),
  CHR2("2", 243199373),
  CHR3("3", 198022430),
  CHR4("4", 191154276),
  CHR5("5", 180915260),
  CHR6("6", 171115067),
  CHR7("7", 159138663),
  CHR8("8", 146364022),
  CHR9("9", 141213431),
  CHR10("10", 135534747),
  CHR11("11", 135006516),
  CHR12("12", 133851895),
  CHR13("13", 115169878),
  CHR14("14", 107349540),
  CHR15("15", 102531392),
  CHR16("16", 90354753),
  CHR17("17", 81195210),
  CHR18("18", 78077248),
  CHR19("19", 59128983),
  CHR20("20", 63025520),
  CHR21("21", 48129895),
  CHR22("22", 51304566),
  X("X", 155270560),
  Y("Y", 59373566),
  MT("MT", 16569);

  private final String name;
  /*
   * Max position for a chromosome
   */
  private final int length;

  /*
   * Constants
   */
  private static final String PREFIX = "chr".toUpperCase();
  private static final int PREFIX_LENGTH = PREFIX.length();

  private static final String CHROMOSOME_SEPARATOR = ":";
  private static final String RANGE_SEPARATOR = "-";

  private static final Map<String, Chromosome> LOOKUP_TABLE_BY_NAME;

  static {
    val values = EnumSet.allOf(Chromosome.class);
    val lookup = new HashMap<String, Chromosome>(values.size());
    for (val c : values) {
      lookup.put(c.getName().toUpperCase(), c);
    }
    LOOKUP_TABLE_BY_NAME = Collections.unmodifiableMap(lookup);
  }

  /*
   * Gets a Chromosome instance by its name (1-22, X, Y or MT).
   */
  public static Chromosome ofName(final String name) {
    checkArgument(!isNullOrEmpty(name), "The name of a chromosome must not empty or null.");

    val result = LOOKUP_TABLE_BY_NAME.get(name);
    if (null == result) {
      throw new BadRequestException("Invalid chromosome name (must be 1-22, X, Y or MT): " + name);
    }
    return result;
  }

  /*
   * Gets a Chromosome instance by its literal name in the format of 'chr' plus 1-22 or 'x', 'y', 'mt'. This might
   * appear redundant to the built-in valueOf(); however, this supports literals in lowercase too.
   */
  public static Chromosome ofLiteral(final String literal) {
    checkArgument(!isNullOrEmpty(literal), "The name of a chromosome must not empty or null.");

    // Try the fast way first.
    try {
      return Chromosome.valueOf(literal);
    } catch (Exception e) {
      /*
       * This empty catch is intentional so that the function can continue on.
       */
    }

    val input = literal.trim().toUpperCase();
    val name =
        (input.startsWith(PREFIX) && input.length() > PREFIX_LENGTH) ? input.substring(PREFIX_LENGTH) : input;
    return ofName(name);
  }

  @Override
  public String toString() {
    return this.getName();
  }

  /*
   * Checks if the position falls within the range of 0 to the max length associated with a particular chromosome. If it
   * does not, a BadRequestException will be thrown.
   * 
   * @param positionToCheck
   * 
   * @throws BadRequestException if positionToCheck exceeds the max associated with the chromosome.
   */
  public void checkPosition(final int positionToCheck) {
    if (positionToCheck < 0) {
      throw new BadRequestException("The 'position' argument must not be negative.");
    }
    if (positionToCheck > this.length) {
      throw new BadRequestException("The requested position (" + positionToCheck + ") exceeds the max limit ("
          + this.length + ") for Chromosome " + this.getName());
    }
  }

  /*
   * Parses a string to a valid length for a chromosome. It will throw a BadRequestException when an error occurs.
   */
  public Integer parsePosition(final String requestedPosition) {
    checkArgument(!isNullOrEmpty(requestedPosition), "The 'position' argument must not empty or null.");

    val wantedPosition = tryParse(requestedPosition.trim().replaceAll(",", ""));
    if (null == wantedPosition) {
      throw new BadRequestException("Error parsing '" + requestedPosition + "' to an integer.");
    }
    checkPosition(wantedPosition);
    return wantedPosition;
  }

  /*
   * Offers a safe (exception-free) way to parse a string to a valid position for a particular chromosome, backed by a
   * default value.
   * 
   * Warning: the defaultValue is NOT validated. It is assumed valid.
   */
  public Integer safeParsePositionElseDefault(final String positionInString, final int defaultValue) {
    Integer result = null;
    try {
      result = parsePositionElseDefault(positionInString, defaultValue);
    } catch (Exception e) {
      result = defaultValue;
    }
    return result;
  }

  private Integer parsePositionElseDefault(final String requestedPosition, final int defaultValue) {
    return (isNullOrEmpty(requestedPosition)) ? defaultValue : parsePosition(requestedPosition);
  }

  /*
   * Parses a string to a ChromosomeLocation instance, validating the chromosome and its lower and upper bounds along
   * the way. An example of a valid string is: chr1:20-100
   */
  public static ChromosomeLocation toChromosomeLocation(final String chromosomeWithRange) {
    checkArgument(!isNullOrEmpty(chromosomeWithRange), "The 'chromosomeWithRange' argument must not empty or null.");

    String[] parts = chromosomeWithRange.split(CHROMOSOME_SEPARATOR);
    val chromosome = ofLiteral(parts[0]);
    Integer start = null;
    Integer end = null;
    if (parts.length > 1 && !isNullOrEmpty(parts[1])) {
      String[] range = parts[1].split(RANGE_SEPARATOR);

      start = chromosome.parsePositionElseDefault(range[0], 0);
      if (range.length > 1) {
        end = chromosome.parsePositionElseDefault(range[1], chromosome.getLength());
      }
    }
    return new ChromosomeLocation(chromosome, start, end);
  }
}
