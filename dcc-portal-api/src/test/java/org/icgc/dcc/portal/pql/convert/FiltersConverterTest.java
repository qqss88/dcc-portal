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

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FiltersConverterTest {

  FiltersConverter converter = new FiltersConverter();
  private final static ObjectMapper mapper = createObjectMapper();

  @Test
  public void convertToEqualTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:'DO1'}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("eq(donor.id,'DO1')");
  }

  @Test
  public void convertToArrayTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['DO1']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.id,'DO1')");
  }

  @Test
  public void convertToArrayTest_multivalue() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['DO1', 'DO2']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.id,'DO1','DO2')");
  }

  @Test
  public void notTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:'DO1'}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("ne(donor.id,'DO1')");
  }

  @Test
  public void notTest_array() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:['DO1','DO2']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(in(donor.id,'DO1','DO2'))");
  }

  @Test
  public void existsTest() {
    val result = converter.convertFilters(createFilters("{donor:{hasCuratedSet:true}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("exists(gene.curatedSetId)");
  }

  @Test
  public void missingTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:'DO1'}, hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("ne(donor.id,'DO1'),missing(gene.pathwayId)");
  }

  @Test
  public void multiFilterTest() {
    val result = converter.convertFilters(createFilters("{donor:{hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId)");
  }

  @SneakyThrows
  public static JqlFilters createFilters(String filters) {
    return mapper.readValue(new FiltersParam(filters).get().toString(), JqlFilters.class);
  }

  private static ObjectMapper createObjectMapper() {
    return registerJqlDeserializer(new ObjectMapper());
  }

  private static ObjectMapper registerJqlDeserializer(ObjectMapper mapper) {
    val module = new SimpleModule();
    module.addDeserializer(JqlFilters.class, new JqlFiltersDeserializer());
    mapper.registerModule(module);

    return mapper;
  }

}
