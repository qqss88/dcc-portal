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
package org.icgc.dcc.portal.pql.convert;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Query;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

@Slf4j
public class Jql2PqlConverterTest {

  Jql2PqlConverter converter = new Jql2PqlConverter();

  @Test
  public void fieltdsTest() {
    val query = Query.builder().fields(ImmutableList.of("id", "age")).build();
    assertResponse(query, "select(id,age)");
  }

  @Test
  public void fieldsWithFilterTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{is:1}}}").get())
        .build();
    assertResponse(query, "select(id,age),eq(donor.id,1)");
  }

  @Test
  public void sizeTest() {
    val query = Query.builder().size(100).build();
    assertResponse(query, "limit(100)");
  }

  @Test
  public void fromSizeTest() {
    val query = Query.builder().size(100).from(10).build();
    assertResponse(query, "limit(9,100)");
  }

  @Test
  public void sortTest() {
    val query = Query.builder().sort("id").order("asc").build();
    assertResponse(query, "sort(+id)");
  }

  @Test
  public void includeFacetsTest() {
    val query = Query.builder().includes(singletonList("facets")).build();
    val result = converter.convert(query, Type.DONOR_CENTRIC);
    log.debug("{}", result);
    assertThat(result).contains("facets(*)");
  }

  private void assertResponse(Query query, String exectedResult) {
    val result = converter.convert(query, Type.DONOR_CENTRIC);
    log.debug("{}", result);

    if (query.hasFields()) {
      assertThat(result).isEqualTo(exectedResult);
    } else {
      assertThat(result).contains(exectedResult);
    }

  }

}
