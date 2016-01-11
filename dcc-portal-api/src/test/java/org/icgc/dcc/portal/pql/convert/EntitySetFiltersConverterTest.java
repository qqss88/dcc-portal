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

import org.junit.Test;

import lombok.val;

public class EntitySetFiltersConverterTest {

  FiltersConverter converter = new FiltersConverter();

  @Test
  public void donorEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{donor:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.entitySetId,'abc')");
  }

  @Test
  public void donorEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{donor:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor,in(donor.entitySetId,'abc'))");
  }

  @Test
  public void donorEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{donor:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(ssm_occurrence,in(donor.entitySetId,'abc'))");
  }

  @Test
  public void geneEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{gene:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,in(gene.entitySetId,'abc'))");
  }

  @Test
  public void geneEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{gene:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.entitySetId,'abc')");
  }

  @Test
  public void geneEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{gene:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.entitySetId,'abc'))");
  }

  @Test
  public void mutationEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{mutation:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,in(mutation.entitySetId,'abc'))");
  }

  @Test
  public void mutationEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{mutation:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor.ssm,in(mutation.entitySetId,'abc'))");
  }

  @Test
  public void mutationEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{mutation:{entitySetId:{is:['abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.entitySetId,'abc')");
  }

  @Test
  public void donorInlineEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.entitySetId,'abc')");
  }

  @Test
  public void donorInlineEntitySetNotTest_donor() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(in(donor.entitySetId,'abc'))");
  }

  @Test
  public void donorInlineEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor,in(donor.entitySetId,'abc'))");
  }

  @Test
  public void donorInlineEntitySetNotTest_gene() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor,not(in(donor.entitySetId,'abc')))");
  }

  @Test
  public void donorInlineEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(ssm_occurrence,in(donor.entitySetId,'abc'))");
  }

  @Test
  public void donorInlineEntitySetNotTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{donor:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(ssm_occurrence,not(in(donor.entitySetId,'abc')))");
  }

  @Test
  public void geneInlineEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,in(gene.entitySetId,'abc'))");
  }

  @Test
  public void geneInlineEntitySetNotTest_donor() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,not(in(gene.entitySetId,'abc')))");
  }

  @Test
  public void geneInlineEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.entitySetId,'abc')");
  }

  @Test
  public void geneInlineEntitySetNotTest_gene() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("not(in(gene.entitySetId,'abc'))");
  }

  @Test
  public void geneInlineEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.entitySetId,'abc'))");
  }

  @Test
  public void geneInlineEntitySetNotTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{gene:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,not(in(gene.entitySetId,'abc')))");
  }

  @Test
  public void mutationInlineEntitySetTest_donor() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,in(mutation.entitySetId,'abc'))");
  }

  @Test
  public void mutationInlineEntitySetNotTest_donor() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,not(in(mutation.entitySetId,'abc')))");
  }

  @Test
  public void mutationInlineEntitySetTest_gene() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor.ssm,in(mutation.entitySetId,'abc'))");
  }

  @Test
  public void mutationInlineEntitySetNotTest_gene() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor.ssm,not(in(mutation.entitySetId,'abc')))");
  }

  @Test
  public void mutationInlineEntitySetTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{is:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.entitySetId,'abc')");
  }

  @Test
  public void mutationInlineEntitySetNotTest_mutation() {
    val filters = FiltersConverterTest.createFilters("{mutation:{id:{not:['ES:abc']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("not(in(mutation.entitySetId,'abc'))");
  }

}
