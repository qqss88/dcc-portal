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
package org.icgc.dcc.portal.service;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.GeneListDAO;
import org.skife.jdbi.v2.DBI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Gene-list related operations, uses genes under the hood
 */
@Service
@Slf4j
public class GeneListService {

  private final DBI dbi;
  private final GeneListDAO geneListDAO;

  @Autowired
  public GeneListService(DBI dbi) {
    this.dbi = dbi;
    dbi.open();
    geneListDAO = dbi.open(GeneListDAO.class);
  }

  /*
   * Get the next sequence Id, and use the Id as key to insert and return it. Do this in 2 discrete steps for
   * compatibility
   */
  public long insert(String data) {
    long id = geneListDAO.nextId();
    geneListDAO.insert(id, data);
    return id;
  }

  public String get(long id) {
    return geneListDAO.get(id);
  }

  /*
   * Test
   */
  /*
   * public static void main(String[] args) { DBI dbi = new
   * DBI("jdbc:h2:genelist;MODE=PostgreSQL;INIT=runscript from 'inMemGenelist.sql'"); GeneListService service = new
   * GeneListService(dbi);
   * 
   * service.insert("a,b,c,d,e,f,g"); // 0 service.insert("a,b,c,d,e,f,g,lalalal"); // 1
   * 
   * log.info("Data: {}", service.get(1)); log.info("Data: {}", service.get(0)); }
   */

}
