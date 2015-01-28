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

import lombok.Getter;

@Getter
public class ArrayFieldModel extends FieldModel {

  private final FieldModel element;

  private ArrayFieldModel(String name, FieldModel element) {
    this(name, null, element);
  }

  private ArrayFieldModel(String name, String uiAlias, FieldModel element) {
    this(name, uiAlias, false, element);
  }

  private ArrayFieldModel(String name, boolean nested, FieldModel element) {
    this(name, null, nested, element);
  }

  private ArrayFieldModel(String name, String uiAlias, boolean nested, FieldModel element) {
    super(name, uiAlias, FieldModel.FieldType.ARRAY, nested);
    this.element = element;
  }

  public static ArrayFieldModel arrayOfStrings(String name) {
    return new ArrayFieldModel(name, EMPTY_STRING_FIELD);
  }

  public static ArrayFieldModel arrayOfObjects(String name, ObjectFieldModel element) {
    return new ArrayFieldModel(name, element);
  }

  public static ArrayFieldModel nestedArrayOfObjects(String name, ObjectFieldModel element) {
    return new ArrayFieldModel(name, true, element);
  }

}
