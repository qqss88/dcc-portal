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
package org.dcc.portal.pql.meta;

import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;

import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;

public class GeneCentricTypeModel extends AbstractTypeModel {

  public GeneCentricTypeModel() {
    super(defineFields());
  }

  private static List<FieldModel> defineFields() {
    return new ImmutableList.Builder<FieldModel>()
        .add(string("_gene_id", "id"))
        .add(string("symbol", "symbol"))
        .add(string("name", "name"))
        .add(string("biotype", "type"))
        .add(string("chromosome", "chromosome"))
        .add(long_("start", "start"))
        .add(long_("strand", "strand"))
        .add(string("description", "description"))
        .add(arrayOfStrings("synonyms", "synonyms"))
        .add(defineExternalDbIds())
        .add(defineSummary())
        .add(arrayOfStrings("pathways", "pathways"))
        .build();
  }

  private static ObjectFieldModel defineExternalDbIds() {
    return object("external_db_ids", "externalDbIds",
        arrayOfStrings("entrez_gene"));
  }

  private static ObjectFieldModel defineSummary() {
    return object("_summary",
        long_("_affected_donor_count", "affectedDonorCountTotal"),
        string("_affected_transcript_id", "affectedTranscriptIds"));
  }

}
