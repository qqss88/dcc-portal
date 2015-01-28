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
package meta;

import java.util.List;

import lombok.Getter;
import lombok.val;

import com.google.common.collect.ImmutableList;

@Getter
public class ObjectFieldModel extends FieldModel {

  private final List<? extends FieldModel> fields;

  private ObjectFieldModel(String name, List<? extends FieldModel> fields) {
    this(name, null, fields);
  }

  private ObjectFieldModel(String name, String uiAlias, List<? extends FieldModel> fields) {
    this(name, uiAlias, false, fields);
  }

  private ObjectFieldModel(String name, boolean nested, List<? extends FieldModel> fields) {
    this(name, null, nested, fields);
  }

  private ObjectFieldModel(String name, String uiAlias, boolean nested, List<? extends FieldModel> fields) {
    super(name, null, FieldType.OBJECT, nested);
    this.fields = fields;
  }

  public static <T extends FieldModel> ObjectFieldModel object(String name, T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(name, fieldsList.build());
  }

  public static <T extends FieldModel> ObjectFieldModel object(T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(FieldModel.NO_NAME, fieldsList.build());
  }

}
