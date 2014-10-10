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

import static java.lang.String.format;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.GeneListDAO;
import org.skife.jdbi.v2.DBI;

import com.google.inject.Singleton;

/**
 * Offers gene-list operations
 */
@Singleton
@Slf4j
public class GeneListService {

  final DBI dbi;
  final GeneListDAO geneListDAO;

  public GeneListService() {
    // TODO: Get from configuration
    String testURI = format("jdbc:h2:genelist;MODE=PostgreSQL;INIT=runscript from '%s'", "inMemGenelist.sql");
    dbi = new DBI(testURI);
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

  // Test
  public static void main(String[] args) {
    GeneListService service = new GeneListService();

    service.insert("a,b,c,d,e,f,g"); // 0
    service.insert("a,b,c,d,e,f,g,lalalal"); // 1

    log.info("Data: {}", service.get(1));
    log.info("Data: {}", service.get(0));
  }

  /*
   * public static void _main(String[] args) throws SQLException, IOException { String testURI =
   * format("jdbc:h2:genelist;MODE=PostgreSQL;INIT=runscript from '%s'", "inMemGenelist.sql"); DBI dbi = new
   * DBI(testURI);
   * 
   * // Just do a sanity check Handle h = dbi.open(); h.close();
   * 
   * // Test more Handle handle = dbi.open(); handle.execute("insert into genelist(id, data) values (2, 'test1')");
   * handle.execute("insert into genelist(id, data) values (5, 'test1,test2')");
   * handle.execute("insert into genelist(id, data) values (7, 'test1,test2,test3,test4,test5')");
   * handle.execute("insert into genelist(id, data) values (19, 'test1,test2,test3,test4,test5,test6,test7')"); val
   * query = handle.createQuery("select * from genelist"); val result = query.list(); for (val r : result) { Long id =
   * (Long) r.get("id"); Clob clob = (Clob) r.get("data");
   * 
   * Reader reader = clob.getCharacterStream(); String content = CharStreams.toString(reader);
   * log.info("Table contains {} -> {}", r.get("id"), r.get("data")); log.info("Clob content {}", content); } }
   */

}
