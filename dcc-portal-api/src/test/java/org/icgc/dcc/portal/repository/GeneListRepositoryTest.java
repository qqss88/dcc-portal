/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.repository;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

public class GeneListRepositoryTest {

  GeneListRepository repository;

  @Before
  public void setUp() {
    val dbi = new DBI("jdbc:h2:genelist;MODE=PostgreSQL;INIT=runscript from 'src/test/sql/schema.sql'");
    this.repository = dbi.open(GeneListRepository.class);
  }

  @After
  public void tearDown() {
    repository.close();
  }

  @Test
  public void testInsertFind() {
    val expectedData = "test";

    val geneListId1 = repository.insert(expectedData);
    assertThat(geneListId1).isPositive();

    val geneListId2 = repository.insert(expectedData);
    assertThat(geneListId2).isPositive();

    assertThat(geneListId1).isLessThan(geneListId2);

    val actualData = repository.find(geneListId1);
    assertThat(expectedData).isEqualTo(actualData);
  }

}
