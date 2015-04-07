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
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.pql.convert.model.JqlFilters;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Slf4j
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
  public void missingValueTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['_missing']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(donor.id)");
  }

  @Test
  public void missingValueTest_array() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['_missing', 'DO1']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("or(missing(donor.id),in(donor.id,'DO1'))");
  }

  @Test
  public void mutationNestedFiltersTest_gene() {
    val filters = createFilters("{donor:{id:{is:'DO1'}},mutation:{id:{is:'MU1'},consequenceType:{is:'start_lost'}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    log.info("PQL: {}", result);
    assertThat(result)
        .isEqualTo(
            "nested(donor,"
                + "and(nested(donor.ssm,and("
                + "nested(donor.ssm.consequence,eq(mutation.consequenceType,'start_lost')),"
                + "eq(mutation.id,'MU1'))),eq(donor.id,'DO1')))");
  }

  @Test
  public void donorFiltersTest_donor() {
    val filters = createFilters("{donor:{primarySite:{is:['Brain']},ageAtDiagnosisGroup:{is:['60 - 69']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.primarySite,'Brain'),in(donor.ageAtDiagnosisGroup,'60 - 69')");
  }

  @Test
  public void donorFiltersTest_mutation() {
    val filters = createFilters("{donor:{primarySite:{is:['Brain']},ageAtDiagnosisGroup:{is:['60 - 69']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result)
        .isEqualTo("nested(ssm_occurrence,in(donor.primarySite,'Brain'),in(donor.ageAtDiagnosisGroup,'60 - 69'))");
  }

  @Test
  public void mutationNestedAndNonNestedTest() {
    val filters = createFilters("{mutation:{platformNested:{is:['M1','M2']},id:{is:['M1','M2']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).
        isEqualTo("in(mutation.id,'M1','M2'),nested(ssm_occurrence.observation,in(platformNested,'M1','M2'))");
  }

  @Test
  public void multifilterWithMissingKeywordTest() {
    val filters = createFilters("{donor:{gender:{is:['_missing','male']}},gene:{type:{is:['antisense']}},"
        + "mutation:{id:{is:'MU1'}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("eq(mutation.id,'MU1'),"
        + "nested(transcript,in(gene.type,'antisense')),"
        + "nested(ssm_occurrence,or(missing(donor.gender),in(donor.gender,'male')))");
  }

  @Test
  public void mutationLocationTest() {
    val result = converter.convertFilters(createFilters("{mutation:{location:{is:['chr12:43566-3457633']}}}"),
        MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.location,'chr12:43566-3457633')");
  }

  @Test
  public void mutationLocationTest_otherType() {
    val result = converter.convertFilters(createFilters("{mutation:{location:{is:['chr12:43566-3457633']}}}"),
        DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.location,'chr12:43566-3457633')");
  }

  @Test
  public void geneLocationTest() {
    val result = converter.convertFilters(createFilters("{gene:{location:{is:['chr12:43566-3457633']}}}"),
        GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.location,'chr12:43566-3457633')");
  }

  @Test
  public void geneLocationTest_otherType() {
    val result = converter.convertFilters(createFilters("{gene:{location:{is:['chr12:43566-3457633']}}}"),
        MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(gene.location,'chr12:43566-3457633')");
  }

  @Test
  @Ignore
  public void pathwayTest_donor() {
    // TODO: is such filter even possible? 'hasPathway' should be added to 'gene' type only, not 'donor'
    val result = converter.convertFilters(createFilters("{donor:{hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId)");
  }

  @Test
  public void pathwayTest() {
    val result = converter.convertFilters(createFilters("{gene:{hasPathway:false}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId)");
  }

  @Test
  public void pathwayTest_otherType() {
    val result = converter.convertFilters(createFilters("{gene:{hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId)");
  }

  @Test
  public void goTermAndFilterTest_mutation() {
    val filters = createFilters("{mutation:{id:{is:['M1','M2']}},gene:{goTermId:{is:['321']}}}");
    val result = converter.convertFilters(filters, Type.MUTATION_CENTRIC);
    log.info("{}", result);
    assertThat(result).isEqualTo("in(mutation.id,'M1','M2'),in(gene.goTermId,'321')");
  }

  @Test
  public void curratedSetTest() {
    val filters = createFilters("{gene:{curatedSetId:{is:['GS1']},hasPathway:true}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.curatedSetId,'GS1'),exists(gene.pathwayId)");
  }

  @Test
  public void curratedSetTest_mutation() {
    val filters = createFilters("{gene:{curatedSetId:{is:['GS1']},hasPathway:true}}");
    val result = converter.convertFilters(filters, Type.MUTATION_CENTRIC);
    assertThat(result).isEqualTo("exists(gene.pathwayId),nested(transcript,in(gene.curatedSetId,'GS1'))");
  }

  @Test
  public void hasPathwayAndOtherFilterTest() {
    val filters = createFilters("{donor:{analysisTypes:{is:['non-NGS']}},gene:{hasPathway:true}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.analysisTypes,'non-NGS'),exists(gene.pathwayId)");
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
