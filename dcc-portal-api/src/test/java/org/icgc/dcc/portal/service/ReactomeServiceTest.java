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

import java.io.IOException;

import lombok.val;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * Test the ability to get a list of dbIds and matching Uniprot ids from Reactome's restAPI
 */
public class ReactomeServiceTest {

  private ReactomeService service = new ReactomeService();

  @Test
  public void testSuccessMapping() {
    val result = service.mapProteinIds(Lists.newArrayList("Q9C035", "Q13477", "P30613"));
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get("Q9C035")).isEqualTo("103827");
  }

  @Test
  public void testSuccessPathway() throws IOException {
    val result = service.getPathwayDiagramStream("REACT_578.8");
    assertThat(result).isNotNull();
    assertThat(result.read()).isEqualTo("<".charAt(0));
  }

}
