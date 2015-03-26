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
package org.icgc.dcc.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.portal.repository.BaseRepositoryTest;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * Test the ability to get a list of dbIds and matching Uniprot ids from Reactome's restAPI
 */
public class ReactomeServiceTest extends BaseRepositoryTest {

  private ReactomeService service;

  @Before
  public void setUp() throws Exception {
    service = new ReactomeService();
  }

  @Test
  public void testSuccessMapping() {
    val result = service.matchProteinIds(Lists.newArrayList("49127", "6020621", "5998147"));
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get("49127")).isEqualTo("P30154");
  }
}
