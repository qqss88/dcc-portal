/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.util;

import static com.google.common.base.Strings.isNullOrEmpty;
import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import org.icgc.dcc.portal.model.Location;

@NoArgsConstructor(access = PRIVATE)
public final class LocationUtils {

  /**
   * Constants.
   */
  private static final String RANGE_SPLIT = "-";
  private static final String CHR_SPLIT = ":";
  private static final String CHR_PREFIX = "chr";

  public static Location parseLocation(String text) {
    String normalized = text.trim();
    String[] parts = normalized.split(CHR_SPLIT);
    String chromosome = parseChromosome(parts[0]);

    Integer start = null, end = null;
    if (parts.length == 2) {
      String[] range = parts[1].split(RANGE_SPLIT);
      start = isNullOrEmpty(range[0]) ? 0 : parseRangeBound(range[0]);

      if (range.length == 2) {
        end = parseRangeBound(range[1]);
      }
    }

    return new Location(chromosome, start, end);
  }

  private static String parseChromosome(String value) {
    return value.replaceFirst("(?i)^" + CHR_PREFIX + "(.*)", "$1").toUpperCase();
  }

  private static int parseRangeBound(String value) {
    return Integer.parseInt(value.replaceAll(",", ""));
  }

}
