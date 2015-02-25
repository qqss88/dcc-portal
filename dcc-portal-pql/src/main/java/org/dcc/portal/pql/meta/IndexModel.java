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

import static java.lang.String.format;
import lombok.NonNull;
import lombok.Value;

import org.icgc.dcc.portal.model.IndexModel.Type;

@Value
public class IndexModel {

  private static final DonorCentricTypeModel donorCentric = new DonorCentricTypeModel();
  private static final GeneCentricTypeModel geneCentric = new GeneCentricTypeModel();
  private static final MutationCentricTypeModel mutationCentric = new MutationCentricTypeModel();

  public boolean isNested(@NonNull String field, Type type) {
    return getTypeModel(type).isNested(field);
  }

  public String getNestedPath(@NonNull String field, Type type) {
    return getTypeModel(type).getNestedPath(field);
  }

  public static AbstractTypeModel getTypeModel(Type type) {
    switch (type) {
    case DONOR_CENTRIC:
      return donorCentric;
    case GENE_CENTRIC:
      return geneCentric;
    case MUTATION_CENTRIC:
      return mutationCentric;
    }

    throw new IllegalArgumentException(format("Type %s was not found", type.getId()));
  }

  public static AbstractTypeModel getDonorCentricTypeModel() {
    return donorCentric;
  }

  public static AbstractTypeModel getMutationCentricTypeModel() {
    return mutationCentric;
  }

}
